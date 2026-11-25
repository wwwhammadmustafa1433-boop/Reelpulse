package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AccountVisibility(val wireValue: String) {
    PERSONAL("personal"),
    PUBLIC("public");

    companion object {
        fun fromWire(raw: String?): AccountVisibility {
            return entries.firstOrNull {
                it.wireValue.equals(raw?.trim(), ignoreCase = true)
            } ?: PUBLIC
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val email: String? = null,
    val mobileNumber: String? = null,
    val bio: String? = null,
    val accountVisibility: String = AccountVisibility.PUBLIC.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedVisibility(): AccountVisibility = AccountVisibility.fromWire(accountVisibility)
}
