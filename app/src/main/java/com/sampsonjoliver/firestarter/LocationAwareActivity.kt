package com.sampsonjoliver.firestarter

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.sampsonjoliver.firestarter.utils.TAG

abstract class LocationAwareActivity : FirebaseActivity(),
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    companion object {
        const val LOCATION_UPDATE_INTERVAL = 10000L
        const val LOCATION_UPDATE_MIN_INTERVAL = 5000L

        const val PERMISSION_REQUEST_FINE_LOCATION = 0
        const val REQUEST_CHECK_SETTINGS = 1
    }

    val locationRequest = LocationRequest().apply {
        interval = LOCATION_UPDATE_INTERVAL
        fastestInterval = LOCATION_UPDATE_MIN_INTERVAL
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    val googleApiClient: GoogleApiClient by lazy { GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }

    override fun onConnected(connectionHint: Bundle?) {
        // Get the last known location so we can show something ASAP, then start regular fine-grained updates
        val lastLoc = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
        Log.d(TAG, lastLoc?.toString() ?: "null")

        onLocationChanged(lastLoc)
    }

    override fun onConnectionSuspended(cause: Int) {
        // Called when the client is temporarily in a disconnected state
        if (cause == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST || cause == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, R.string.google_services_disconnected, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        // An unresolvable error has occurred and a connection to Google APIs
        // could not be established. Display an error message, or handle
        // the failure silently
        AlertDialog.Builder(this)
                .setTitle(R.string.google_services_required_title)
                .setMessage(R.string.google_services_required_message)
                .setPositiveButton(R.string.okay, { dialogInterface, i -> dialogInterface.dismiss() })
    }

    fun startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
    }

    fun startLocationUpdatesWithChecks() {
        checkLocationPermissions()
    }

    fun stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
    }

    fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation and handle successes and failures
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.location_permission_rationale_title))
                        .setMessage(getString(R.string.location_permission_rationale_message))
                        .setNegativeButton(getString(R.string.cancel), { dialogInterface, i ->
                            dialogInterface.dismiss()
                        })
                        .setPositiveButton(getString(R.string.grant), { dialogInterface, i ->
                            dialogInterface.dismiss()
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_FINE_LOCATION)
                        }).show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_FINE_LOCATION)
            }
        } else {
            // Permissions granted, so lets continue
            checkLocationSettings()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
                    checkLocationSettings()
                } else {
                    startLocationUpdatesWithChecks()
                }
            }
        }
    }

    fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())

        result.setResultCallback {
            val status = it.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.SUCCESS -> startLocationUpdates()
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    try {
                        status.startResolutionForResult(this@LocationAwareActivity, REQUEST_CHECK_SETTINGS)
                    } catch (e: IntentSender.SendIntentException) {
                        // Ignore the error.
                    }
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    // todo Location settings are not satisfied. However, we have no way
                    // to fix the settings so we won't show the dialog.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (requestCode == Activity.RESULT_OK)
                    startLocationUpdates()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onStart() {
        super.onStart()
        googleApiClient?.connect()
    }

    override fun onResume() {
        super.onResume()
        if (googleApiClient?.isConnected ?: false)
            startLocationUpdatesWithChecks()
    }

    override fun onPause() {
        super.onPause()
        if (googleApiClient?.isConnected ?: false)
            stopLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        googleApiClient?.disconnect()
    }
}