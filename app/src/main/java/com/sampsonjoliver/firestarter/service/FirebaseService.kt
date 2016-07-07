package com.sampsonjoliver.firestarter.service

import android.support.annotation.StringDef
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


@StringDef(References.Sessions, References.Users)
@kotlin.annotation.Retention(kotlin.annotation.AnnotationRetention.SOURCE)
annotation class Reference

object References {
    const val Sessions = "sessions"
    const val Messages = "messages"
    const val SessionSubscriptions = "sessionSubscriptions"
    const val UserSubscriptions = "userSubscriptions"
    const val Users = "users"
}

object FirebaseService {
    val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance().apply { setPersistenceEnabled(true) } }

    fun getReference(@Reference ref: String): DatabaseReference {
        return database.getReference(ref)
    }
}