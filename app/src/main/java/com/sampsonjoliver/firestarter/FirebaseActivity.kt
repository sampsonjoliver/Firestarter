package com.sampsonjoliver.firestarter

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.sampsonjoliver.firestarter.service.SessionManager
import com.sampsonjoliver.firestarter.views.login.LoginActivity

abstract class FirebaseActivity : AppCompatActivity(),
        SessionManager.SessionAuthListener {

    override fun onLogin() {

    }

    override fun onLogout() {
        applicationContext.startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SessionManager.startSession(this, this)
    }

    override fun onStop() {
        super.onStop()
        SessionManager.stopSession()
    }
}