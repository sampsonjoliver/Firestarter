package com.sampsonjoliver.firestarter.utils

import android.location.Location

object DistanceUtils {
    const val METERS_IN_MILE = 1609.34f
    const val METERS_IN_KM = 1000.0f
    const val METERS_IN_FOOT = 0.3048f

    enum class DistanceUnits {
        METRIC,
        IMPERIAL
    }

    enum class DistanceResolution {
        MINOR,
        MAJOR,
        AUTO
    }

    fun formatDistance(pos1: LatLng, pos2: LatLng, unit: DistanceUnits = DistanceUnits.METRIC, minResolution: DistanceResolution = DistanceResolution.AUTO): String {
        return formatDistance(latLngDistance(pos1, pos2).get(0), unit, minResolution)
    }

    fun formatDistance(distanceInMeters: Float, unit: DistanceUnits = DistanceUnits.METRIC, minResolution: DistanceResolution = DistanceResolution.AUTO): String {
        var distance: Float
        var determinedResolution = minResolution
        if (unit == DistanceUnits.METRIC) {
            if (minResolution == DistanceResolution.MAJOR) {
                distance = distanceInMeters / METERS_IN_KM
            } else if (minResolution == DistanceResolution.MINOR) {
                distance = distanceInMeters
            } else {
                distance = if (distanceInMeters / METERS_IN_KM < 1) distanceInMeters else distanceInMeters / METERS_IN_KM
                determinedResolution = if (distanceInMeters / METERS_IN_KM < 1) DistanceResolution.MINOR else DistanceResolution.MAJOR
            }
        } else {
            if (minResolution == DistanceResolution.MAJOR) {
                distance = distanceInMeters / METERS_IN_MILE
            } else if (minResolution == DistanceResolution.MINOR) {
                distance = distanceInMeters / METERS_IN_FOOT
            } else {
                distance = if (distanceInMeters / METERS_IN_MILE < 1) distanceInMeters / METERS_IN_FOOT else distanceInMeters / METERS_IN_MILE
                determinedResolution = if (distanceInMeters / METERS_IN_MILE < 1) DistanceResolution.MINOR else DistanceResolution.MAJOR
            }
        }

        val unitStr = if (unit == DistanceUnits.METRIC) {
            if (determinedResolution == DistanceResolution.MAJOR) { "km" }
            else { "m" }
        } else {
            if (determinedResolution == DistanceResolution.MAJOR) { "mi" }
            else { "ft" }
        }

        return String.format(if (determinedResolution == DistanceResolution.MAJOR) "%.2f %s" else "%.0f %s", distance, unitStr)
    }

    /**
     * The computed distance is stored in results[0].
     * If results has length 2 or greater, the initial bearing is stored in results[1].
     * If results has length 3 or greater, the final bearing is stored in results[2].
     */
    fun latLngDistance(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): FloatArray {
        val result = FloatArray(3)
        Location.distanceBetween(latitude1, longitude1, latitude2, longitude2, result)
        return result
    }
}