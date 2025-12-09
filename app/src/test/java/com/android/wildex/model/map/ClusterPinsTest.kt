package com.android.wildex.model.map

import com.android.wildex.model.utils.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClusterPinsTest {
  private fun postPin(
      id: String,
      lat: Double,
      lon: Double,
  ): MapPin.PostPin =
      MapPin.PostPin(
          id = id,
          authorId = "author-$id",
          location = Location(lat, lon, "Label-$id"),
          imageURL = "https://example.com/$id.jpg",
      )

  private fun clusterPin(
      id: String,
      lat: Double,
      lon: Double,
      count: Int,
  ): MapPin.ClusterPin =
      MapPin.ClusterPin(
          id = id,
          location = Location(lat, lon, "Cluster-$id"),
          count = count,
      )

  @Test
  fun emptyPins_returnsEmpty() {
    val result = clusterPinsForZoom(emptyList(), zoomBand = 5)
    assertTrue(result.isEmpty())
  }

  @Test
  fun zoomBandHigh_returnsRawPins_andDoesNotCluster() {
    val p1 = postPin("p1", 46.0, 6.0)
    val p2 = postPin("p2", 46.5, 6.5)
    val preCluster = clusterPin("c1", 47.0, 7.0, count = 3)
    val pins = listOf(p1, p2, preCluster)
    val result = clusterPinsForZoom(pins, zoomBand = 11)
    assertEquals(3, result.size)
    assertEquals(listOf("p1", "p2", "c1"), result.map { it.id })
  }

  @Test
  fun onlyClusterPins_withClusteringRadius_returnsEmpty() {
    val c1 = clusterPin("c1", 46.0, 6.0, count = 2)
    val c2 = clusterPin("c2", 46.1, 6.1, count = 5)
    val pins = listOf<MapPin>(c1, c2)
    val result = clusterPinsForZoom(pins, zoomBand = 8)
    assertEquals(2, result.size)
    assertTrue(result.all { it is MapPin.ClusterPin })
    assertEquals(setOf("c1", "c2"), result.map { it.id }.toSet())
  }

  @Test
  fun singlePin_isConvertedToSingleClusterPin() {
    val p = postPin("p1", 46.0, 6.0)
    val result = clusterPinsForZoom(listOf(p), zoomBand = 9)
    assertEquals(1, result.size)
    val cluster = result.single() as MapPin.ClusterPin
    assertEquals("cluster_single_p1", cluster.id)
    assertEquals(1, cluster.count)
    assertEquals(p.location.latitude, cluster.location.latitude, 1e-9)
    assertEquals(p.location.longitude, cluster.location.longitude, 1e-9)
  }

  @Test
  fun threeClosePins_lowZoom_mergedIntoOneCluster() {
    val p1 = postPin("p1", 46.0000, 6.0000)
    val p2 = postPin("p2", 46.0100, 6.0000)
    val p3 = postPin("p3", 46.0200, 6.0000)
    val pins = listOf<MapPin>(p1, p2, p3)
    val result = clusterPinsForZoom(pins, zoomBand = 8)
    assertEquals(1, result.size)
    val cluster = result.single() as MapPin.ClusterPin
    assertEquals(3, cluster.count)
    val expectedLat = (p1.location.latitude + p2.location.latitude + p3.location.latitude) / 3.0
    val expectedLon = (p1.location.longitude + p2.location.longitude + p3.location.longitude) / 3.0
    assertEquals(expectedLat, cluster.location.latitude, 1e-9)
    assertEquals(expectedLon, cluster.location.longitude, 1e-9)
  }

  @Test
  fun farPins_lowZoom_formMultipleClusters() {
    val p1 = postPin("p1", 46.5200, 6.6300)
    val p2 = postPin("p2", 46.5210, 6.6310)
    val p3 = postPin("p3", 46.2040, 6.1430)
    val p4 = postPin("p4", 46.2050, 6.1440)
    val pins = listOf<MapPin>(p1, p2, p3, p4)
    val result = clusterPinsForZoom(pins, zoomBand = 8)
    assertEquals(2, result.size)
    assertTrue(result.all { it is MapPin.ClusterPin })
    val counts = result.map { (it as MapPin.ClusterPin).count }.sorted()
    assertEquals(listOf(2, 2), counts)
  }

  @Test
  fun samePins_clusteredAtZoom9_butNotAtZoom11() {
    val p1 = postPin("p1", 46.0000, 6.0000)
    val p2 = postPin("p2", 46.0090, 6.0000)
    val pins = listOf<MapPin>(p1, p2)
    val clustered = clusterPinsForZoom(pins, zoomBand = 9)
    assertEquals(1, clustered.size)
    val cluster = clustered.single() as MapPin.ClusterPin
    assertEquals(2, cluster.count)
    val raw = clusterPinsForZoom(pins, zoomBand = 11)
    assertEquals(2, raw.size)
    assertTrue(raw.none { it is MapPin.ClusterPin })
    assertEquals(setOf("p1", "p2"), raw.map { it.id }.toSet())
  }

  @Test
  fun stackSameLocationPins_singlePin_returnsSamePin() {
    val p = postPin("p1", 46.0, 6.0)
    val result = stackSameLocationPins(listOf(p))
    assertEquals(1, result.size)
    val only = result.single()
    assertTrue(only is MapPin.PostPin)
    assertEquals("p1", only.id)
    assertEquals(p.location.latitude, only.location.latitude, 1e-9)
    assertEquals(p.location.longitude, only.location.longitude, 1e-9)
  }

  @Test
  fun stackSameLocationPins_multiplePins_sameLocation_stackedIntoCluster() {
    val p1 = postPin("p1", 46.0, 6.0)
    val p2 = postPin("p2", 46.0, 6.0)
    val p3 = postPin("p3", 47.0, 7.0) // different location, should stay as-is
    val result = stackSameLocationPins(listOf(p1, p2, p3))
    assertEquals(2, result.size)
    val cluster = result.first { it is MapPin.ClusterPin } as MapPin.ClusterPin
    val lone = result.first { it.id == "p3" }
    assertEquals("stack_p1", cluster.id)
    assertEquals(2, cluster.count)
    assertEquals(p1.location.latitude, cluster.location.latitude, 1e-9)
    assertEquals(p1.location.longitude, cluster.location.longitude, 1e-9)
    assertEquals(setOf("p1", "p2"), cluster.childIds.toSet())
    assertTrue(lone is MapPin.PostPin)
    assertEquals("p3", lone.id)
  }

  @Test
  fun clusterPinsForZoom_mergesExistingClusterPinAndPost_withWeightsAndChildIds() {
    val baseLocation = Location(46.0, 6.0, "ClusterBase")
    val existingCluster =
        MapPin.ClusterPin(
            id = "c1",
            location = baseLocation,
            count = 3,
            childIds = listOf("a", "b", "c"),
        )
    val post =
        postPin(
            id = "p1",
            lat = 46.001,
            lon = 6.001,
        )
    val pins: List<MapPin> = listOf(existingCluster, post)
    val result = clusterPinsForZoom(pins, zoomBand = 8)
    assertEquals(1, result.size)
    val merged = result.single() as MapPin.ClusterPin
    assertEquals(4, merged.count)
    assertEquals(setOf("a", "b", "c", "p1"), merged.childIds.toSet())
    val expectedLat = (baseLocation.latitude * 3 + post.location.latitude * 1) / 4.0
    val expectedLon = (baseLocation.longitude * 3 + post.location.longitude * 1) / 4.0
    assertEquals(expectedLat, merged.location.latitude, 1e-6)
    assertEquals(expectedLon, merged.location.longitude, 1e-6)
  }
}
