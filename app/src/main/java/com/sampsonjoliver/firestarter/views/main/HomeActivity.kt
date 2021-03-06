package com.sampsonjoliver.firestarter.views.main

import android.app.ProgressDialog
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
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
import com.google.firebase.analytics.FirebaseAnalytics
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
import com.sampsonjoliver.firestarter.utils.analytics.Events
import com.sampsonjoliver.firestarter.utils.analytics.Params
import com.sampsonjoliver.firestarter.views.channel.ChannelActivity
import com.sampsonjoliver.firestarter.views.channel.create.CreateChannelActivity
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_main_peekbar.*

class HomeActivity : LocationAwareActivity(),
        OnMapReadyCallback,
        GoogleMap.OnCameraChangeListener {

    companion object {
        const val LOCATION_KEY = "LOCATION_KEY"
        const val MAP_LOCATION_RADIUS_KM = 0.6
    }

    val progressDialog by lazy { ProgressDialog(this) }
    var searchFilters = mutableMapOf<String, Boolean>()

    // Listener attached to user's subscriptions, gets the id of each session
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

                val hasSubscriptions = adapter.hasSubscriptions()
                adapter.subscribedSessions.indexOfFirst { it.sessionId == key }.whenNotEqual(-1) {
                    adapter.notifyItemRemoved(adapter.subscribedSessionIndexToAdapterIndex(it))
                    if (hasSubscriptions && adapter.subscribedSessions.isEmpty())
                        adapter.notifyItemRemoved(adapter.getSubscribedHeaderIndex())
                }
                adapter._subscribedSessions.removeAll { it.sessionId == key }
            }
        }

        override fun onCancelled(p0: DatabaseError?) {
            Log.w(this.TAG, "onCancelled", p0?.toException())
            Toast.makeText(this@HomeActivity, "Failed to load sessions list.", Toast.LENGTH_SHORT).show()
        }
    }

    // Listener fetches details of the session
    val subscribedSessionListener = object : ValueEventListener {
        override fun onCancelled(dataSnapshot: DatabaseError?) {
            Log.w(this@HomeActivity.TAG, "onCancelled")
        }

        override fun onDataChange(dataSnapshot: DataSnapshot?) {
            Log.w(this@HomeActivity.TAG, "onDataChange: ${dataSnapshot?.key}")

            dataSnapshot?.getValue(Session::class.java)?.let { session ->
                session.sessionId = dataSnapshot.key

                val isSubscribedEmpty = adapter.hasSubscriptions().not()
                val index = adapter._subscribedSessions.indexOfFirst { it.sessionId == session.sessionId }
                if (index == -1) {
                    adapter._subscribedSessions.insertSorted(session) { it.topic }
                    if (isSubscribedEmpty && adapter.subscribedSessions.size == 1) {
                        adapter.notifyItemInserted(adapter.getSubscribedHeaderIndex())
                    }

                    val filteredIndex = adapter.subscribedSessions.indexOfFirst { it.sessionId == session.sessionId }
                    filteredIndex.whenNotEqual(-1) { adapter.notifyItemInserted(adapter.subscribedSessionIndexToAdapterIndex(filteredIndex)) }
                } else {
                    adapter._subscribedSessions[index] = session

                    val filteredIndex = adapter.subscribedSessions.indexOfFirst { it.sessionId == session.sessionId }
                    filteredIndex.whenNotEqual(-1) { adapter.notifyItemChanged(adapter.subscribedSessionIndexToAdapterIndex(filteredIndex)) }
                }
            }
        }
    }

    // Listens for nearby session keys
    var geoQueryListener = object : GeoQueryEventListener {
        override fun onGeoQueryReady() = Unit

        override fun onKeyEntered(key: String?, location: GeoLocation?) {
            googleMap?.run {
                if (key != null && (adapter.subscribedSessions.any { it.sessionId == key } || adapter.nearbySessions.any { it.sessionId == key })) {
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
            val hasNearbySessions = adapter.hasNearbySessions()
            adapter.nearbySessions.indexOfFirst { it.sessionId == key }.whenNotEqual(-1) {
                adapter.notifyItemRemoved(adapter.nearbySessionIndexToAdapterIndex(it))
            }
            adapter._nearbySessions.removeAll { it.sessionId == key }

            if (hasNearbySessions && adapter.nearbySessions.isEmpty())
                adapter.notifyItemRemoved(adapter.getNearbyHeaderIndex())

            FirebaseService.getReference(References.Sessions)
                    .child(key)
                    .removeEventListener(nearbySessionListener)
        }

        override fun onGeoQueryError(error: DatabaseError?) {
            Snackbar.make(recycler, "There was an error querying Geofire: " + error?.message, Snackbar.LENGTH_LONG).show()
            FirebaseAnalytics.getInstance(this@HomeActivity).logEvent(Events.GEO_QUERY_ERROR, Bundle().apply {
                putString(Params.STATUS, error?.code?.toString() ?: "")
                putString(Params.MESSAGE, error?.message)
            })
        }
    }

    // Get the details of nearby sessions
    val nearbySessionListener = object : ValueEventListener {
        override fun onCancelled(dataSnapshot: DatabaseError?) {
            Log.w(this@HomeActivity.TAG, "onCancelled")
        }

        override fun onDataChange(dataSnapshot: DataSnapshot?) {
            Log.w(this@HomeActivity.TAG, "onDataChange: ${dataSnapshot?.key}")

            dataSnapshot?.getValue(Session::class.java)?.let { session ->
                session.sessionId = dataSnapshot.key

                val index = adapter._nearbySessions.indexOfFirst { it.sessionId == session.sessionId }
                if (index == -1) {
                    val isNearbyEmpty = adapter.hasNearbySessions().not()
                    adapter._nearbySessions.insertSorted(session) { it.topic }
                    if (isNearbyEmpty && adapter.nearbySessions.size == 1) {
                        adapter.notifyItemInserted(adapter.getNearbyHeaderIndex())
                    }

                    val filteredIndex = adapter.nearbySessions.indexOfFirst { it.sessionId == session.sessionId }
                    filteredIndex.whenNotEqual(-1) { adapter.notifyItemInserted(adapter.nearbySessionIndexToAdapterIndex(filteredIndex)) }

                    Log.d(TAG, "Inserted nearby session at index=$index, and filteredIndex=$filteredIndex")
                } else {
                    adapter._nearbySessions[index] = session

                    val filteredIndex = adapter.nearbySessions.indexOfFirst { it.sessionId == session.sessionId }
                    filteredIndex.whenNotEqual(-1) { adapter.notifyItemChanged(adapter.nearbySessionIndexToAdapterIndex(filteredIndex)) }
                    Log.d(TAG, "Updated nearby session at index=$index, and filteredIndex=$filteredIndex")
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
        SessionManager.signout()
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

        if (adapter.location != null)
            tryCenterMapOnUser()

        refreshMapMarkers()
    }

    fun bindOverlayData(sessions: List<Session>, userLocation: Location?) {
        if (sessions.isEmpty()) peekbar.hideView() else peekbar.showView()

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

            itemOverlay.setOnClickListener {
                startActivity(Intent(this@HomeActivity, ChannelActivity::class.java).apply {
                    putExtra(ChannelActivity.EXTRA_SESSION_ID, sessions.first().sessionId)
                })
            }
        } else {
            itemOverlay.setOnClickListener(null)
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
                .orderByValue()
                .equalTo(true)

        if (detach) {
            sessionQuery.removeEventListener(sessionSubscriptionListener)
            userSubscriptionQuery.removeEventListener(sessionSubscriptionListener)

            adapter.subscribedSessions.plus(adapter.nearbySessions).forEach {
                FirebaseService.getReference(References.Sessions)
                        .child(it.sessionId)
                        .removeEventListener(subscribedSessionListener)
            }

            if (isGeoQueryAttached) {
                geoQuery?.removeGeoQueryEventListener(geoQueryListener)
                isGeoQueryAttached = false
            }
        } else {
            sessionQuery.addChildEventListener(sessionSubscriptionListener)
            userSubscriptionQuery.addChildEventListener(sessionSubscriptionListener)

            geoQuery = FirebaseService.geoQueryForSessions(geoQueryListener, adapter.location?.latitude ?: 0.0, adapter.location?.longitude ?: 0.0, MAP_LOCATION_RADIUS_KM)
            isGeoQueryAttached = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_settings -> return true
            R.id.action_logout -> return consume { logout() }
            R.id.action_search -> return consume { showFiltersDialog() }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun showFiltersDialog() {
        progressDialog.setMessage(getString(R.string.loading))
        progressDialog.show()
        FirebaseService.getReference(References.tags).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
                progressDialog.dismiss()
            }

            override fun onDataChange(p0: DataSnapshot?) {
                progressDialog.dismiss()
                val tags = p0?.children?.map { it.key }?.toTypedArray()
                val selectedTags = tags?.map { Pair(it, searchFilters[it] ?: false) }.orEmpty().toMap(mutableMapOf())
                val checkedIndexes = selectedTags.values.toBooleanArray()
                AlertDialog.Builder(this@HomeActivity)
                        .setMultiChoiceItems(tags, checkedIndexes, { dialogInterface, i, b ->
                            selectedTags[tags?.get(i).orEmpty()] = b
                        })
                        .setPositiveButton(getString(R.string.save), { dialogInterface, i ->
                            searchFilters = selectedTags
                            adapter.filter = searchFilters.filter { it.value }.map { it.key }
                            refreshMapMarkers()
                        })
                        .setNegativeButton(getString(R.string.clear), { dialogInterface, i ->
                            searchFilters.clear()
                            adapter.filter = emptyList()
                            refreshMapMarkers()
                        })
                        .show()
            }
        })
    }

    fun refreshMapMarkers() {
        googleMap?.let { map ->
            map.clear()
            mapMarkers.forEach { marker ->
                if (adapter.subscribedSessions.any { it.sessionId == marker.key } || adapter.nearbySessions.any { it.sessionId == marker.key })
                    addMapMarker(marker.key, marker.value.position)
            }
        }
    }
}
