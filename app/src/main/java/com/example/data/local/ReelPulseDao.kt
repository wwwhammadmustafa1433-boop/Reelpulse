package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelPulseDao {
    @Query("SELECT * FROM videos ORDER BY createdAtMs DESC")
    fun observeVideos(): Flow<List<VideoEntity>>

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Query("UPDATE videos SET sharesCount = sharesCount + 1 WHERE id = :videoId")
    suspend fun incrementShareCount(videoId: String)

    @Query("SELECT * FROM watch_history ORDER BY watchedAtMs DESC")
    fun observeWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Insert
    suspend fun insertWatchHistory(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history")
    suspend fun clearWatchHistory()

    @Query("SELECT * FROM liked_videos ORDER BY likedAtMs DESC")
    fun observeLikedVideos(): Flow<List<LikedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun likeVideo(entity: LikedVideoEntity)

    @Query("DELETE FROM liked_videos WHERE videoId = :videoId")
    suspend fun unlikeVideo(videoId: String)

    @Query("DELETE FROM liked_videos")
    suspend fun clearLikes()

    @Query("SELECT * FROM saved_videos ORDER BY savedAtMs DESC")
    fun observeSavedVideos(): Flow<List<SavedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVideo(entity: SavedVideoEntity)

    @Query("DELETE FROM saved_videos WHERE videoId = :videoId")
    suspend fun unsaveVideo(videoId: String)

    @Query("SELECT * FROM follows ORDER BY followedAtMs DESC")
    fun observeFollows(): Flow<List<FollowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun followCreator(entity: FollowEntity)

    @Query("DELETE FROM follows WHERE creatorId = :creatorId")
    suspend fun unfollowCreator(creatorId: String)

    @Query("DELETE FROM follows")
    suspend fun clearFollows()

    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY createdAtMs DESC")
    fun observeCommentsForVideo(videoId: String): Flow<List<CommentEntity>>

    @Query("SELECT videoId, COUNT(*) as count FROM comments GROUP BY videoId")
    fun observeCommentCounts(): Flow<List<CommentCountTuple>>

    @Insert
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Query("SELECT * FROM user_profile WHERE id = 'local_user' LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)
}

data class CommentCountTuple(
    val videoId: String,
    val count: Int
)
