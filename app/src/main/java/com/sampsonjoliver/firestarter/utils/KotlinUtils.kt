package com.sampsonjoliver.firestarter.utils

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.SystemClock
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

val <T : Any> T.TAG: String
    get() = this.javaClass.name

fun ViewGroup.inflate(@LayoutRes layoutId: Int, attachToParent: Boolean): View {
    return LayoutInflater.from(this.context).inflate(layoutId, this, attachToParent)
}

inline fun <T, R: Comparable<R>> MutableCollection<T>.insertSorted(obj: T, crossinline selector: (T) -> R?): Int {
    this.add(obj)
    this.sortedBy(selector)
    return this.indexOf(obj)
}

var View.visible: Boolean
    get() = this.visibility == View.VISIBLE
    set(value) {
        this.visibility = if (value) View.VISIBLE else View.INVISIBLE
    }

var View.appear: Boolean
    get() = this.visibility == View.VISIBLE
    set(value) {
        this.visibility = if (value) View.VISIBLE else View.GONE
    }

fun Any?.exists():Boolean = this != null

inline fun consume(func: () -> Unit): Boolean { func(); return true }

inline fun <T> T.whenEqual(that: T, block: (T) -> Unit) {
    if (this == that) block.invoke(this)
}

inline fun <T> T.whenNotEqual(that: T, block: (T) -> Unit) {
    if (this != that) block.invoke(this)
}

fun View.setBackgroundResourcePreservePadding(@LayoutRes res: Int) {
    val padBottom = paddingBottom
    val padTop = paddingTop
    val padLeft = paddingLeft
    val padRight = paddingRight
    val padStart = paddingStart
    val padEnd = paddingEnd
    this.setBackgroundResource(res)
    setPadding(padLeft, padTop, padRight, padBottom)
    setPaddingRelative(padStart, padTop, padEnd, padBottom)
}

fun Context.copyToClipboard(key: String, obj: String) {
    // Backwards compatible clipboard service
    val sdk = android.os.Build.VERSION.SDK_INT
    if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager
        clipboard.text = obj
    } else {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(key, obj)
        clipboard.primaryClip = clip
    }
}

// Animation handler for old APIs without animation support

fun Marker.animateMarkerTo(lat: Double, lng: Double) {
    val handler = Handler()
    val start = SystemClock.uptimeMillis()
    val DURATION_MS = 3000f
    val interpolator = AccelerateDecelerateInterpolator()
    val startPosition = position

    handler.post(object : Runnable {
        override fun run() {
            val elapsed = SystemClock.uptimeMillis() - start
            val t = elapsed / DURATION_MS
            val v = interpolator.getInterpolation(t)

            val currentLat = (lat - startPosition.latitude) * v + startPosition.latitude
            val currentLng = (lng - startPosition.longitude) * v + startPosition.longitude
            position = LatLng(currentLat, currentLng)

            // if animation is not finished yet, repeat
            if (t < 1) {
                handler.postDelayed(this, 16)
            }
        }
    })
}

fun Location?.latLng(): LatLng {
    return LatLng(this?.latitude ?: 0.0, this?.longitude ?: 0.0)
}

fun GeoLocation?.latLng(): LatLng {
    return LatLng(this?.latitude ?: 0.0, this?.longitude ?: 0.0)
}