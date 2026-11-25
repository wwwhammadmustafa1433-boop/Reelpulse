package com.example.data.repository

import com.example.data.local.CommentEntity
import com.example.data.local.FollowEntity
import com.example.data.local.LikedVideoEntity
import com.example.data.local.ReelPulseDao
import com.example.data.local.SavedVideoEntity
import com.example.data.local.SeedData
import com.example.data.local.UserProfileEntity
import com.example.data.local.VideoEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.remote.VideoMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReelPulseRepository(private val dao: ReelPulseDao) {

    val videos: Flow<List<VideoEntity>> = dao.observeVideos()
    val watchHistory: Flow<List<WatchHistoryEntity>> = dao.observeWatchHistory()
    val likedVideoIds: Flow<Set<String>> = dao.observeLikedVideos().map { list ->
        list.map { it.videoId }.toSet()
    }
    val savedVideoIds: Flow<Set<String>> = dao.observeSavedVideos().map { list ->
        list.map { it.videoId }.toSet()
    }
    val followedCreatorIds: Flow<Set<String>> = dao.observeFollows().map { list ->
        list.map { it.creatorId }.toSet()
    }
    val commentCounts: Flow<Map<String, Int>> = dao.observeCommentCounts().map { list ->
        list.associate { it.videoId to it.count }
    }
    val userProfile: Flow<UserProfileEntity> = dao.observeUserProfile().map {
        it ?: SeedData.defaultProfile
    }

    suspend fun ensureSeeded() {
        if (dao.getVideoCount() == 0) {
            val now = System.currentTimeMillis()
            dao.insertVideos(SeedData.initialVideos(now))
            dao.insertComments(SeedData.initialComments(now))
            dao.upsertUserProfile(SeedData.defaultProfile)
        }
    }

    fun observeComments(videoId: String): Flow<List<CommentEntity>> =
        dao.observeCommentsForVideo(videoId)

    suspend fun toggleLike(videoId: String, currentlyLiked: Boolean) {
        if (currentlyLiked) {
            dao.unlikeVideo(videoId)
        } else {
            dao.likeVideo(LikedVideoEntity(videoId = videoId))
        }
    }

    suspend fun toggleSave(videoId: String, currentlySaved: Boolean) {
        if (currentlySaved) {
            dao.unsaveVideo(videoId)
        } else {
            dao.saveVideo(SavedVideoEntity(videoId = videoId))
        }
    }

    suspend fun toggleFollow(creatorId: String, creatorName: String, currentlyFollowed: Boolean) {
        if (currentlyFollowed) {
            dao.unfollowCreator(creatorId)
        } else {
            dao.followCreator(FollowEntity(creatorId = creatorId, creatorName = creatorName))
        }
    }

    suspend fun recordShare(videoId: String) {
        dao.incrementShareCount(videoId)
    }

    suspend fun recordWatchSession(
        video: VideoEntity,
        watchTimeMs: Long
    ) {
        if (watchTimeMs < 450L) return
        val duration = video.durationMs.coerceAtLeast(1000L)
        val completion = (watchTimeMs.toFloat() / duration.toFloat()).coerceIn(0f, 1.5f)
        dao.insertWatchHistory(
            WatchHistoryEntity(
                videoId = video.id,
                creatorId = video.creatorId,
                category = video.category,
                tagsCsv = video.tagsCsv,
                watchTimeMs = watchTimeMs,
                videoDurationMs = duration,
                completionRate = completion.coerceAtMost(1.0f)
            )
        )
    }

    suspend fun addComment(videoId: String, authorName: String, authorHandle: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insertComment(
            CommentEntity(
                videoId = videoId,
                authorName = authorName,
                authorHandle = authorHandle,
                text = trimmed
            )
        )
    }

    suspend fun publishVideo(
        id: String? = null,
        title: String,
        caption: String,
        category: String,
        tagsCsv: String,
        soundTitle: String,
        videoUrl: String,
        filterId: String = "normal",
        durationMs: Long = 15_000L,
        profile: UserProfileEntity
    ) {
        val now = System.currentTimeMillis()
        val videoId = id?.takeIf { it.isNotBlank() } ?: "vid_user_$now"
        val resolvedTitle = title.trim().ifBlank { "Untitled Pulse" }
        dao.insertVideo(
            VideoEntity(
                id = videoId,
                title = resolvedTitle,
                caption = caption.trim().ifBlank { resolvedTitle },
                creatorId = "local_user",
                creatorName = profile.displayName,
                creatorHandle = profile.handle,
                creatorAvatarKey = "local_user",
                videoUrl = videoUrl,
                category = category,
                tagsCsv = tagsCsv.split(",", " ")
                    .map { it.trim().replace(" ", "") }
                    .filter { it.isNotEmpty() && it != "#" }
                    .map { (if (it.startsWith("#")) it else "#$it").take(32) }
                    .distinctBy { it.lowercase() }
                    .take(10)
                    .joinToString(","),
                soundTitle = soundTitle.ifBlank { "Original Sound — ${profile.handle}" },
                durationMs = durationMs.coerceAtLeast(1_000L),
                baseLikesCount = 1,
                commentsCount = 0,
                sharesCount = 0,
                accentHex = 0xFFFF2E54,
                isUserCreated = true,
                filterId = filterId,
                createdAtMs = now
            )
        )
    }

    suspend fun syncFirestoreVideos(remoteVideos: List<VideoMetadata>) {
        if (remoteVideos.isEmpty()) return
        val entities = remoteVideos.mapNotNull { remote ->
            val docId = remote.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            VideoEntity(
                id = docId,
                title = remote.title.ifBlank { "Untitled Pulse" },
                caption = remote.caption.ifBlank { "Fresh clip shared on ReelPulse" },
                creatorId = "local_user",
                creatorName = remote.creatorName.ifBlank { SeedData.defaultProfile.displayName },
                creatorHandle = remote.creatorHandle.ifBlank { SeedData.defaultProfile.handle },
                creatorAvatarKey = "local_user",
                videoUrl = remote.videoUri,
                category = remote.category.ifBlank { "Tech" },
                tagsCsv = remote.tags.joinToString(","),
                soundTitle = remote.soundTitle.ifBlank { "Original Sound" },
                durationMs = remote.durationMs.coerceAtLeast(1_000L),
                baseLikesCount = 1,
                commentsCount = 0,
                sharesCount = 0,
                accentHex = 0xFFFF2E54,
                isUserCreated = true,
                filterId = remote.filterId.ifBlank { "normal" },
                createdAtMs = remote.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            )
        }
        if (entities.isNotEmpty()) {
            dao.insertVideos(entities)
        }
    }

    suspend fun updateProfile(displayName: String, handle: String, bio: String) {
        val sanitizedHandle = handle.trim().let {
            if (it.startsWith("@")) it else "@$it"
        }
        dao.upsertUserProfile(
            UserProfileEntity(
                id = "local_user",
                displayName = displayName.trim().ifEmpty { SeedData.defaultProfile.displayName },
                handle = sanitizedHandle.ifEmpty { SeedData.defaultProfile.handle },
                bio = bio.trim()
            )
        )
    }

    suspend fun resetPersonalizationSignals() {
        dao.clearWatchHistory()
        dao.clearLikes()
        dao.clearFollows()
    }
}
