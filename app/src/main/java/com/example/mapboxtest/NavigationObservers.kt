package com.example.mapboxtest

import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NavigationObservers(
    private val mapFragment: MapFragment
) {

    val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = mapFragment.routeLineApi.updateTraveledRouteLine(point)
        mapFragment.routeLineView.renderRouteLineUpdate(mapFragment.mapStyle, result)
    }

    val arrivalObserver = object : ArrivalObserver {
        override fun onWaypointArrival(routeProgress: RouteProgress) {
            // Handle waypoint arrival if needed
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            // Handle starting the next leg of the route if needed
        }

        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            MapboxNavigationProvider.retrieve().stopTripSession()
            val finishedTrips = getFinishedTrips(mapFragment.requireContext()).toMutableList()
            val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val existingEntry = finishedTrips.find {
                it.name == mapFragment.currentDestinationName && it.address == mapFragment.currentDestinationAddress
            }

            if (existingEntry != null) {
                existingEntry.timestamps.add(timestamp)
                existingEntry.count += 1
                existingEntry.point = mapFragment.currentDestinationPoint ?: existingEntry.point
            } else {
                val newEntry = DestinationEntry(
                    name = mapFragment.currentDestinationName ?: "",
                    address = mapFragment.currentDestinationAddress ?: "",
                    point = mapFragment.currentDestinationPoint ?: Point.fromLngLat(0.0, 0.0),
                    timestamps = mutableListOf(timestamp),
                    count = 1
                )
                finishedTrips.add(newEntry)
            }

            saveFinishedTrips(mapFragment.requireContext(), finishedTrips)
        }
    }

    val navigationCameraStateObserver = NavigationCameraStateChangedObserver { state ->
        mapFragment.handleNavigationCameraStateChange(state)
    }

    val routesObserver = RoutesObserver { result ->
        if (result.navigationRoutes.isNotEmpty()) {
            val routeLines = result.navigationRoutes.map { NavigationRouteLine(it, null) }
            mapFragment.routeLineApi.setNavigationRouteLines(routeLines) { expected ->
                mapFragment.mapView.mapboxMap.getStyle { style ->
                    mapFragment.routeLineView.renderRouteDrawData(style, expected)
                }
            }
        }
    }

    fun cleanup() {
        //maneuverApi.cancel()
    }

}
