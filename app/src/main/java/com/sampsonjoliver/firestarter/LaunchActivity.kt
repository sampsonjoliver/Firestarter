package com.sampsonjoliver.firestarter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.sampsonjoliver.firestarter.utils.TAG
import com.sampsonjoliver.firestarter.views.main.HomeActivity

class LaunchActivity : FirebaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        refreshRemoteConfig()
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
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LocationAwareActivity.PERMISSION_REQUEST_FINE_LOCATION)
                        }).show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LocationAwareActivity.PERMISSION_REQUEST_FINE_LOCATION)
            }
        } else {
            // Permissions granted, so lets continue
            startHomeActivity()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            LocationAwareActivity.PERMISSION_REQUEST_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.getOrNull(0) == PackageManager.PERMISSION_GRANTED) {
                    startHomeActivity()
                } else {
                    finish()
                }
            }
        }
    }

    fun refreshRemoteConfig() {
        val config = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
        config.setConfigSettings(configSettings)
        config.setDefaults(R.xml.config)

        config.fetch().addOnCompleteListener (this) { task ->
            if (task.isSuccessful) {
                Log.d(this@LaunchActivity.TAG, "Remote Config Fetch Succeeded")
                config.activateFetched()
            } else {
                Log.d(this@LaunchActivity.TAG, "Remote Config Fetch Failed")
            }
        }
    }

    fun startHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    override fun onLogin() {
        checkLocationPermissions()
    }
}