package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class VideoMetadata(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val caption: String = "",
    val category: String = "Tech",
    val tags: List<String> = emptyList(),
    val soundTitle: String = "",
    val videoUri: String = "",
    val filterId: String = "normal",
    val filterName: String = "Normal",
    val sourceType: String = "camera",
    val durationMs: Long = 15_000L,
    val creatorName: String = "",
    val creatorHandle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "title" to title,
        "caption" to caption,
        "category" to category,
        "tags" to tags,
        "soundTitle" to soundTitle,
        "videoUri" to videoUri,
        "filterId" to filterId,
        "filterName" to filterName,
        "sourceType" to sourceType,
        "durationMs" to durationMs,
        "creatorName" to creatorName,
        "creatorHandle" to creatorHandle,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
