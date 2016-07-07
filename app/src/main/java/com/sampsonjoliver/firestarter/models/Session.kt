package com.sampsonjoliver.firestarter.models

import com.google.android.gms.maps.model.LatLng
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
        var tags: Map<String, Boolean>,
        var startDate: Long,
        var durationMs: Long
) {
    var sessionId: String? = null
    var startDateAsDate: Date = Date()
        get() = Date(startDate)
        set

    var location: LatLng
        get() = LatLng(lat, lng)
        set(value) {
            lat = value.latitude
            lng = value.longitude
        }

    constructor() : this("", "", "", "", "", 0.0, 0.0, "", "", emptyMap(), 0, 0)
}