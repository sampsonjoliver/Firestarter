package com.sampsonjoliver.firestarter.utils

import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

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