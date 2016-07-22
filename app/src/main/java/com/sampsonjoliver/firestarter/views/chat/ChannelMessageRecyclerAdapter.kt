package com.sampsonjoliver.firestarter.views.chat

import android.os.Handler
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Message
import com.sampsonjoliver.firestarter.utils.inflate
import com.sampsonjoliver.firestarter.utils.setBackgroundResourcePreservePadding
import kotlinx.android.synthetic.main.row_chat.view.*

class ChannelMessageRecyclerAdapter(val currentUserId: String, val listener: ChatListener) : RecyclerView.Adapter<ChannelMessageRecyclerAdapter.ChatHolder>() {
    interface ChatListener {
        fun onItemInsertedListener()
        fun onMessageLongPress(message: Message)
    }

    companion object {
        const val USER_CHAT_COLOUR = R.color.chat_selector_1
        const val OTHER_CHAT_COLOUR = R.color.chat_selector_2

        const val MESSAGE_GROUP_TIME_OFFSET_MS = 1000 * 60
        const val MAX_BLOCK_SIZE = 10
    }

    data class MessageBlock(
            val userId: String,
            val userImageUrl: String,
            var startTime: Long,
            var endTime: Long,
            val messages: MutableList<Message> = mutableListOf()
    ) {
        constructor(message: Message) : this(message.userId, message.userImageUrl, message.getTimestampLong(), message.getTimestampLong(), mutableListOf(message))

        fun pushMessage(message: Message): Boolean {
            if (messages.isEmpty()) {
                messages.add(message)
                startTime = message.getTimestampLong()
                endTime = message.getTimestampLong()
                return true
            } else if (messages.size < MAX_BLOCK_SIZE && message.userId == userId && message.getTimestampLong() <= endTime + MESSAGE_GROUP_TIME_OFFSET_MS && message.getTimestampLong() >= startTime) {
                messages.add(message)
                endTime = message.getTimestampLong()
                return true
            }
            return false
        }
    }

    val messages: MutableList<MessageBlock> = mutableListOf()

    fun addMessage(message: Message) {
        if (messages.isEmpty() || messages[0].pushMessage(message).not()) {
            messages.add(0, MessageBlock(message))
            notifyItemInserted(0)
            listener.onItemInsertedListener()
        } else {
            notifyItemChanged(0)
            listener.onItemInsertedListener()
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ChatHolder?, position: Int) {
        holder?.bind(messages[position],
                isCurrentUser = messages[position].userId == currentUserId,
                alignRight = messages[position].userId == currentUserId, listener = listener)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ChatHolder = ChatHolder(parent?.inflate(R.layout.row_chat, false))

    class ChatHolder(itemview: View?) : RecyclerView.ViewHolder(itemview) {
        val MESSAGE_ID = 0
        val UPDATE_INTERVAL_MS: Long = 1000 * 60

        private val handler = object : Handler() {
            fun start() {
                removeMessages(MESSAGE_ID)
                sendEmptyMessageDelayed(MESSAGE_ID, UPDATE_INTERVAL_MS)
            }

            override fun handleMessage(msg: android.os.Message?) {
                this@ChatHolder.itemView.time.text = DateUtils.getRelativeDateTimeString(itemView.context, messageGroup?.startTime ?: 0,
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL)

                removeMessages(MESSAGE_ID)
                this.sendEmptyMessageDelayed(MESSAGE_ID, UPDATE_INTERVAL_MS)
            }
        }
        var messageGroup: MessageBlock? = null

        fun bind(messageGroup: MessageBlock, isCurrentUser: Boolean, alignRight: Boolean, listener: ChatListener) {
            this.messageGroup = messageGroup
            handler.start()

            itemView.image.setImageURI(messageGroup.userImageUrl)

            itemView.time.text = DateUtils.getRelativeDateTimeString(itemView.context, messageGroup.startTime,
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL)

            (itemView as ViewGroup?)?.layoutDirection = if (alignRight) LinearLayout.LAYOUT_DIRECTION_RTL else LinearLayout.LAYOUT_DIRECTION_LTR

            itemView.messageGroup.removeAllViews()

            messageGroup.messages.forEachIndexed { i, it ->
                itemView.messageGroup.addView(inflateMessageView(it, isCurrentUser, alignRight, i != 0, i != messageGroup.messages.size - 1, itemView.messageGroup, listener))
            }
        }

        fun inflateMessageView(message: Message, isCurrentUser: Boolean, alignRight: Boolean, hasPrevious: Boolean, hasNext: Boolean, parent: ViewGroup, listener: ChatListener): View {
            val messageView = parent.inflate(R.layout.row_chat_message, false) as TextView
            messageView.text = message.message
            messageView.setBackgroundResourcePreservePadding(
                    if (hasPrevious && hasNext && alignRight) R.drawable.chat_bubble_right
                    else if (hasPrevious && hasNext && !alignRight) R.drawable.chat_bubble_left
                    else if (hasPrevious && !hasNext && alignRight) R.drawable.chat_bubble_topright
                    else if (hasPrevious && !hasNext && !alignRight) R.drawable.chat_bubble_topleft
                    else if (!hasPrevious && hasNext && alignRight) R.drawable.chat_bubble_bottomright
                    else if (!hasPrevious && hasNext && !alignRight) R.drawable.chat_bubble_bottomleft
                    else R.drawable.chat_bubble_single
            )
            ViewCompat.setBackgroundTintList(messageView, messageView.resources.getColorStateList(
                    if (isCurrentUser) USER_CHAT_COLOUR else OTHER_CHAT_COLOUR))

            messageView.setOnLongClickListener {
                listener.onMessageLongPress(message)
                true
            }

            return messageView
        }
    }
}