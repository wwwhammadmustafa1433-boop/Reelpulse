package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val blockedCreatorId: String = "",
    val blockedCreatorName: String = "",
    val blockedCreatorHandle: String = "",
    val blockedCreatorAvatarKey: String = "tech",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "blockedCreatorId" to blockedCreatorId,
        "blockedCreatorName" to blockedCreatorName,
        "blockedCreatorHandle" to blockedCreatorHandle,
        "blockedCreatorAvatarKey" to blockedCreatorAvatarKey,
        "createdAt" to FieldValue.serverTimestamp()
    )
}
