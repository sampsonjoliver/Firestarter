package com.sampsonjoliver.firestarter.service

import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.sampsonjoliver.firestarter.utils.TAG

class FirestarterInstanceIdService : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        super.onTokenRefresh()

        // Get updated InstanceID token.
        val refreshedToken = FirebaseInstanceId.getInstance().token
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        refreshedToken?.let { token -> sendRegistrationToServer(refreshedToken) }
    }

    fun sendRegistrationToServer(token: String) {
        if (SessionManager.userActiveSession()) {
            Log.d(TAG, "Registered Instance ID to user ${SessionManager.getUid()}")
            FirebaseService.getReference(References.Users)
                    .child(SessionManager.getUid())
                    .child(References.UserInstanceIds)
                    .child(token)
                    .setValue(true)
        }
    }
}