package com.mikatechnology.BusTracker.ui.map

import com.google.android.gms.maps.model.LatLng
import com.mikatechnology.BusTracker.data.model.DriverLocation

object DriverRouteDisplay {
  fun polylinePoints(
      route: List<LatLng>,
      driverLocation: DriverLocation?,
      isTripActive: Boolean
  ): List<LatLng> {
    if (!isTripActive) return route

    val live = driverLocation?.let { LatLng(it.latitude, it.longitude) } ?: return route

    if (route.isEmpty()) {
      return listOf(live, live)
    }

    val points = route.toMutableList()
    points.add(live)
    return points
  }

  fun shouldDrawPolyline(
      route: List<LatLng>,
      driverLocation: DriverLocation?,
      isTripActive: Boolean
  ): Boolean {
    return isTripActive && polylinePoints(route, driverLocation, isTripActive).isNotEmpty()
  }
}
