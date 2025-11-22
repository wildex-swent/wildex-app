package com.android.wildex.model.map

import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Location
import kotlin.math.*

/**
 * Cluster the given [pins] according to the [zoomBand]:
 * - zoomBand >= 11: no clustering, return raw pins
 * - zoomBand 9–10: small clusters (2km radius)
 * - zoomBand <= 8: big clusters (8km radius)
 */
fun clusterPinsForZoom(
    pins: List<MapPin>,
    zoomBand: Int,
): List<MapPin> {
  if (pins.isEmpty()) return emptyList()
  val radiusMeters =
      when {
        zoomBand >= 11 -> 0.0
        zoomBand >= 9 -> 2000.0
        else -> 8000.0
      }
  if (radiusMeters <= 0.0) return pins
  return clusterByDistance(pins, radiusMeters)
}

/**
 * Cluster all pins by distance: pins that are within [radiusMeters] of each other (transitively)
 * are merged into a single ClusterPin. Uses a union–find (disjoint set) data structure to group
 * pins.
 *
 * @param pins List of MapPin to cluster (excluding existing ClusterPins).
 * @param radiusMeters Distance threshold for clustering (in meters).
 * @return List of ClusterPins representing the clustered pins.
 */
private fun clusterByDistance(
    pins: List<MapPin>,
    radiusMeters: Double,
): List<MapPin> {
  val basePins = pins.filter { it !is MapPin.ClusterPin }
  val trivial = handleTrivialClusterCases(basePins)
  if (trivial != null) return trivial

  val n = basePins.size
  val parent = IntArray(n) { it }

  // build union–find components
  mergeClosePins(basePins, radiusMeters, parent)

  // collect groups
  val groups = buildClusterGroups(basePins, parent)

  // build ClusterPins
  return buildClusterPins(groups)
}

/** Handle trivial cases for clustering: empty list or single pin. */
private fun handleTrivialClusterCases(basePins: List<MapPin>): List<MapPin>? {
  if (basePins.isEmpty()) return emptyList()
  if (basePins.size == 1) {
    val p = basePins[0]
    return listOf(
        MapPin.ClusterPin(
            id = "cluster_single_${p.id}",
            location = p.location,
            count = 1,
        ),
    )
  }
  return null
}

/** Merge close pins into union–find structure. */
private fun mergeClosePins(
    basePins: List<MapPin>,
    radiusMeters: Double,
    parent: IntArray,
) {
  fun find(x: Int): Int {
    var r = x
    while (parent[r] != r) r = parent[r]
    var cur = x
    while (parent[cur] != cur) {
      val p = parent[cur]
      parent[cur] = r
      cur = p
    }
    return r
  }

  fun union(a: Int, b: Int) {
    val ra = find(a)
    val rb = find(b)
    if (ra != rb) parent[rb] = ra
  }

  val n = basePins.size
  for (i in 0 until n) {
    val li = basePins[i].location
    for (j in i + 1 until n) {
      val lj = basePins[j].location
      if (distanceMeters(li.latitude, li.longitude, lj.latitude, lj.longitude) <= radiusMeters) {
        union(i, j)
      }
    }
  }
}

/** Build groups of pins from union–find structure. */
private fun buildClusterGroups(
    basePins: List<MapPin>,
    parent: IntArray,
): Map<Int, List<MapPin>> {
  fun findRoot(x: Int): Int {
    var r = x
    while (parent[r] != r) r = parent[r]
    var cur = x
    while (parent[cur] != cur) {
      val p = parent[cur]
      parent[cur] = r
      cur = p
    }
    return r
  }
  return basePins.indices
      .groupBy { i -> findRoot(i) }
      .mapValues { (_, indices) -> indices.map { idx -> basePins[idx] } }
}

/** Build ClusterPins from groups of pins. */
private fun buildClusterPins(
    groups: Map<Int, List<MapPin>>,
): List<MapPin> =
    groups.map { (root, group) ->
      val avgLat = group.map { it.location.latitude }.average()
      val avgLon = group.map { it.location.longitude }.average()
      val count = group.size
      val clusterId: Id = "cluster_$root"

      MapPin.ClusterPin(
          id = clusterId,
          location = Location(avgLat, avgLon, "Cluster"),
          count = count,
      )
    }

/** Approximate distance between two lat/lon points (meters). */
private fun distanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
  val r = 6371000.0
  val phi1 = lat1.toRadians()
  val phi2 = lat2.toRadians()
  val dPhi = (lat2 - lat1).toRadians()
  val dLambda = (lon2 - lon1).toRadians()
  val a = sin(dPhi / 2).pow(2.0) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2.0)
  val c = 2 * atan2(sqrt(a), sqrt(1 - a))
  return r * c
}

private fun Double.toRadians(): Double = this * Math.PI / 180.0
