package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val caption: String,
    val creatorId: String,
    val creatorName: String,
    val creatorHandle: String,
    val creatorAvatarKey: String,
    val videoUrl: String,
    val category: String,
    val tagsCsv: String,
    val soundTitle: String,
    val durationMs: Long,
    val baseLikesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val accentHex: Long,
    val isUserCreated: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    val tags: List<String>
        get() = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val creatorId: String,
    val category: String,
    val tagsCsv: String,
    val watchTimeMs: Long,
    val videoDurationMs: Long,
    val completionRate: Float,
    val watchedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "liked_videos")
data class LikedVideoEntity(
    @PrimaryKey val videoId: String,
    val likedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_videos")
data class SavedVideoEntity(
    @PrimaryKey val videoId: String,
    val savedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey val creatorId: String,
    val creatorName: String,
    val followedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val authorName: String,
    val authorHandle: String,
    val text: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "local_user",
    val displayName: String,
    val handle: String,
    val bio: String
)
