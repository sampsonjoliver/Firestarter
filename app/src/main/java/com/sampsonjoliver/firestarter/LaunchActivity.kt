package com.sampsonjoliver.firestarter

import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.sampsonjoliver.firestarter.views.main.HomeActivity

class LaunchActivity : FirebaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
    }

    override fun onLogin() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}