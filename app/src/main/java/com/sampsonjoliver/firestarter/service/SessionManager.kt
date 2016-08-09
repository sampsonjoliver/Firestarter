package com.sampsonjoliver.firestarter.service

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserInfo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.sampsonjoliver.firestarter.utils.TAG

object SessionManager {
    const val PREF_USER_ID = "PREF_USER_ID"
    const val DISPLAY_NAME = "DISPLAY_NAME"
    const val EMAIL = "EMAIL"
    const val PHOTO_URL = "PHOTO_URL"
    const val PROVIDER_ID = "PROVIDER_ID"

    lateinit var sessionListener: FirebaseAuth.AuthStateListener

    private fun getDefaultPrefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    interface SessionAuthListener {
        fun onLogin()
        fun onLogout()
    }

    fun startSession(context: Context, sessionAuthListener: SessionAuthListener) {
        sessionListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                Log.d(this@SessionManager.TAG, "onAuthStateChanged:signed_in:" + user.uid)
                SessionManager.setUserDetails(context, user)
                sessionAuthListener.onLogin()
            } else {
                // User is signed out
                Log.d(this@SessionManager.TAG, "onAuthStateChanged:signed_out")
                sessionAuthListener.onLogout()
            }
        }
        FirebaseAuth.getInstance().addAuthStateListener(sessionListener)
    }

    fun stopSession() {
        FirebaseAuth.getInstance().removeAuthStateListener(sessionListener)
    }

    fun getUid() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    fun getUsername() = FirebaseAuth.getInstance().currentUser?.displayName ?: ""

    fun getUserPhotoUrl(): String {
        return FirebaseAuth.getInstance().currentUser?.providerData?.find { it.photoUrl?.toString().isNullOrBlank().not() }?.photoUrl?.toString() ?: ""
    }

    fun getUserPhotoUrl(context: Context): String {
        return FirebaseAuth.getInstance().currentUser?.providerData?.find { it.photoUrl?.toString().isNullOrBlank().not() }?.photoUrl?.toString() ?: ""
    }

    fun setUserDetails(context: Context, userInfo: UserInfo) {
        getDefaultPrefs(context).edit()
                .putString(PREF_USER_ID, userInfo.uid)
                .putString(DISPLAY_NAME, userInfo.displayName)
                .putString(EMAIL, userInfo.email)
                .putString(PHOTO_URL, userInfo.photoUrl?.path)
                .putString(PROVIDER_ID, userInfo.providerId)
                .commit()
    }

    fun clearUserDetails(context: Context) {
        getDefaultPrefs(context).edit().clear().commit()
    }

    fun registerPresenceObserver(observer: ValueEventListener) {
        FirebaseService.database.getReference(".info/connected").addValueEventListener(observer)
    }

    fun deregisterPresenceObserver(observer: ValueEventListener) {
        FirebaseService.database.getReference(".info/connected").removeEventListener(observer)
    }

    fun initPresenceTracking(uid: String) {
        val userConnRef = FirebaseService.database.getReference("${References.Users}/$uid/connections")
        val userPresenceRef = FirebaseService.database.getReference("${References.Users}/$uid/lastOnline")
        val connRef = FirebaseService.database.getReference(".info/connected")

        connRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError?) {
                System.err.println("Listener was cancelled at .info/connected");
            }

            override fun onDataChange(snapshot: DataSnapshot?) {
                val connected = snapshot?.getValue(Boolean::class.java) ?: false
                if (connected) {
                    // add this device to my connections list
                    // this value could contain info about the device or a timestamp too
                    val con = userConnRef.push()
                    con.setValue(true);

                    // when this device disconnects, remove it
                    con.onDisconnect().removeValue();

                    // when I disconnect, update the last time I was seen online
                    userPresenceRef.onDisconnect().setValue(ServerValue.TIMESTAMP);
                }
            }
        })
    }
}