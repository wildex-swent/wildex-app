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
 * Stack pins that have the exact same location into a single ClusterPin.
 *
 * @param pins List of MapPin to stack.
 * @return List of MapPin with stacked ClusterPins.
 */
fun stackSameLocationPins(pins: List<MapPin>): List<MapPin> {
  if (pins.isEmpty()) return emptyList()
  return pins
      .groupBy { it.location.latitude to it.location.longitude }
      .flatMap { (_, group) ->
        if (group.size == 1) {
          group
        } else {
          val first = group.first()
          listOf(
              MapPin.ClusterPin(
                  id = "stack_${first.id}",
                  location = first.location,
                  count = group.size,
                  childIds = group.map { it.id },
              ))
        }
      }
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

  val n = pins.size
  val parent = IntArray(n) { it }

  // build union–find components
  mergeClosePins(pins, radiusMeters, parent)

  // collect groups
  val groups = buildClusterGroups(pins, parent)

  // build ClusterPins
  return buildClusterPins(groups)
}

/**
 * Merge close pins into union–find structure.
 *
 * @param basePins List of MapPin to cluster.
 * @param radiusMeters Distance threshold for clustering (in meters).
 * @param parent Union–find parent array.
 */
private fun mergeClosePins(
    basePins: List<MapPin>,
    radiusMeters: Double,
    parent: IntArray,
) {
  val n = basePins.size
  for (i in 0 until n) {
    val li = basePins[i].location
    for (j in i + 1 until n) {
      val lj = basePins[j].location
      if (distanceMeters(li.latitude, li.longitude, lj.latitude, lj.longitude) <= radiusMeters) {
        union(i, j, parent)
      }
    }
  }
}

/**
 * Union operation for union–find structure.
 *
 * @param a Index of first element.
 * @param b Index of second element.
 * @param parent Union–find parent array.
 */
private fun union(a: Int, b: Int, parent: IntArray) {
  val ra = find(a, parent)
  val rb = find(b, parent)
  if (ra != rb) parent[rb] = ra
}

/**
 * Find root of x in union–find structure with path compression.
 *
 * @param x Index of element to find.
 * @param parent Union–find parent array.
 * @return Root index of x.
 */
private fun find(x: Int, parent: IntArray): Int {
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

/**
 * Build groups of pins from union–find structure.
 *
 * @param basePins List of MapPin to cluster.
 * @param parent Union–find parent array.
 * @return Map of root index to list of MapPin in that group.
 */
private fun buildClusterGroups(
    basePins: List<MapPin>,
    parent: IntArray,
): Map<Int, List<MapPin>> {
  return basePins.indices
      .groupBy { find(it, parent) }
      .mapValues { (_, indices) -> indices.map { basePins[it] } }
}

/**
 * Build ClusterPins from groups of pins.
 *
 * @param groups Map of root index to list of MapPin in that group.
 * @return List of ClusterPins representing the clustered pins.
 */
private fun buildClusterPins(
    groups: Map<Int, List<MapPin>>,
): List<MapPin> =
    groups.values.map { group ->
      if (group.size == 1) {
        val only = group.first()
        when (only) {
          is MapPin.ClusterPin -> only
          else ->
              MapPin.ClusterPin(
                  id = "cluster_single_${only.id}",
                  location = only.location,
                  count = 1,
                  childIds = listOf(only.id),
              )
        }
      } else {
        var weightedLatSum = 0.0
        var weightedLonSum = 0.0
        var totalCount = 0
        val allChildIds = mutableListOf<Id>()

        for (pin in group) {
          val weight =
              when (pin) {
                is MapPin.ClusterPin -> pin.count
                else -> 1
              }
          weightedLatSum += pin.location.latitude * weight
          weightedLonSum += pin.location.longitude * weight
          totalCount += weight

          when (pin) {
            is MapPin.ClusterPin -> allChildIds += pin.childIds
            else -> allChildIds += pin.id
          }
        }

        val avgLat = weightedLatSum / totalCount
        val avgLon = weightedLonSum / totalCount

        MapPin.ClusterPin(
            id = "cluster_${group.hashCode()}",
            location = Location(avgLat, avgLon, "Cluster"),
            count = totalCount,
            childIds = allChildIds,
        )
      }
    }

/**
 * Approximate distance between two lat/lon points (meters).
 *
 * @param lat1 Latitude of first point.
 * @param lon1 Longitude of first point.
 * @param lat2 Latitude of second point.
 * @param lon2 Longitude of second point.
 * @return Distance between the two points in meters.
 */
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

/** Convert degrees to radians. */
private fun Double.toRadians(): Double = this * Math.PI / 180.0
