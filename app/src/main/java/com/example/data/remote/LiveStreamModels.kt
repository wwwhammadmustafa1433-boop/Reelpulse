package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class LiveStreamSession(
    @DocumentId
    val id: String = "",
    val hostUserId: String = "",
    val hostName: String = "",
    val hostHandle: String = "",
    val title: String = "",
    val category: String = "Music",
    val filterId: String = "normal",
    val status: String = "live",
    val visibility: String = "public",
    val viewerCount: Int = 1,
    val likeCount: Int = 0,
    val startedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val endedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "hostUserId" to hostUserId,
        "hostName" to hostName,
        "hostHandle" to hostHandle,
        "title" to title,
        "category" to category,
        "filterId" to filterId,
        "status" to "live",
        "visibility" to visibility,
        "viewerCount" to viewerCount.coerceAtLeast(0),
        "likeCount" to likeCount.coerceAtLeast(0),
        "startedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class LiveChatMessage(
    @DocumentId
    val id: String = "",
    val streamId: String = "",
    val streamHostId: String = "",
    val streamVisibility: String = "public",
    val senderId: String = "",
    val senderName: String = "",
    val senderHandle: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "streamId" to streamId,
        "streamHostId" to streamHostId,
        "streamVisibility" to streamVisibility,
        "senderId" to senderId,
        "senderName" to senderName,
        "senderHandle" to senderHandle,
        "text" to text,
        "createdAt" to FieldValue.serverTimestamp()
    )
}
