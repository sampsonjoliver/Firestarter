package com.sampsonjoliver.firestarter.views.main

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.sampsonjoliver.firestarter.LocationAwareActivity
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Session
import com.sampsonjoliver.firestarter.service.FirebaseService
import com.sampsonjoliver.firestarter.service.References
import com.sampsonjoliver.firestarter.service.SessionManager
import com.sampsonjoliver.firestarter.utils.*
import com.sampsonjoliver.firestarter.views.channel.ChannelActivity
import com.sampsonjoliver.firestarter.views.channel.create.CreateChannelActivity
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class HomeActivity : LocationAwareActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleMap.OnCameraChangeListener,
        GeoQueryEventListener {

    companion object {
        const val LOCATION_KEY = "LOCATION_KEY"
        const val MAP_LOCATION_RADIUS_KM = 0.6
    }

    val sessionSubscriptionListener = object : ChildEventListener {
        override fun onChildMoved(dataSnapshot: DataSnapshot?, previousChildName: String?) {
            Log.w(this.TAG, "onChildMoved: ${dataSnapshot?.key}")
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot?, previousChildName: String?) {
            Log.w(this@HomeActivity.TAG, "onDataChange: ${dataSnapshot?.key}")
        }

        override fun onChildAdded(dataSnapshot: DataSnapshot?, previousChildName: String?) {
            Log.w(this.TAG, "onChildAdded: ${dataSnapshot?.key}")

            dataSnapshot?.key?.let { key ->
                FirebaseService.getReference(References.Sessions)
                        .child(key)
                        .addValueEventListener(subscribedSessionListener)
            }
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot?) {
            Log.w(this.TAG, "onChildRemoved: ${dataSnapshot?.key}")

            dataSnapshot?.key?.let { key ->
                FirebaseService.getReference(References.Sessions)
                        .child(key)
                        .removeEventListener(subscribedSessionListener)

                adapter?.subscribedSessions?.indexOfFirst { it.sessionId == key }.whenNotEqual(-1) {
                    it?.let {
                        adapter?.notifyItemRemoved(adapter.subscribedSessionIndexToAdapterIndex(it))
                        adapter?.subscribedSessions?.removeAt(it)
                        if (adapter?.subscribedSessions?.size == 0)
                            adapter?.notifyItemChanged(adapter.getSubscribedHeaderIndex())
                    }
                }
            }
        }

        override fun onCancelled(p0: DatabaseError?) {
            Log.w(this.TAG, "onCancelled", p0?.toException())
            Toast.makeText(this@HomeActivity, "Failed to load sessions list.", Toast.LENGTH_SHORT).show()
        }
    }

    val subscribedSessionListener = object : ValueEventListener {
        override fun onCancelled(dataSnapshot: DatabaseError?) {
            Log.w(this@HomeActivity.TAG, "onCancelled")
        }

        override fun onDataChange(dataSnapshot: DataSnapshot?) {
            Log.w(this@HomeActivity.TAG, "onDataChange: ${dataSnapshot?.key}")

            dataSnapshot?.getValue(Session::class.java)?.let { session ->
                session.sessionId = dataSnapshot.key
                var index = adapter?.subscribedSessions?.indexOfFirst { it.sessionId == session.sessionId } ?: -1

                if (index == -1) {
                    index = adapter?.subscribedSessions?.insertSorted(session) { it.topic } ?: -1
                    if (adapter?.subscribedSessions?.size == 1) {
                        adapter?.notifyItemInserted(adapter.getSubscribedHeaderIndex())
                    }
                    adapter?.notifyItemInserted(adapter.subscribedSessionIndexToAdapterIndex(index))
                } else {
                    adapter?.subscribedSessions?.set(index, session)
                    adapter?.notifyItemChanged(adapter.subscribedSessionIndexToAdapterIndex(index))
                }
            }
        }
    }

    val nearbySessionListener = object : ValueEventListener {
        override fun onCancelled(dataSnapshot: DatabaseError?) {
            Log.w(this@HomeActivity.TAG, "onCancelled")
        }

        override fun onDataChange(dataSnapshot: DataSnapshot?) {
            Log.w(this@HomeActivity.TAG, "onDataChange: ${dataSnapshot?.key}")

            dataSnapshot?.getValue(Session::class.java)?.let { session ->
                session.sessionId = dataSnapshot.key
                var index = adapter?.nearbySessions?.indexOfFirst { it.sessionId == session.sessionId } ?: -1

                if (index == -1) {
                    index = adapter?.nearbySessions?.insertSorted(session) { it.topic } ?: -1
                    adapter?.notifyItemInserted(adapter.nearbySessionIndexToAdapterIndex(index))
                } else {
                    adapter?.nearbySessions?.set(index, session)
                    adapter?.notifyItemChanged(adapter.nearbySessionIndexToAdapterIndex(index))
                }
            }
        }
    }

    val adapter: HomeRecyclerAdapter = HomeRecyclerAdapter(object : HomeRecyclerAdapter.OnSessionClickedListener {
        override fun onSessionClicked(session: Session) {
            startActivity(Intent(this@HomeActivity, ChannelActivity::class.java).apply {
                putExtra(ChannelActivity.EXTRA_SESSION_ID, session.sessionId)
            })
        }
    })

    val behavior: BottomSheetBehavior<RecyclerView>
        get() = BottomSheetBehavior.from(recycler)

    var googleMap: GoogleMap? = null
    var mapMarkers: MutableMap<String, Marker> = mutableMapOf()
    var geoQuery: GeoQuery? = null
    val googleMapFragment: MapFragment
            get() = fragmentManager.findFragmentById(R.id.mapFragment) as MapFragment
    var isGeoQueryAttached = false
    var mapManuallyPanned = false

    override fun onGeoQueryReady() = Unit

    override fun onKeyEntered(key: String?, location: GeoLocation?) {
        googleMap?.run {
            if (key != null) {
                addMapMarker(key, location.latLng())

                FirebaseService.getReference(References.Sessions)
                        .child(key)
                        .addValueEventListener(nearbySessionListener)
            }
        }
    }

    override fun onKeyMoved(key: String?, location: GeoLocation?) {
        mapMarkers[key]?.animateMarkerTo(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
    }

    override fun onKeyExited(key: String?) {
        mapMarkers.remove(key)?.remove()

        FirebaseService.getReference(References.Sessions)
                .child(key)
                .removeEventListener(nearbySessionListener)
    }

    override fun onGeoQueryError(error: DatabaseError?) {
        Snackbar.make(recycler, "There was an error querying Geofire: " + error?.message, Snackbar.LENGTH_LONG).show()
    }

    override fun onConnected(connectionHint: Bundle?) {
        super.onConnected(connectionHint)
        startLocationUpdatesWithChecks()
    }

    override fun onLocationChanged(location: Location?) {
        // Update the UI with the latest location
        adapter.location = location
        tryCenterMapOnUser()
    }

    fun logout() {
        attachDataWatcher(detach = true)
        FirebaseAuth.getInstance().signOut()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        this.googleMap = googleMap

        googleMap?.clear()

        googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.run {
            isMyLocationButtonEnabled = true
            isIndoorLevelPickerEnabled = false
        }
        googleMap?.setOnCameraChangeListener(this)
        googleMap?.setOnMapClickListener {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bindOverlayData(emptyList(), adapter.location)
        }
        googleMap?.setOnMarkerClickListener { marker ->
            val sessionsAtMarker = adapter.nearbySessions.filter { it.getLocation() == marker.position }
                    .plus(adapter.subscribedSessions.filter { it.getLocation() == marker.position })
                    .distinctBy { it.sessionId }
            consume { bindOverlayData(sessionsAtMarker, adapter.location) }
        }

        // todo add all markers already loaded
        if (adapter.location != null)
            tryCenterMapOnUser()
    }

    fun bindOverlayData(sessions: List<Session>, userLocation: Location?) {
        itemOverlay.appear = sessions.isNotEmpty()
        if (sessions.size > 1) {
            sessionImage.appear = false
            sessionName.text = getString(R.string.n_sessions_at_location, sessions.size)
            sessionDistance.appear = false
        } else if (sessions.size == 1) {
            sessionImage.appear = true
            sessionDistance.appear = true
            sessionImage.setImageURI(sessions.first().bannerUrl)
            sessionName.text = sessions.first().topic
            sessionDistance.text = DistanceUtils.formatDistance(LatLng(userLocation?.latitude ?: 0.0, userLocation?.longitude ?: 0.0), sessions.first().getLocation())
        }
    }

    fun tryCenterMapOnUser() {
        if (!mapManuallyPanned) {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(adapter.location.latLng(), 13f))
        }
    }

    fun addMapMarker(key: String, latLng: LatLng) {
        // create and add a map marker
        googleMap?.run {
            val marker = addMarker(MarkerOptions().position(latLng))
            mapMarkers.put(key, marker)
        }
    }

    override fun onCameraChange(cameraPosition: CameraPosition?) {
        if (cameraPosition?.target != adapter.location.latLng())
            mapManuallyPanned = true

        // Update geofire query
        val center = Location("center").apply {
            latitude = googleMap?.projection?.visibleRegion?.latLngBounds?.center?.latitude ?: 0.0
            longitude = googleMap?.projection?.visibleRegion?.latLngBounds?.center?.longitude ?: 0.0
        }

        val corner = Location("corner").apply {
            latitude = googleMap?.projection?.visibleRegion?.farRight?.latitude ?: 0.0
            longitude = googleMap?.projection?.visibleRegion?.farRight?.longitude ?: 0.0
        }

        val radiusMeters = center.distanceTo(corner)
        geoQuery?.center = GeoLocation(center.latitude, center.longitude)
        geoQuery?.radius = Math.max(Math.min(radiusMeters / 1000.0, 10.0), 0.1)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putParcelable(LOCATION_KEY, adapter.location)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        updateValuesFromBundle(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        attachDataWatcher()
    }

    override fun onPause() {
        super.onStop()
        attachDataWatcher(true)
    }

    fun initView() {
        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        googleMapFragment.getMapAsync(this)

        fab.setOnClickListener {
            startActivity(Intent(this, CreateChannelActivity::class.java))
        }

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout?.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById(R.id.nav_view) as NavigationView?
        navigationView?.setNavigationItemSelectedListener(this)

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)
        (recycler.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            if (savedInstanceState.keySet().contains(LOCATION_KEY))
                adapter.location = savedInstanceState.getParcelable(LOCATION_KEY)
        }
    }

    fun attachDataWatcher(detach: Boolean = false) {
        // Sessions owned by the user
        val sessionQuery = FirebaseService
                .getReference(References.Sessions)
                .orderByChild("userId")
                .equalTo(SessionManager.getUid())

        // Sessions the user is subscribed to
        val userSubscriptionQuery = FirebaseService
                .getReference(References.UserSubscriptions)
                .child(SessionManager.getUid())
                .orderByChild("startDate")

        if (detach) {
            sessionQuery.removeEventListener(sessionSubscriptionListener)
            userSubscriptionQuery.removeEventListener(sessionSubscriptionListener)

            adapter.subscribedSessions.plus(adapter.nearbySessions).forEach {
                FirebaseService.getReference(References.Sessions)
                        .child(it.sessionId)
                        .removeEventListener(subscribedSessionListener)
            }

            if (isGeoQueryAttached) {
                geoQuery?.removeGeoQueryEventListener(this)
                isGeoQueryAttached = false
            }
        } else {
            sessionQuery.addChildEventListener(sessionSubscriptionListener)
            userSubscriptionQuery.addChildEventListener(sessionSubscriptionListener)

            geoQuery = FirebaseService.geoQueryForSessions(this, adapter.location?.latitude ?: 0.0, adapter.location?.longitude ?: 0.0, MAP_LOCATION_RADIUS_KM)
            isGeoQueryAttached = true
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawerLayout) as DrawerLayout?
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

        val drawer = findViewById(R.id.drawerLayout) as DrawerLayout?
        drawer?.closeDrawer(GravityCompat.START)
        return true
    }
}
