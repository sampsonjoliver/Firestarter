package com.sampsonjoliver.firestarter.models

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import java.util.*

class Session(
        var userId: String,
        var username: String,
        var topic: String,
        var description: String,
        var bannerUrl: String,
        var lat: Double,
        var lng: Double,
        var address: String,
        var url: String,
        var tags: MutableMap<String, Boolean>,
        var startDate: Long,
        var durationMs: Long
) {
    @Exclude var sessionId: String? = null
    @Exclude var startDateAsDate: Date = Date()
        @Exclude get() = Date(startDate)

    @Exclude var endDate: Long = 0
        @Exclude get() = startDate + durationMs

    @Exclude var endDateAsDate: Date = Date()
        @Exclude get() = Date(startDate + durationMs)

    @Exclude fun getLocation(): LatLng = LatLng(lat, lng)
    @Exclude fun setLocation(value: LatLng) {
            lat = value.latitude
            lng = value.longitude
        }

    constructor() : this("", "", "", "", "", 0.0, 0.0, "", "", mutableMapOf(), 0, 0)
}