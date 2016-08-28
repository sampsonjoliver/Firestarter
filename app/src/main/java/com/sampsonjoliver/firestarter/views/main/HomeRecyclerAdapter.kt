package com.sampsonjoliver.firestarter.views.main

import android.location.Location
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.model.LatLng
import com.sampsonjoliver.firestarter.R
import com.sampsonjoliver.firestarter.models.Session
import com.sampsonjoliver.firestarter.utils.DistanceUtils
import com.sampsonjoliver.firestarter.utils.appear
import com.sampsonjoliver.firestarter.utils.inflate
import com.sampsonjoliver.firestarter.views.RecyclerEmptyViewHolder
import com.sampsonjoliver.firestarter.views.RecyclerHeaderViewHolder
import kotlinx.android.synthetic.main.row_session.view.*
import java.util.*

class HomeRecyclerAdapter(val listener: OnSessionClickedListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        val TYPE_SUBSCRIBED_HEADER = 0
        val TYPE_NEARBY_HEADER = 1
        val TYPE_SUBSCRIBED_SESSION = 2
        val TYPE_NEARBY_SESSION = 3
        val TYPE_EMPTY = 4
    }

    var location: Location? = null
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    interface OnSessionClickedListener {
        fun onSessionClicked(session: Session)
    }

    val subscribedSessions: MutableList<Session> = mutableListOf()
    val nearbySessions: MutableList<Session> = mutableListOf()

    fun hasSubscriptions() = subscribedSessions.size > 0
    fun hasNearbySessions() = nearbySessions.size > 0

    fun nearbySessionIndexToAdapterIndex(dataIndex: Int): Int {
        return (if (hasSubscriptions()) 1 + subscribedSessions.size else 0) + 1 + dataIndex
    }

    fun subscribedSessionIndexToAdapterIndex(dataIndex: Int): Int {
        return 1 + dataIndex
    }

    fun getSubscribedHeaderIndex() = 0
    fun getNearbyHeaderIndex() = if (hasSubscriptions()) 1 + subscribedSessions.size else 0

    fun getSession(position: Int): Session {
        if (getItemViewType(position) != TYPE_SUBSCRIBED_SESSION && getItemViewType(position) != TYPE_NEARBY_SESSION)
            throw IndexOutOfBoundsException()

        if (hasSubscriptions() && position < subscribedSessions.size + 1)
            return subscribedSessions[position - 1]
        else if (hasNearbySessions() && position < nearbySessions.size + 1 + (if (hasSubscriptions()) subscribedSessions.size + 1 else 0))
            return nearbySessions[position - 1 - (if (hasSubscriptions()) subscribedSessions.size + 1 else 0)]
        else
            throw IndexOutOfBoundsException()
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            if (hasSubscriptions())
                return TYPE_SUBSCRIBED_HEADER
            else if (hasNearbySessions())
                return TYPE_NEARBY_HEADER
            else
                return TYPE_EMPTY
        } else {
            if (hasSubscriptions() && position < subscribedSessions.size + 1)
                return TYPE_SUBSCRIBED_SESSION
            else if (hasSubscriptions() && position == subscribedSessions.size + 1)
                return TYPE_NEARBY_HEADER
            else if (hasNearbySessions() && position < nearbySessions.size + 1 + if (hasSubscriptions()) subscribedSessions.size + 1 else 0)
                return TYPE_NEARBY_SESSION
        }

        return TYPE_EMPTY
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        if (holder is SessionViewHolder)
            holder.bindView(getSession(position), listener)
        else if (holder is RecyclerHeaderViewHolder) {
            if (getItemViewType(position) == TYPE_NEARBY_HEADER)
                holder.bind(holder.itemView.context.resources.getString(R.string.home_nearby_sessions))
            else if (getItemViewType(position) == TYPE_SUBSCRIBED_HEADER)
                holder.bind(holder.itemView.context.resources.getString(R.string.home_my_sessions))
        }

        else if (holder is RecyclerEmptyViewHolder)
            holder.bind(holder.itemView.context.resources.getString(R.string.home_no_sessions))
    }

    override fun getItemCount(): Int = (subscribedSessions.size + if (subscribedSessions.size > 0) 1 else 0) +
            (nearbySessions.size + if (nearbySessions.size > 0) 1 else 0)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder? {
        return when (viewType) {
            TYPE_SUBSCRIBED_HEADER -> RecyclerHeaderViewHolder(RecyclerHeaderViewHolder.inflate(parent))
            TYPE_NEARBY_HEADER -> RecyclerHeaderViewHolder(RecyclerHeaderViewHolder.inflate(parent))
            TYPE_SUBSCRIBED_SESSION -> SessionViewHolder(parent?.inflate(R.layout.row_session, false))
            TYPE_NEARBY_SESSION -> SessionViewHolder(parent?.inflate(R.layout.row_session, false))
            TYPE_EMPTY -> RecyclerEmptyViewHolder(RecyclerEmptyViewHolder.inflate(parent))
            else -> null
        }
    }

    inner class SessionViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
        fun bindView(session: Session, listener: OnSessionClickedListener) {
            itemView.title.text = session.topic
            itemView.subtitle.text = session.username
            itemView.image.setImageURI(session.bannerUrl)

            itemView.distance.appear = location != null
            if (location != null) {
                itemView.distance.text = DistanceUtils.formatDistance(LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0), session.getLocation())
            }

            itemView.time.text = DateUtils.getRelativeTimeSpanString(
                    session.startDateAsDate.time ?: Date().time,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE)

            itemView.setOnClickListener { listener.onSessionClicked(session) }
        }
    }
}