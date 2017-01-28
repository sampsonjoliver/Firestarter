package com.sampsonjoliver.firestarter.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object IntentUtils {
    val RC_SIGN_IN = 100
    val REQUEST_PLACE_PICKER = 101
    val REQUEST_IMAGE_CAPTURE = 102
    val REQUEST_IMAGE_PICKER = 103

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

    fun dispatchPickPhotoIntent(activity: Activity) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        activity.startActivityForResult(intent, REQUEST_IMAGE_PICKER)
    }

    fun dispatchTakePictureIntent(activity: Activity): String? {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Ensure that there's a camera activity to handle the intent
        takePictureIntent.resolveActivity(activity.packageManager)?.run {
            // Create the File where the photo should go
            try {
                val (photoFile, photoPath) = createImageFile(activity)

                // Continue only if the File was successfully created
                val photoURI = FileProvider.getUriForFile(activity, "com.sampsonjoliver.firestarter.fileprovider", photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                activity.startActivityForResult(takePictureIntent, IntentUtils.REQUEST_IMAGE_CAPTURE)

                return photoPath
            } catch (ex: IOException) {
                // Error occurred while creating the File
            }
        }

        return null
    }

    @Throws(IOException::class)
    private fun createImageFile(context: Context): Pair<File, String> {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */)

        // Save a file: path for use with ACTION_VIEW intents
        val currentPhotoPath = "file:" + image.absolutePath
        return Pair(image, currentPhotoPath)
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