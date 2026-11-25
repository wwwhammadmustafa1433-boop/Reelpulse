package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BlockedUserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to manage blocked users.")
    }

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user blocked list read." }
        val collectionPath = "users/$uid/blocked_users"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("blocked_users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            BlockedUser::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, collectionPath)
                    }
                    throw error
                }
        )
    }

    suspend fun blockUser(
        userId: String,
        creatorId: String,
        creatorName: String,
        creatorHandle: String,
        creatorAvatarKey: String
    ): Result<Unit> {
        val safeCreatorId = creatorId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        val docPath = "users/$userId/blocked_users/$safeCreatorId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user block write." }
            require(safeCreatorId.isNotEmpty()) { "Invalid user identifier." }
            require(safeCreatorId != uid && safeCreatorId != "local_user") {
                "You cannot block your own account."
            }

            val cleanName = creatorName.trim().ifEmpty { "ReelPulse Creator" }.take(50)
            val cleanHandle = creatorHandle.trim().let {
                if (it.startsWith("@")) it else "@$it"
            }.ifEmpty { "@creator" }.take(32)
            val cleanAvatar = creatorAvatarKey.trim().ifEmpty { "tech" }.take(32)

            val payload = BlockedUser(
                userId = uid,
                blockedCreatorId = safeCreatorId,
                blockedCreatorName = cleanName,
                blockedCreatorHandle = cleanHandle,
                blockedCreatorAvatarKey = cleanAvatar
            )

            db.collection("users")
                .document(uid)
                .collection("blocked_users")
                .document(safeCreatorId)
                .set(payload.toCreateMap())
                .await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(
        userId: String,
        blockedCreatorId: String
    ): Result<Unit> {
        val safeCreatorId = blockedCreatorId.trim().take(128)
        val docPath = "users/$userId/blocked_users/$safeCreatorId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user unblock." }
            require(safeCreatorId.isNotEmpty()) { "Invalid blocked user identifier." }

            db.collection("users")
                .document(uid)
                .collection("blocked_users")
                .document(safeCreatorId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
