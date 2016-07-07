package com.sampsonjoliver.firestarter.views.chat

import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Message
import com.sampsonjoliver.firestarter.utils.appear
import com.sampsonjoliver.firestarter.utils.inflate
import kotlinx.android.synthetic.main.row_chat.view.*

class MessageRecyclerAdapter(val currentUserId: String) : RecyclerView.Adapter<MessageRecyclerAdapter.ChatHolder>() {
    companion object {
        const val USER_CHAT_COLOUR = R.color.mariner
        const val OTHER_CHAT_COLOUR = R.color.malachite
    }

    val messages: MutableList<Message> = mutableListOf()

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ChatHolder?, position: Int) {
        holder?.bind(messages[position],
                isCurrentUser = messages[position].userId == currentUserId,
                alignRight = messages[position].userId == currentUserId,
                hasPrevious = messages.getOrNull(position+1)?.userId == messages[position].userId,
                hasNext = messages.getOrNull(position-1)?.userId == messages[position].userId)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ChatHolder = ChatHolder(parent?.inflate(R.layout.row_chat, false))

    class ChatHolder(itemview: View?) : RecyclerView.ViewHolder(itemview) {
        fun bind(message: Message, isCurrentUser: Boolean, alignRight: Boolean, hasPrevious: Boolean, hasNext: Boolean) {
            itemView.image.appear = !hasPrevious
            itemView.time.appear = !hasNext
            itemView.image.setImageURI(message.userImageUrl)

            itemView.time.text = DateUtils.getRelativeDateTimeString(itemView.context, message.getTimestampLong(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL)

            itemView.message.text = message.message
            (itemView as ViewGroup?)?.layoutDirection = if (alignRight) LinearLayout.LAYOUT_DIRECTION_RTL else LinearLayout.LAYOUT_DIRECTION_LTR

            ViewCompat.setBackgroundTintList(itemView.message, itemView.message.resources.getColorStateList(
                    if (isCurrentUser) USER_CHAT_COLOUR else OTHER_CHAT_COLOUR))
        }
    }
}