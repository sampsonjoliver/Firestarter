package com.sampsonjoliver.firestarter.models

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.database.Exclude
import com.google.firebase.database.ServerValue

class Message(
        var userId: String,
        var userImageUrl: String,
        var sessionId: String,
        var message: String,
        var contentUri: String? = null,
        var contentThumbUri: String? = null,
        var title: String? = null
) : Parcelable {
    companion object {
        @JvmField val CREATOR: Parcelable.Creator<Message> = object : Parcelable.Creator<Message> {
            override fun createFromParcel(source: Parcel): Message {
                return Message(source)
            }

            override fun newArray(size: Int): Array<Message> {
                return emptyArray()
            }
        }
    }

    var messageId: String = ""
    private var timestamp: Long = 0

    constructor() : this("", "", "", "")

    constructor(parcel: Parcel) : this(
            userId = parcel.readString(),
            userImageUrl = parcel.readString(),
            sessionId = parcel.readString(),
            message = parcel.readString(),
            contentUri = parcel.readString(),
            contentThumbUri = parcel.readString(),
            title = parcel.readString()
    )

    @Exclude fun getTimestampLong() = timestamp
    @Exclude fun setTimestampLong(timestamp: Long) { this.timestamp = timestamp }
    fun getTimestamp() = ServerValue.TIMESTAMP
    fun setTimestamp(timestamp: Long) { this.timestamp = timestamp }

    override fun writeToParcel(parcel: Parcel?, p1: Int) {
        parcel?.run {
            writeString(userId)
            writeString(userImageUrl)
            writeString(sessionId)
            writeString(message)
            writeString(contentUri)
            writeString(contentThumbUri)
            writeString(title)
        }
    }

    override fun describeContents(): Int {
        return 0
    }
}