package com.sampsonjoliver.firestarter.models

import com.google.firebase.database.Exclude
import com.google.firebase.database.ServerValue

class Message(
        var userId: String,
        var userImageUrl: String,
        var sessionId: String,
        var message: String,
        var contentUri: String? = null
) {
    var messageId: String = ""
    private var timestamp: Long = 0

    constructor() : this("", "", "", "")

    @Exclude fun getTimestampLong() = timestamp
    @Exclude fun setTimestampLong(timestamp: Long) { this.timestamp = timestamp }
    fun getTimestamp() = ServerValue.TIMESTAMP
    fun setTimestamp(timestamp: Long) { this.timestamp = timestamp }
}