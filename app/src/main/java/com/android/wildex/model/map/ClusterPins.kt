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
  if (basePins.isEmpty()) return emptyList()
  val n = basePins.size
  if (n == 1) {
    val p = basePins[0]
    return listOf(
        MapPin.ClusterPin(
            id = "cluster_single_${p.id}",
            location = p.location,
            count = 1,
        ))
  }
  // ---- union–find (disjoint sets over pins) ----
  val parent = IntArray(n) { it }

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

  // ---- merge any pair closer than radiusMeters ----
  for (i in 0 until n) {
    val li = basePins[i].location
    for (j in i + 1 until n) {
      val lj = basePins[j].location
      if (distanceMeters(li.latitude, li.longitude, lj.latitude, lj.longitude) <= radiusMeters) {
        union(i, j)
      }
    }
  }

  // ---- collect groups (components) ----
  val groups = mutableMapOf<Int, MutableList<MapPin>>()
  for (i in 0 until n) {
    val root = find(i)
    groups.getOrPut(root) { mutableListOf() }.add(basePins[i])
  }

  // ---- build ClusterPins from each group ----
  val result = mutableListOf<MapPin>()
  for ((root, group) in groups) {
    val avgLat = group.map { it.location.latitude }.average()
    val avgLon = group.map { it.location.longitude }.average()
    val count = group.size
    val clusterId: Id = "cluster_$root"

    result +=
        MapPin.ClusterPin(
            id = clusterId,
            location = Location(avgLat, avgLon, "Cluster"),
            count = count,
        )
  }

  return result
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
