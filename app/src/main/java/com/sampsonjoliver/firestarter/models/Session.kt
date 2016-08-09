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
        var tags: Map<String, Boolean>,
        var startDate: Long,
        var durationMs: Long
) {
    @Exclude var sessionId: String? = null
    @Exclude var startDateAsDate: Date = Date()
        get() = Date(startDate)
        set

    @Exclude var endDate: Long = 0
        get() = startDate + durationMs
        set

    @Exclude var endDateAsDate: Date = Date()
        get() = Date(startDate + durationMs)
        set

    fun getLocation(): LatLng = LatLng(lat, lng)
    fun setLocation(value: LatLng) {

            lat = value.latitude
            lng = value.longitude
        }

    constructor() : this("", "", "", "", "", 0.0, 0.0, "", "", emptyMap(), 0, 0)
}