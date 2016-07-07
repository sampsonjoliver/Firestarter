package com.sampsonjoliver.firestarter

import android.os.Bundle

class LaunchActivity : FirebaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
    }

    override fun onLogin() {

    }
}