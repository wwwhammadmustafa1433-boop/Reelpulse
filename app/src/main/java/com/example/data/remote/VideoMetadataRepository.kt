package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.camera.CameraFilterPreset
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
import kotlin.random.Random

class VideoMetadataRepository(
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
            ?: throw IllegalStateException("User must be signed in before posting video metadata.")
    }

    fun observeUserVideos(userId: String): Flow<List<VideoMetadata>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed; refusing cross-user video read." }
        val collectionPath = "users/$uid/videos"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("videos")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            VideoMetadata::class.java,
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

    suspend fun fetchUserVideosOnce(userId: String): List<VideoMetadata> {
        val collectionPath = "users/$userId/videos"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user video read." }
            val snapshot = db.collection("users")
                .document(uid)
                .collection("videos")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(
                    VideoMetadata::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, collectionPath)
            emptyList()
        }
    }

    suspend fun saveVideoMetadata(
        userId: String,
        title: String,
        caption: String,
        category: String,
        tagsCsv: String,
        soundTitle: String,
        videoUri: String,
        filterPreset: CameraFilterPreset,
        sourceType: String,
        durationMs: Long,
        creatorName: String,
        creatorHandle: String
    ): Result<VideoMetadata> {
        val now = System.currentTimeMillis()
        val videoId = "vid_${now}_${Random.nextInt(1000, 9999)}"
        val docPath = "users/$userId/videos/$videoId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user video write." }

            val rawTitle = title.trim().take(100)
            val rawCaption = caption.trim().take(500)
            require(rawTitle.isNotEmpty() || rawCaption.isNotEmpty()) {
                "Please enter a title before uploading."
            }

            val cleanTitle = rawTitle.ifEmpty {
                rawCaption.lineSequence().firstOrNull()?.take(48)?.trim().orEmpty()
                    .ifEmpty { "ReelPulse Short" }
            }.take(100)
            val cleanCaption = rawCaption.ifEmpty { cleanTitle }.take(500)

            val cleanCategory = category.trim().ifEmpty { "Tech" }.take(32)
            val cleanTags = tagsCsv.split(",", " ")
                .map { it.trim().replace(" ", "") }
                .filter { it.isNotEmpty() && it != "#" }
                .map { (if (it.startsWith("#")) it else "#$it").take(32) }
                .distinctBy { it.lowercase() }
                .take(10)
            val cleanSound = soundTitle.trim().ifEmpty { "Original Sound" }.take(100)
            val cleanUri = videoUri.trim().take(500)
            require(cleanUri.isNotEmpty()) { "Please record or select a video first." }

            val cleanSource = when (sourceType.lowercase()) {
                "camera", "gallery", "sample" -> sourceType.lowercase()
                else -> "gallery"
            }
            val safeDuration = durationMs.coerceIn(1_000L, 600_000L)
            val safeCreatorName = creatorName.trim().ifEmpty { "ReelPulse Creator" }.take(50)
            val safeCreatorHandle = creatorHandle.trim().let {
                if (it.startsWith("@")) it else "@$it"
            }.ifEmpty { "@creator" }.take(32)

            val model = VideoMetadata(
                id = videoId,
                userId = uid,
                title = cleanTitle,
                caption = cleanCaption,
                category = cleanCategory,
                tags = cleanTags,
                soundTitle = cleanSound,
                videoUri = cleanUri,
                filterId = filterPreset.id.take(32),
                filterName = filterPreset.displayName.take(32),
                sourceType = cleanSource,
                durationMs = safeDuration,
                creatorName = safeCreatorName,
                creatorHandle = safeCreatorHandle
            )

            db.collection("users")
                .document(uid)
                .collection("videos")
                .document(videoId)
                .set(model.toCreateMap())
                .await()

            Result.success(model)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
