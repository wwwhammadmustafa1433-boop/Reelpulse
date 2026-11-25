package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserAccountRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserAccount(userId: String): Flow<UserAccount?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user read." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserAccount::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureOrUpsertAccount(
        userId: String,
        displayName: String,
        handle: String,
        email: String?,
        mobileNumber: String?,
        bio: String?,
        overwriteExisting: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val safeName = displayName.trim().take(50).let {
                if (it.length >= 2) it else "ReelPulse Creator"
            }
            val rawHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }
            val safeHandle = rawHandle.take(32).let {
                if (it.length >= 2) it else "@creator"
            }
            val safeEmail = email?.trim()?.take(120)?.takeIf { it.isNotEmpty() }
            val safeMobile = mobileNumber?.trim()?.take(20)?.takeIf { it.length in 7..20 }
            val safeBio = bio?.trim()?.take(200)?.takeIf { it.isNotEmpty() }

            val docRef = db.collection("users").document(uid)
            val existingSnap = docRef.get().await()

            if (!existingSnap.exists()) {
                val payload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "handle" to safeHandle,
                    "email" to safeEmail,
                    "mobileNumber" to safeMobile,
                    "bio" to safeBio,
                    "accountVisibility" to AccountVisibility.PUBLIC.wireValue,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else if (overwriteExisting) {
                val updates = mutableMapOf<String, Any?>(
                    "displayName" to safeName,
                    "handle" to safeHandle,
                    "email" to safeEmail,
                    "mobileNumber" to safeMobile,
                    "bio" to safeBio,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateAccountProfile(
        userId: String,
        displayName: String,
        handle: String,
        mobileNumber: String?,
        bio: String?
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user update." }
            val safeName = displayName.trim().take(50)
            require(safeName.length >= 2) { "Name must be at least 2 characters." }
            val rawHandle = handle.trim().let { if (it.startsWith("@")) it else "@$it" }
            val safeHandle = rawHandle.take(32)
            require(safeHandle.length >= 2) { "Handle must be at least 2 characters." }

            val cleanedMobile = mobileNumber?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanedMobile != null) {
                require(cleanedMobile.length in 7..20) { "Mobile number must be 7 to 20 characters." }
            }
            val cleanedBio = bio?.trim()?.take(200)

            val docRef = db.collection("users").document(uid)
            val updates = mutableMapOf<String, Any?>(
                "displayName" to safeName,
                "handle" to safeHandle,
                "mobileNumber" to cleanedMobile,
                "bio" to cleanedBio,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateAccountVisibility(
        userId: String,
        visibility: AccountVisibility
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user visibility update." }

            val updates = mapOf(
                "accountVisibility" to visibility.wireValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
