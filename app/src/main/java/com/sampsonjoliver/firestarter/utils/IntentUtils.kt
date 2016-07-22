package com.sampsonjoliver.firestarter.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.android.gms.maps.model.LatLng

object IntentUtils {
    val RC_SIGN_IN = 100

    val MAP_GEO_URI = "geo:%f,%f?q=%f,%f"
    val MAP_GEO_LABELLED_URI = "geo:%f,%f?q=%f,%f(%s)"
    val MAP_NAV_URI = "google.navigation:q=%f,%f"

    fun launchMaps(context: Context, latLng: LatLng, label: String? = null) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if (label == null)
            MAP_GEO_URI.format(latLng.latitude, latLng.longitude, latLng.latitude, latLng.longitude)
        else
            MAP_GEO_LABELLED_URI.format(latLng.latitude, latLng.longitude, latLng.latitude, latLng.longitude, label)
        ))
        resolveIntent(context, intent).whenEqual(true) { attemptActivity(context, intent) }
    }

    fun launchMapsNavigation(context: Context, latLng: LatLng) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(MAP_NAV_URI.format(latLng.latitude, latLng.longitude)))
        resolveIntent(context, intent).whenEqual(true) { attemptActivity(context, intent) }
    }

    fun resolveIntent(context: Context, intent: Intent): Boolean {
        val resolution = intent.resolveActivity(context.packageManager)
        return resolution != null
    }

    fun attemptActivity(context: Context, intent: Intent): Boolean {
        try {
            context.startActivity(intent)
            return true
        } catch (e: ActivityNotFoundException) {
            Log.d(TAG, "Could not launch intent " + intent.toString())
        }

        return false
    }
}