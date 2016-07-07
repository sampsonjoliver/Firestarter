package com.sampsonjoliver.firestarter.service

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserInfo
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

    fun getUid(context: Context) = getDefaultPrefs(context).getString(PREF_USER_ID, null)

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
}