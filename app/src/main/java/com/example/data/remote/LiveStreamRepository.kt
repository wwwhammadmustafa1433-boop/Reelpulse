package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val TAG = "LiveStreamRepository"

class LiveStreamRepository(
    context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    )

    private val streamsCollection get() = firestore.collection("live_streams")

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Please sign in to access Live.")

    fun observePublicLiveStreams(): Flow<List<LiveStreamSession>> = callbackFlow {
        if (auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val registration = streamsCollection
            .whereEqualTo("visibility", "public")
            .whereEqualTo("status", "live")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = parseStreams(snapshot)
                trySend(items)
            }
        awaitClose { registration.remove() }
    }.catch { err ->
        if (err is CancellationException) throw err
        val fsErr = generateSequence(err) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        Log.e(TAG, "Failed observing public live streams", fsErr ?: err)
        throw (fsErr ?: err)
    }

    fun observeMyLiveStreams(): Flow<List<LiveStreamSession>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val registration = streamsCollection
            .whereEqualTo("hostUserId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = parseStreams(snapshot).filter { it.status == "live" }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }.catch { err ->
        if (err is CancellationException) throw err
        val fsErr = generateSequence(err) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        Log.e(TAG, "Failed observing personal live streams", fsErr ?: err)
        throw (fsErr ?: err)
    }

    fun observeStream(streamId: String): Flow<LiveStreamSession?> = callbackFlow {
        val registration = streamsCollection
            .document(streamId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val session = snapshot?.let { parseSingleStream(it) }
                trySend(session)
            }
        awaitClose { registration.remove() }
    }.catch { err ->
        if (err is CancellationException) throw err
        val fsErr = generateSequence(err) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        Log.e(TAG, "Failed observing live stream $streamId", fsErr ?: err)
        throw (fsErr ?: err)
    }

    fun observeStreamChat(
        streamId: String,
        streamHostId: String,
        streamVisibility: String
    ): Flow<List<LiveChatMessage>> = callbackFlow {
        val chatRef = streamsCollection.document(streamId).collection("chat")
        val query = if (streamVisibility == "public") {
            chatRef.whereEqualTo("streamVisibility", "public")
        } else {
            chatRef.whereEqualTo("streamHostId", streamHostId)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val messages = snapshot?.documents?.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                LiveChatMessage(
                    id = doc.id,
                    streamId = data["streamId"] as? String ?: streamId,
                    streamHostId = data["streamHostId"] as? String ?: streamHostId,
                    streamVisibility = data["streamVisibility"] as? String ?: streamVisibility,
                    senderId = data["senderId"] as? String ?: "",
                    senderName = data["senderName"] as? String ?: "Viewer",
                    senderHandle = data["senderHandle"] as? String ?: "@viewer",
                    text = data["text"] as? String ?: "",
                    createdAt = doc.getTimestamp(
                        "createdAt",
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                )
            }?.sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE } ?: emptyList()

            trySend(messages)
        }
        awaitClose { registration.remove() }
    }.catch { err ->
        if (err is CancellationException) throw err
        val fsErr = generateSequence(err) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        Log.e(TAG, "Failed observing live chat for $streamId", fsErr ?: err)
        throw (fsErr ?: err)
    }

    suspend fun startLiveStream(
        hostName: String,
        hostHandle: String,
        title: String,
        category: String,
        filterId: String,
        visibility: AccountVisibility
    ): Result<LiveStreamSession> {
        return try {
            val uid = requireUid()
            val cleanTitle = title.trim().ifBlank { "Live Studio Session" }.take(100)
            val cleanCategory = category.trim().ifBlank { "Music" }.take(32)
            val cleanFilter = filterId.trim().ifBlank { "normal" }.take(32)
            val cleanHostName = hostName.trim().ifBlank { "ReelPulse Creator" }.take(50)
            val cleanHandle = hostHandle.trim().ifBlank { "@creator" }.take(32)
            val streamId = "live_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

            val session = LiveStreamSession(
                id = streamId,
                hostUserId = uid,
                hostName = cleanHostName,
                hostHandle = cleanHandle,
                title = cleanTitle,
                category = cleanCategory,
                filterId = cleanFilter,
                status = "live",
                visibility = visibility.wireValue,
                viewerCount = 1,
                likeCount = 0
            )

            streamsCollection
                .document(streamId)
                .set(session.toCreateMap())
                .await()

            Result.success(session)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start live stream", e)
            Result.failure(e)
        }
    }

    suspend fun updateStreamFilter(streamId: String, filterId: String): Result<Unit> {
        return try {
            requireUid()
            streamsCollection
                .document(streamId)
                .update(
                    mapOf(
                        "filterId" to filterId.take(32),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed updating stream filter", e)
            Result.failure(e)
        }
    }

    suspend fun endLiveStream(streamId: String): Result<Unit> {
        return try {
            requireUid()
            streamsCollection
                .document(streamId)
                .update(
                    mapOf(
                        "status" to "ended",
                        "endedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed ending live stream $streamId", e)
            Result.failure(e)
        }
    }

    suspend fun sendHeartReaction(streamId: String): Result<Unit> {
        return try {
            requireUid()
            streamsCollection
                .document(streamId)
                .update(
                    mapOf(
                        "likeCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending live heart reaction", e)
            Result.failure(e)
        }
    }

    suspend fun adjustViewerCount(streamId: String, delta: Int): Result<Unit> {
        return try {
            requireUid()
            val docRef = streamsCollection.document(streamId)
            firestore.runTransaction { tx ->
                val snapshot = tx.get(docRef)
                if (!snapshot.exists()) return@runTransaction null
                if (snapshot.getString("status") != "live") return@runTransaction null
                val current = (snapshot.get("viewerCount") as? Number)?.toInt() ?: 1
                val updated = (current + delta).coerceAtLeast(0)
                tx.update(
                    docRef,
                    mapOf(
                        "viewerCount" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed adjusting viewer count", e)
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(
        stream: LiveStreamSession,
        senderName: String,
        senderHandle: String,
        rawText: String
    ): Result<Unit> {
        val text = rawText.trim()
        if (text.isEmpty()) return Result.success(Unit)
        if (text.length > 250) {
            return Result.failure(IllegalArgumentException("Message must be 250 characters or fewer."))
        }
        return try {
            val uid = requireUid()
            val msgId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val message = LiveChatMessage(
                id = msgId,
                streamId = stream.id,
                streamHostId = stream.hostUserId,
                streamVisibility = stream.visibility,
                senderId = uid,
                senderName = senderName.trim().ifBlank { "Viewer" }.take(50),
                senderHandle = senderHandle.trim().ifBlank { "@viewer" }.take(32),
                text = text
            )
            streamsCollection
                .document(stream.id)
                .collection("chat")
                .document(msgId)
                .set(message.toCreateMap())
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending live chat message", e)
            Result.failure(e)
        }
    }

    private fun parseStreams(snapshot: QuerySnapshot?): List<LiveStreamSession> {
        return snapshot?.documents
            ?.mapNotNull { parseSingleStream(it) }
            ?.sortedByDescending { it.startedAt?.toDate()?.time ?: 0L }
            ?: emptyList()
    }

    private fun parseSingleStream(doc: DocumentSnapshot): LiveStreamSession? {
        val data = doc.data ?: return null
        return LiveStreamSession(
            id = doc.id,
            hostUserId = data["hostUserId"] as? String ?: "",
            hostName = data["hostName"] as? String ?: "Creator",
            hostHandle = data["hostHandle"] as? String ?: "@creator",
            title = data["title"] as? String ?: "Live Stream",
            category = data["category"] as? String ?: "Music",
            filterId = data["filterId"] as? String ?: "normal",
            status = data["status"] as? String ?: "live",
            visibility = data["visibility"] as? String ?: "public",
            viewerCount = (data["viewerCount"] as? Number)?.toInt() ?: 1,
            likeCount = (data["likeCount"] as? Number)?.toInt() ?: 0,
            startedAt = doc.getTimestamp(
                "startedAt",
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ),
            updatedAt = doc.getTimestamp(
                "updatedAt",
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ),
            endedAt = doc.getTimestamp(
                "endedAt",
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        )
    }
}
