package com.sampsonjoliver.firestarter.service

import android.support.annotation.StringDef
import android.util.Log
import com.firebase.geofire.*
import com.google.firebase.database.*
import com.sampsonjoliver.firestarter.models.Session
import com.sampsonjoliver.firestarter.utils.TAG
import java.util.concurrent.atomic.AtomicInteger


@StringDef(References.Sessions, References.Users)
@kotlin.annotation.Retention(kotlin.annotation.AnnotationRetention.SOURCE)
annotation class Reference

object References {
    const val Sessions = "sessions"
    const val Messages = "messages"
    const val SessionSubscriptions = "sessionSubscriptions"
    const val UserSubscriptions = "userSubscriptions"
    const val Users = "users"
    const val GeoSessions = "geosessions"
    const val NumUsers = "numUsers"
}

object FirebaseService {
    val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance().apply { setPersistenceEnabled(true) } }

    fun getReference(@Reference ref: String): DatabaseReference {
        return database.getReference(ref)
    }

    fun geoQueryForSessions(listener: GeoQueryEventListener, latitude: Double, longitude: Double, radiusKm: Double): GeoQuery {
        val ref = FirebaseDatabase.getInstance().getReference(References.GeoSessions)
        val geoFire = GeoFire(ref)
        val geoQuery = geoFire.queryAtLocation(GeoLocation(latitude, longitude), radiusKm)
        geoQuery.addGeoQueryEventListener(listener)

        return geoQuery
    }

    fun getLocationForSession(sessionId: String, listener: LocationCallback) {
        // Listen for location for a single object
        val ref = FirebaseDatabase.getInstance().getReference(References.GeoSessions)
        val geoFire = GeoFire(ref)
        geoFire.getLocation(sessionId, listener)
    }

    fun createSession(session: Session, onFinish: () -> Unit = {}, onError: () -> Unit = {}) {
        FirebaseService.getReference(References.Sessions)
                .push()
                .setValue(session, DatabaseReference.CompletionListener { databaseError, databaseReference ->
                    Log.w(this@FirebaseService.TAG, "Session onPushMessage: error=" + databaseError?.message)

                    if (databaseError == null) {
                        val ref = FirebaseDatabase.getInstance().getReference(References.GeoSessions)
                        val geoFire = GeoFire(ref)

                        geoFire.setLocation(databaseReference.key, GeoLocation(session.lat, session.lng)) { key: String, databaseError: DatabaseError? ->
                            if (databaseError != null) {
                                onError()
                            } else {
                                FirebaseService.getReference(References.SessionSubscriptions)
                                        .child(databaseReference.key)
                                        .child(SessionManager.getUid())
                                        .setValue(true, DatabaseReference.CompletionListener { databaseError, databaseReference ->
                                            Log.w(this@FirebaseService.TAG, "SessionSubscription onPushMessage: error=" + databaseError?.message)
                                            if (databaseError == null) {
                                                onFinish()
                                            } else {
                                                onError()
                                            }
                                        })
                            }
                        }
                    } else {
                        onError()
                    }
                })
    }

    fun updateSessionSubscription(sessionId: String, isUnsubscribe: Boolean, onFinish: () -> Unit = {}) {
        val otherTasksDone = AtomicInteger(0)

        FirebaseService.getReference(References.SessionSubscriptions)
                .child(sessionId).child(References.NumUsers)
                .runTransaction(object : Transaction.Handler {
                    override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot: DataSnapshot?) {
                        Log.d(TAG, "postTransaction:onComplete:" + databaseError)
                        if (otherTasksDone.incrementAndGet() == 3) {
                            onFinish()
                        }
                    }

                    override fun doTransaction(mutableData: MutableData?): Transaction.Result {
                        mutableData?.value = (mutableData?.value as? Long)?.apply {
                            if (isUnsubscribe) dec() else inc()
                        }
                        return Transaction.success(mutableData)
                    }
                })

        FirebaseService.getReference(References.SessionSubscriptions)
                .child(sessionId).child(SessionManager.getUid())
                .runTransaction(object : Transaction.Handler {
                    override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot: DataSnapshot?) {
                        Log.d(TAG, "postTransaction:onComplete:" + databaseError)
                        if (otherTasksDone.incrementAndGet() == 3) {
                            onFinish()
                        }
                    }

                    override fun doTransaction(mutableData: MutableData?): Transaction.Result {
                        mutableData?.value = isUnsubscribe.not()
                        return Transaction.success(mutableData)
                    }
                })

        FirebaseService.getReference(References.UserSubscriptions)
                .child(SessionManager.getUid()).child(sessionId)
                .runTransaction(object : Transaction.Handler {
                    override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot: DataSnapshot?) {
                        Log.d(TAG, "postTransaction:onComplete:" + databaseError)
                        if (otherTasksDone.incrementAndGet() == 3) {
                            onFinish()
                        }
                    }

                    override fun doTransaction(mutableData: MutableData?): Transaction.Result {
                        mutableData?.value = isUnsubscribe.not()
                        return Transaction.success(mutableData)
                    }
                })
    }
}