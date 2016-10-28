package com.sampsonjoliver.firestarter.views.channel

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.sampsonjoliver.firestarter.LocationAwareActivity
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Message
import com.sampsonjoliver.firestarter.models.Session
import com.sampsonjoliver.firestarter.service.FirebaseService
import com.sampsonjoliver.firestarter.service.References
import com.sampsonjoliver.firestarter.service.SessionManager
import com.sampsonjoliver.firestarter.utils.*
import kotlinx.android.synthetic.main.activity_channel.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ChannelActivity : LocationAwareActivity(),
        ChannelMessageRecyclerAdapter.ChatListener,
        ChildEventListener {

    companion object {
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
    }

    var session: Session? = null
    var location: Location? = null
    val sessionId: String? by lazy { intent.getStringExtra(EXTRA_SESSION_ID) }
    var isSessionOwner: Boolean = false
    val adapter by lazy { ChannelMessageRecyclerAdapter(SessionManager.getUid(), this) }

    var currentPhotoPath: String? = null

    val sessionSubscriberListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError?) {
            Log.w(this@ChannelActivity.TAG, "onCancelled")
        }

        override fun onDataChange(p0: DataSnapshot?) {
            val numSubscribers = p0?.getValue(Int::class.java)
            users.text = numSubscribers.toString()
        }
    }

    val userSubscriptionListener = object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError?) {
            Log.w("${this@ChannelActivity.TAG}:${this.TAG}", "onCancelled")
        }

        override fun onDataChange(p0: DataSnapshot?) {
            Log.w("${this@ChannelActivity.TAG}:${this.TAG}", "onDataChange: ${p0?.key}")

            setUserSubscriptionState(p0?.getValue(Boolean::class.java) ?: false)
        }
    }

    fun setUserSubscriptionState(isUserSubscribed: Boolean) {
        joinGroup.appear = !isUserSubscribed
        bottomView.isEnabled = isUserSubscribed
        messageText.isEnabled = isUserSubscribed
        sendButton.isEnabled = isUserSubscribed
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
            isSessionOwner = session?.userId == SessionManager.getUid()
            supportInvalidateOptionsMenu()

            banner.setImageURI(session?.bannerUrl)
            collapsingToolbar.title = session?.topic
            title = session?.topic
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

    override fun onChildAdded(dataSnapshot: DataSnapshot?, previousChildName: String?) {
        Log.w(this.TAG, "onChildAdded: ${dataSnapshot?.key}")
        dataSnapshot?.getValue(Message::class.java)?.let {
            adapter.addMessage(it)
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
        menu?.findItem(R.id.menu_leave)?.isVisible = isSessionOwner.not()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_leave)?.isVisible = isSessionOwner.not()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_leave -> {
                sessionId?.let { FirebaseService.updateSessionSubscription(it, true, { finish() }) }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_channel)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setOnClickListener { appBarLayout.setExpanded(true, true) }
        messageText.setOnClickListener { appBarLayout.setExpanded(false, true) }
        setUserSubscriptionState(false)

        distance.setOnClickListener {
            if (session?.getLocation() != null)
                IntentUtils.launchMaps(this@ChannelActivity, session?.getLocation()!!, session?.topic)
        }

        sessionId?.let { sessionId ->
            joinBtn.setOnClickListener { FirebaseService.updateSessionSubscription(sessionId, false) }
        }

        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        recycler.adapter = adapter

        attachDataListener()

        sendButton.setOnClickListener { sendNewMessage(messageText) }
        photoButton.setOnClickListener {  }

        messageText.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendNewMessage(messageText)
                return@OnEditorActionListener true
            }
            false
        })

        photoButton.setOnClickListener {
            addPhoto()
        }
    }

    fun sendNewMessage(messageWidget: EditText) {
        if (messageWidget.text.isNullOrBlank().not()) {
            // Upload the message to firebase and clear the message textbox
            sendNewMessage(messageWidget.text.toString(), SessionManager.getUid())
            messageWidget.setText("")
        }
    }

    fun sendNewMessage(messageText: String, userId: String, contentUri: String? = null) {
        val message = Message(userId, SessionManager.getUserPhotoUrl(), sessionId ?: "", messageText, contentUri)

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

        FirebaseService.getReference(References.UserSubscriptions)
                .child(SessionManager.getUid())
                .child(sessionId)
                .run {
                    when (detach) {
                        true -> this.removeEventListener(userSubscriptionListener)
                        false -> this.addValueEventListener(userSubscriptionListener)
                    }
                }
    }

    override fun onDestroy() {
        super.onDestroy()

        attachDataListener(true)
    }

    fun addPhoto() {
        dispatchTakePictureIntent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            IntentUtils.REQUEST_IMAGE_CAPTURE -> {
                data?.data?.run {
                    FirebaseStorage.getInstance().getReference("${References.Images}/${this.lastPathSegment}")
                            .putFile(this, StorageMetadata.Builder()
                                    .setContentType("image/jpg")
                                    .setCustomMetadata("uid", SessionManager.getUid())
                                    .setCustomMetadata("sessionId", sessionId)
                                    .build()
                            ).addOnSuccessListener { sendNewMessage("", SessionManager.getUid(), it.downloadUrl.toString()) }
                            .addOnFailureListener {  }
                            .addOnProgressListener {  } // todo
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            val photoFile: File
            try {
                photoFile = createImageFile()

                // Continue only if the File was successfully created
                val photoURI = FileProvider.getUriForFile(this, "com.sampsonjoliver.firestarter.fileprovider", photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, IntentUtils.REQUEST_IMAGE_CAPTURE)
            } catch (ex: IOException) {
                // Error occurred while creating the File
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */)

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = "file:" + image.absolutePath
        return image
    }
}