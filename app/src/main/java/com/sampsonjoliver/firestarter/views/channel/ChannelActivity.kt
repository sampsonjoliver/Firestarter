package com.sampsonjoliver.firestarter.views.channel

import android.location.Location
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.text.format.DateUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.*
import com.sampsonjoliver.firestarter.LocationAwareActivity
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Message
import com.sampsonjoliver.firestarter.models.Session
import com.sampsonjoliver.firestarter.service.FirebaseService
import com.sampsonjoliver.firestarter.service.References
import com.sampsonjoliver.firestarter.service.SessionManager
import com.sampsonjoliver.firestarter.utils.DistanceUtils
import com.sampsonjoliver.firestarter.utils.IntentUtils
import com.sampsonjoliver.firestarter.utils.TAG
import com.sampsonjoliver.firestarter.utils.copyToClipboard
import kotlinx.android.synthetic.main.activity_channel.*

class ChannelActivity : LocationAwareActivity(),
        ChannelMessageRecyclerAdapter.ChatListener,
        ChildEventListener {

    companion object {
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
    }

    var session: Session? = null
    var location: Location? = null
    val sessionId: String? by lazy { intent.getStringExtra(EXTRA_SESSION_ID) }
    val adapter by lazy { ChannelMessageRecyclerAdapter(SessionManager.getUid(), this) }

    val sessionSubscriberListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError?) {
            Log.w(this@ChannelActivity.TAG, "onCancelled")
        }

        override fun onDataChange(p0: DataSnapshot?) {
            val numSubscribers = p0?.getValue(Int::class.java)
            users.text = numSubscribers.toString()
        }
    }

    val sessionListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError?) {
            Log.w(this@ChannelActivity.TAG, "onCancelled")
        }

        override fun onDataChange(p0: DataSnapshot?) {
            Log.w(this@ChannelActivity.TAG, "onDataChange: ${p0?.key}")
            val session = p0?.getValue(Session::class.java)
            session?.sessionId = p0?.key
            this@ChannelActivity.session = session

            banner.setImageURI(session?.bannerUrl)
            collapsingToolbar.title = session?.topic
            time.text = DateUtils.formatDateRange(this@ChannelActivity, session?.startDate ?: 0, session?.startDate?.plus(session.durationMs) ?: 0, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_TIME)

            onLocationChanged(this@ChannelActivity.location)
        }
    }

    override fun onLocationChanged(location: Location?) {
        this@ChannelActivity.location = location

        val geoDistance = DistanceUtils.latLngDistance(session?.getLocation() ?: LatLng(0.0, 0.0), LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0))
        distance.text = DistanceUtils.formatDistance(geoDistance[0])
    }

    override fun onConnected(connectionHint: Bundle?) {
        super.onConnected(connectionHint)
        startLocationUpdatesWithChecks()
    }

    override fun onItemInsertedListener() {
        recycler.smoothScrollToPosition(0)
    }

    override fun onMessageLongPress(message: Message) {
        Snackbar.make(messageText, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show()
        this.copyToClipboard(message.messageId, message.message)
    }

    override fun onChildMoved(p0: DataSnapshot?, previousChildName: String?) = Unit

    override fun onChildChanged(p0: DataSnapshot?, previousChildName: String?) {
        Log.w(this.TAG, "onChildChanged: ${p0?.key}")

        val key = p0?.key ?: ""
        val message = p0?.getValue(Message::class.java)

        // todo need to update message inside of its message group; this is likely to be an expensive
        // op with our current data model
    }

    override fun onChildAdded(p0: DataSnapshot?, previousChildName: String?) {
        Log.w(this.TAG, "onChildAdded: ${p0?.key}")

        val key = p0?.key ?: ""
        val message = p0?.getValue(Message::class.java)

        message?.let {
            adapter.addMessage(message)
        }
    }

    override fun onChildRemoved(p0: DataSnapshot?) {
        Log.w(this.TAG, "onChildRemoved: ${p0?.key}")
        val key = p0?.key ?: ""

        // todo need to update message inside of its message group; this is likely to be an expensive
        // op with our current local data model
    }

    override fun onCancelled(p0: DatabaseError?) {
        Log.w(this.TAG, "onCancelled", p0?.toException());
        Toast.makeText(this@ChannelActivity, "Failed to load chat.",
                Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_channel, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_leave -> {
                sessionId?.let { leaveSession(it) }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun leaveSession(sessionId: String) {
        FirebaseService.getReference(References.SessionSubscriptions)
                .child(sessionId)
                .runTransaction(object : Transaction.Handler {
            override fun onComplete(databaseError: DatabaseError?, b: Boolean, dataSnapshot: DataSnapshot?) {
                Log.d(TAG, "postTransaction:onComplete:" + databaseError)
            }

            override fun doTransaction(mutableData: MutableData?): Transaction.Result {
                val obj = mutableData?.getValue(MutableMap::class.java) as? MutableMap<String, Any?>

                obj?.put("numUsers", (obj.get("numUsers") as? Int)?.dec())
                obj?.put(SessionManager.getUid(), false)

                mutableData?.value = obj
                return Transaction.success(mutableData)
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_channel)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        collapsingToolbar.isTitleEnabled = true
        toolbar.setOnClickListener { appBarLayout.setExpanded(true, true) }
        messageText.setOnClickListener { appBarLayout.setExpanded(false, true) }

        distance.setOnClickListener {
            if (session?.getLocation() != null)
                IntentUtils.launchMaps(this@ChannelActivity, session?.getLocation()!!, session?.topic)
        }

        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        recycler.adapter = adapter

        attachDataListener()

        sendButton.setOnClickListener { sendNewMessage(messageText) }

        messageText.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendNewMessage(messageText)
                return@OnEditorActionListener true
            }
            false
        })
    }

    fun sendNewMessage(messageWidget: EditText) {
        if (messageWidget.text.isNullOrBlank().not()) {
            // Upload the message to firebase and clear the message textbox
            sendNewMessage(messageWidget.text.toString(), SessionManager.getUid())
            messageWidget.setText("")
        }
    }

    fun sendNewMessage(messageText: String, userId: String) {
        val message = Message(userId, SessionManager.getUserPhotoUrl(), sessionId ?: "", messageText)

        FirebaseService.getReference(References.Messages)
                .child(sessionId)
                .push()
                .setValue(message, DatabaseReference.CompletionListener { databaseError, databaseReference ->
                    Log.w(this@ChannelActivity.TAG, "onPushMessage: error=" + databaseError?.message)
                })
    }

    fun attachDataListener(detach: Boolean = false) {
        val ref = FirebaseService.getReference(References.Messages)
                .child(sessionId)
                .orderByChild("timestamp")
                .limitToLast(100)

        if (detach)
            ref.removeEventListener(this)
        else
            ref.addChildEventListener(this)

        FirebaseService.getReference(References.Sessions).child(sessionId).run {
            when (detach) {
                true -> this.removeEventListener(sessionListener)
                else -> this.addValueEventListener(sessionListener)
            }
        }

        FirebaseService.getReference(References.SessionSubscriptions)
                .child(sessionId).child("numUsers")
                .run {
                    when (detach) {
                        true -> this.removeEventListener(sessionSubscriberListener)
                        else -> this.addValueEventListener(sessionSubscriberListener)
                    }
                }
    }

    override fun onDestroy() {
        super.onDestroy()

        attachDataListener(true)
    }
}