package com.sampsonjoliver.firestarter.views.chat

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Message
import com.sampsonjoliver.firestarter.service.FirebaseService
import com.sampsonjoliver.firestarter.service.References
import com.sampsonjoliver.firestarter.service.SessionManager
import com.sampsonjoliver.firestarter.utils.TAG
import com.sampsonjoliver.firestarter.utils.copyToClipboard
import kotlinx.android.synthetic.main.activity_chat.*

class ChatActivity : AppCompatActivity(),
    MessageRecyclerAdapter.ChatListener,
    ChildEventListener {
    companion object {
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
    }

    val sessionId: String? by lazy { intent.getStringExtra(EXTRA_SESSION_ID) }
    val adapter by lazy { MessageRecyclerAdapter(SessionManager.getUid(), this) }

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
        // op with our current data model
    }

    override fun onCancelled(p0: DatabaseError?) {
        Log.w(this.TAG, "onCancelled", p0?.toException());
        Toast.makeText(this@ChatActivity, "Failed to load chat.",
                Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_chat)

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
                    Log.w(this@ChatActivity.TAG, "onPushMessage: error=" + databaseError?.message)
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
    }

    override fun onDestroy() {
        super.onDestroy()

        attachDataListener(true)

    }
}