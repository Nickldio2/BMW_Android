package com.example.mapboxtest

import com.mapbox.common.location.Location
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver


class CustomLocationObserver(
    private val mapFragment: MapFragment,
) {

    val locationObserver = object : LocationObserver {

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {

            val location = locationMatcherResult.enhancedLocation
            val speed = location.speed ?: 0.0
            val keyPoints = locationMatcherResult.keyPoints

            if (speed >= 5.0) {
                mapFragment.handleNewLocation(location, speed, keyPoints)
            }

        }

        override fun onNewRawLocation(rawLocation: Location) {

            val speed = rawLocation.speed ?: 0.0

            if (speed < 5.0) {
                mapFragment.handleNewLocation(rawLocation, speed, emptyList())
            }

        }
    }
}