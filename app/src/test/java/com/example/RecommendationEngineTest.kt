package com.example

import com.example.data.local.SeedData
import com.example.data.local.WatchHistoryEntity
import com.example.recommendation.RecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {

    private val engine = RecommendationEngine()
    private val videos = SeedData.initialVideos(nowMs = 1_000_000_000L)

    @Test
    fun followAndLikesBoostMatchingVideosToTop() {
        val ranked = engine.rankForYouFeed(
            videos = videos,
            watchHistory = listOf(
                WatchHistoryEntity(
                    videoId = "vid_tech_synth",
                    creatorId = "creator_kai",
                    category = "Tech",
                    tagsCsv = "#tech,#synthwave,#studio,#gear",
                    watchTimeMs = 15_000L,
                    videoDurationMs = 15_000L,
                    completionRate = 1.0f,
                    watchedAtMs = 999_990_000L
                )
            ),
            likedIds = setOf("vid_tech_synth"),
            savedIds = emptySet(),
            followedCreatorIds = setOf("creator_kai"),
            commentCounts = emptyMap(),
            explorationRate = 0.05f,
            nowMs = 1_000_000_000L
        )

        // Unseen Tech video by followed creator Kai should rank at #1
        assertEquals("vid_tech_desk", ranked.first().video.id)
        assertTrue(ranked.first().matchScore >= 85)
    }

    @Test
    fun skippedVideoReceivesPenaltyAndLowerMatch() {
        val ranked = engine.rankForYouFeed(
            videos = videos,
            watchHistory = listOf(
                WatchHistoryEntity(
                    videoId = "vid_dance_neon",
                    creatorId = "creator_maya",
                    category = "Dance",
                    tagsCsv = "#dance,#choreo",
                    watchTimeMs = 1_200L,
                    videoDurationMs = 15_000L,
                    completionRate = 0.08f,
                    watchedAtMs = 999_995_000L
                )
            ),
            likedIds = emptySet(),
            savedIds = emptySet(),
            followedCreatorIds = emptySet(),
            commentCounts = emptyMap(),
            explorationRate = 0.15f,
            nowMs = 1_000_000_000L
        )

        val skippedItem = ranked.first { it.video.id == "vid_dance_neon" }
        assertTrue(skippedItem.breakdown.repeatPenaltyPoints < 0)
    }

    @Test
    fun roadmapContainsFourEvolutionStages() {
        val stages = engine.roadmapStages()
        assertEquals(4, stages.size)
        assertEquals("ACTIVE NOW", stages.first().status)
    }
}
