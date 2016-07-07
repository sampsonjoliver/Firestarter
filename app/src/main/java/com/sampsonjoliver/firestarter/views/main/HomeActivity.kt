package com.sampsonjoliver.firestarter.views.main

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.sampsonjoliver.firestarter.FirebaseActivity
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Session
import com.sampsonjoliver.firestarter.service.FirebaseService
import com.sampsonjoliver.firestarter.service.References
import com.sampsonjoliver.firestarter.service.SessionManager
import com.sampsonjoliver.firestarter.utils.TAG
import com.sampsonjoliver.firestarter.utils.insertSorted
import kotlinx.android.synthetic.main.content_main.*

class HomeActivity : FirebaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    val sessionListListener = object : ChildEventListener {
        override fun onChildMoved(p0: DataSnapshot?, previousChildName: String?) {
            Log.w(this.TAG, "onChildMoved: ${p0?.key}")
            // todo re-order the displayed list
        }

        override fun onChildChanged(p0: DataSnapshot?, previousChildName: String?) = Unit

        override fun onChildAdded(p0: DataSnapshot?, previousChildName: String?) {
            Log.w(this.TAG, "onChildAdded: ${p0?.key}")

            val key = p0?.key ?: ""
            FirebaseService.getReference(References.Sessions)
                    .child(key)
                    .addValueEventListener(sessionListener)
        }

        override fun onChildRemoved(p0: DataSnapshot?) {
            Log.w(this.TAG, "onChildRemoved: ${p0?.key}")
            val key = p0?.key ?: ""
            FirebaseService.getReference(References.Sessions)
                    .child(key)
                    .removeEventListener(sessionListener)
        }

        override fun onCancelled(p0: DatabaseError?) {
            Log.w(this.TAG, "onCancelled", p0?.toException());
            Toast.makeText(this@HomeActivity, "Failed to load sessions list.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    val sessionListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError?) {
            Log.w(this@HomeActivity.TAG, "onCancelled")
        }

        override fun onDataChange(p0: DataSnapshot?) {
            Log.w(this@HomeActivity.TAG, "onDataChange: ${p0?.key}")
            val session = p0?.getValue(Session::class.java)
            session?.sessionId = p0?.key

            if (session != null) {
                var index = adapter?.sessions?.indexOfFirst { it.sessionId == p0?.key } ?: -1
                if (index != -1) {
                    adapter?.sessions?.set(index, session)
                    adapter?.notifyItemChanged(index)
                } else {
                    val key = p0?.key
                    if (key != null) {
                        index = adapter?.sessions?.insertSorted(session) { it.topic } ?: -1
                        if (index != -1) adapter?.notifyItemInserted(index)
                    }
                }
            }
        }
    }

    val adapter: HomeRecyclerAdapter? = HomeRecyclerAdapter(LatLng(-37.8148752,144.9623464), object : HomeRecyclerAdapter.OnSessionClickedListener {
        override fun onSessionClicked(session: Session) {
            // todo launch session chat activity
        }
    })

    fun logout() {
        attachDataWatcher(detach = true)
        FirebaseAuth.getInstance().signOut()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SessionManager.getUid(this)

        setContentView(R.layout.activity_home)
        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)

        val fab = findViewById(R.id.fab) as FloatingActionButton?
        fab!!.setOnClickListener { view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show() }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout?
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer?.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById(R.id.nav_view) as NavigationView?
        navigationView?.setNavigationItemSelectedListener(this)

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)
        attachDataWatcher()
    }

    fun attachDataWatcher(detach: Boolean = false) {
        // todo below are two methods for getting user subscriptions, presented for robustness
        // ideally we should not be using both of these
        if (detach) {
            FirebaseService.getReference(References.Sessions)
                    .orderByChild("userId").equalTo(SessionManager.getUid(this))
                    .removeEventListener(sessionListListener)

            FirebaseService.getReference(References.UserSubscriptions)
                    .child(SessionManager.getUid(this))
                    .orderByChild("startDate")
                    .removeEventListener(sessionListListener)
        } else {
            FirebaseService.getReference(References.Sessions)
                    .orderByChild("userId").equalTo(SessionManager.getUid(this))
                    .addChildEventListener(sessionListListener)

            FirebaseService.getReference(References.UserSubscriptions)
                    .child(SessionManager.getUid(this))
                    .orderByChild("startDate")
                    .addChildEventListener(sessionListListener)
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout?
        if (drawer!!.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    @SuppressWarnings("StatementWithEmptyBody")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_logout -> logout()
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout?
        drawer?.closeDrawer(GravityCompat.START)
        return true
    }
}
