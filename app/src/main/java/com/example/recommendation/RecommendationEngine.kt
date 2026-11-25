package com.example.recommendation

import com.example.data.local.VideoEntity
import com.example.data.local.WatchHistoryEntity
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

data class RecommendationBreakdown(
    val followPoints: Int,
    val categoryPoints: Int,
    val tagPoints: Int,
    val creatorLoyaltyPoints: Int,
    val explorationPoints: Int,
    val repeatPenaltyPoints: Int
)

data class ScoredVideo(
    val video: VideoEntity,
    val matchScore: Int,
    val primaryReason: String,
    val breakdown: RecommendationBreakdown,
    val isLiked: Boolean,
    val isSaved: Boolean,
    val isFollowed: Boolean,
    val totalLikesCount: Int,
    val totalCommentsCount: Int
)

data class CategoryAffinity(
    val category: String,
    val rawScore: Float,
    val normalizedWeight: Float
)

data class TagAffinity(
    val tag: String,
    val score: Float
)

data class TrendingTopic(
    val tag: String,
    val categoryHint: String,
    val videoCount: Int,
    val engagementScore: Long,
    val isHot: Boolean = false,
    val isCurated: Boolean = false
)

data class UserInterestProfile(
    val topCategories: List<CategoryAffinity>,
    val topTags: List<TagAffinity>,
    val totalWatches: Int,
    val totalWatchSeconds: Long,
    val avgCompletionRate: Float,
    val totalLikes: Int,
    val totalFollows: Int
)

data class AlgorithmRoadmapStage(
    val stageNumber: Int,
    val title: String,
    val status: String,
    val summary: String,
    val keyTechniques: List<String>
)

class RecommendationEngine {

    fun buildUserInterestProfile(
        videos: List<VideoEntity>,
        watchHistory: List<WatchHistoryEntity>,
        likedIds: Set<String>,
        savedIds: Set<String>,
        followedCreatorIds: Set<String>,
        nowMs: Long = System.currentTimeMillis()
    ): UserInterestProfile {
        val videoById = videos.associateBy { it.id }
        val categoryScores = mutableMapOf<String, Float>()
        val tagScores = mutableMapOf<String, Float>()

        // 1. Watch History signals: completion >= 60% boosts interest, quick skips (< 20%) penalize slightly
        for (watch in watchHistory) {
            val ageHours = ((nowMs - watch.watchedAtMs).coerceAtLeast(0L)) / 3_600_000f
            val recencyWeight = exp(-ageHours / 72f).coerceIn(0.25f, 1.0f)
            val signal = when {
                watch.completionRate >= 0.85f -> 2.2f * recencyWeight
                watch.completionRate >= 0.50f -> 1.2f * recencyWeight
                watch.completionRate >= 0.25f -> 0.4f * recencyWeight
                else -> -0.6f * recencyWeight
            }
            categoryScores[watch.category] = (categoryScores[watch.category] ?: 0f) + signal
            watch.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                tagScores[tag] = (tagScores[tag] ?: 0f) + signal * 0.7f
            }
        }

        // 2. Explicit Likes (+2.8 weight)
        for (likedId in likedIds) {
            val video = videoById[likedId] ?: continue
            categoryScores[video.category] = (categoryScores[video.category] ?: 0f) + 2.8f
            video.tags.forEach { tag ->
                tagScores[tag] = (tagScores[tag] ?: 0f) + 1.8f
            }
        }

        // 3. Saved / Bookmarked clips (+2.2 weight)
        for (savedId in savedIds) {
            val video = videoById[savedId] ?: continue
            categoryScores[video.category] = (categoryScores[video.category] ?: 0f) + 2.2f
            video.tags.forEach { tag ->
                tagScores[tag] = (tagScores[tag] ?: 0f) + 1.4f
            }
        }

        // 4. Followed Creators (+1.6 category affinity)
        videos.filter { it.creatorId in followedCreatorIds }.forEach { video ->
            categoryScores[video.category] = (categoryScores[video.category] ?: 0f) + 0.8f
        }

        val maxCategory = categoryScores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val topCategories = categoryScores.entries
            .sortedByDescending { it.value }
            .map { (cat, raw) ->
                CategoryAffinity(
                    category = cat,
                    rawScore = raw,
                    normalizedWeight = (raw / maxCategory).coerceIn(0f, 1f)
                )
            }

        val topTags = tagScores.entries
            .filter { it.value > 0f }
            .sortedByDescending { it.value }
            .take(8)
            .map { TagAffinity(tag = it.key, score = it.value) }

        val totalWatchMs = watchHistory.sumOf { it.watchTimeMs }
        val avgCompletion = if (watchHistory.isEmpty()) 0f else {
            watchHistory.map { it.completionRate }.average().toFloat()
        }

        return UserInterestProfile(
            topCategories = topCategories,
            topTags = topTags,
            totalWatches = watchHistory.size,
            totalWatchSeconds = totalWatchMs / 1000L,
            avgCompletionRate = avgCompletion,
            totalLikes = likedIds.size,
            totalFollows = followedCreatorIds.size
        )
    }

    fun rankForYouFeed(
        videos: List<VideoEntity>,
        watchHistory: List<WatchHistoryEntity>,
        likedIds: Set<String>,
        savedIds: Set<String>,
        followedCreatorIds: Set<String>,
        commentCounts: Map<String, Int>,
        explorationRate: Float = 0.15f,
        nowMs: Long = System.currentTimeMillis(),
        refreshSeed: Long = 0L
    ): List<ScoredVideo> {
        val profile = buildUserInterestProfile(
            videos = videos,
            watchHistory = watchHistory,
            likedIds = likedIds,
            savedIds = savedIds,
            followedCreatorIds = followedCreatorIds,
            nowMs = nowMs
        )

        val categoryWeightMap = profile.topCategories.associate { it.category to it.normalizedWeight }
        val maxTagScore = profile.topTags.maxOfOrNull { it.score }?.coerceAtLeast(1f) ?: 1f
        val tagWeightMap = profile.topTags.associate { it.tag to (it.score / maxTagScore).coerceIn(0f, 1f) }

        // Creator watch loyalty from watch history completion rates
        val creatorWatches = watchHistory.groupBy { it.creatorId }
        val watchesByVideo = watchHistory.groupBy { it.videoId }
        val hasPersonalSignals = watchHistory.isNotEmpty() || likedIds.isNotEmpty() ||
            savedIds.isNotEmpty() || followedCreatorIds.isNotEmpty()

        data class RankedCandidate(val rawScore: Int, val scored: ScoredVideo)

        return videos.map { video ->
            val isLiked = video.id in likedIds
            val isSaved = video.id in savedIds
            val isFollowed = video.creatorId in followedCreatorIds
            val commentsCount = maxOf(video.commentsCount, commentCounts[video.id] ?: 0)
            val likesCount = video.baseLikesCount + if (isLiked) 1 else 0

            // Signal 1: Follow Boost (0..24)
            val followPoints = if (isFollowed) 24 else 0

            // Signal 2: Category Affinity from Watch History + Likes + Saves (0..24)
            val catWeight = categoryWeightMap[video.category] ?: 0f
            val categoryPoints = if (hasPersonalSignals) {
                (catWeight * 24f).roundToInt()
            } else {
                // Cold-start popularity baseline
                12
            }

            // Signal 3: Tag / Topic Overlap (0..14)
            val tagOverlap = if (video.tags.isEmpty()) 0f else {
                video.tags.mapNotNull { tagWeightMap[it] }.average().let {
                    if (it.isNaN()) 0f else it.toFloat()
                }
            }
            val tagPoints = (tagOverlap * 14f).roundToInt()

            // Signal 4: Creator Watch Loyalty (0..10)
            val creatorAvgCompletion = creatorWatches[video.creatorId]
                ?.map { it.completionRate }
                ?.average()
                ?.toFloat() ?: 0f
            val creatorLoyaltyPoints = if (creatorAvgCompletion >= 0.45f) {
                (creatorAvgCompletion * 10f).roundToInt()
            } else {
                0
            }

            // Signal 5: Exploration & Freshness Boost (2..18)
            val deterministicDiversity = if (refreshSeed == 0L) {
                abs(video.id.hashCode() % 100) / 100f
            } else {
                val mixedHash = (video.id.hashCode().toLong() * 31L) xor
                    (refreshSeed * 1_103_515_245L + 12_345L)
                abs(mixedHash % 100L) / 100f
            }
            val unseenBonus = if (watchesByVideo[video.id].isNullOrEmpty()) 1.0f else 0.2f
            val popularityBaseline = ((video.baseLikesCount / 45_000f).coerceIn(0f, 1f) * 8f)
            val refreshJitter = if (refreshSeed > 0L) {
                deterministicDiversity * (6f + explorationRate * 12f)
            } else {
                0f
            }
            val explorationPoints = (
                (explorationRate * 22f * deterministicDiversity * unseenBonus) +
                    (popularityBaseline * (1f - explorationRate * 0.5f)) +
                    refreshJitter +
                    if (video.isUserCreated) 12f else 0f
                ).roundToInt().coerceIn(2, 18)

            // Signal 6: Saturation / Already-Watched Penalty (0..-20)
            val latestWatch = watchesByVideo[video.id]?.maxByOrNull { it.watchedAtMs }
            val repeatPenaltyPoints = when {
                latestWatch == null -> 0
                latestWatch.completionRate >= 0.85f -> -20
                latestWatch.completionRate >= 0.45f -> -10
                latestWatch.completionRate < 0.18f -> -14 // User skipped quickly
                else -> -5
            }

            val rawTotal = 22 +
                followPoints +
                categoryPoints +
                tagPoints +
                creatorLoyaltyPoints +
                explorationPoints +
                repeatPenaltyPoints

            val matchScore = rawTotal.coerceIn(28, 99)

            val primaryReason = when {
                video.isUserCreated -> "Your Published Short"
                isFollowed -> "Following ${video.creatorHandle}"
                categoryPoints >= 16 -> "Matches ${video.category} interest"
                tagPoints >= 8 -> "Liked ${video.tags.firstOrNull().orEmpty()}"
                creatorLoyaltyPoints >= 7 -> "High watch time on ${video.creatorHandle}"
                !hasPersonalSignals -> "Trending on ReelPulse"
                else -> "Fresh ${video.category} Discovery"
            }

            RankedCandidate(
                rawScore = rawTotal,
                scored = ScoredVideo(
                    video = video,
                    matchScore = matchScore,
                    primaryReason = primaryReason,
                    breakdown = RecommendationBreakdown(
                        followPoints = followPoints,
                        categoryPoints = categoryPoints,
                        tagPoints = tagPoints,
                        creatorLoyaltyPoints = creatorLoyaltyPoints,
                        explorationPoints = explorationPoints,
                        repeatPenaltyPoints = repeatPenaltyPoints
                    ),
                    isLiked = isLiked,
                    isSaved = isSaved,
                    isFollowed = isFollowed,
                    totalLikesCount = likesCount,
                    totalCommentsCount = commentsCount
                )
            )
        }.sortedWith(
            compareByDescending<RankedCandidate> { it.rawScore }
                .thenByDescending { it.scored.video.createdAtMs }
        ).map { it.scored }
    }

    fun roadmapStages(): List<AlgorithmRoadmapStage> = listOf(
        AlgorithmRoadmapStage(
            stageNumber = 1,
            title = "Stage 1: Weighted Heuristic Scoring",
            status = "ACTIVE NOW",
            summary = "Real-time on-device multi-signal ranker combining follow boosts, watch completion rates, skip penalties, like/save category vectors, and tunable exploration.",
            keyTechniques = listOf(
                "Completion-rate weighting (>85% watch boosts, <18% skip penalizes)",
                "Exponential 72h recency decay on watch events",
                "Epsilon-style diversity boost to prevent filter bubbles"
            )
        ),
        AlgorithmRoadmapStage(
            stageNumber = 2,
            title = "Stage 2: Session Context & Negative Feedback",
            status = "NEXT UP",
            summary = "Adapt within a single swipe session based on immediate mood, time-of-day, audio mute state, and explicit 'Not Interested' signals.",
            keyTechniques = listOf(
                "Short-term sliding window (last 5 swipes weighted 3x higher)",
                "Creator & sound fatigue throttling (avoid back-to-back repeats)",
                "Rewatch & share velocity multipliers"
            )
        ),
        AlgorithmRoadmapStage(
            stageNumber = 3,
            title = "Stage 3: Collaborative Filtering",
            status = "SCALE PHASE",
            summary = "Discover cross-topic taste bridges by matching users with similar watch-and-like cohorts using item-item and matrix factorization models.",
            keyTechniques = listOf(
                "Implicit feedback ALS / BPR embeddings over user-video matrix",
                "Co-watch graph ('viewers who finished X also finished Y')",
                "Trending velocity normalization across creator follower tiers"
            )
        ),
        AlgorithmRoadmapStage(
            stageNumber = 4,
            title = "Stage 4: Two-Tower Neural Retrieval & Multi-Task Ranker",
            status = "PRODUCTION ML",
            summary = "Separate fast candidate retrieval from deep multi-objective ranking that jointly predicts watch time, like, comment, share, and follow probabilities.",
            keyTechniques = listOf(
                "Two-tower user & video embeddings with ANN vector search",
                "MMoE / multi-task head: w1*E[WatchTime] + w2*P(Like) + w3*P(Follow)",
                "Contextual bandits (Thompson Sampling) for cold-start new uploads"
            )
        )
    )

    fun computeTrendingTopics(
        videos: List<VideoEntity>,
        likedIds: Set<String> = emptySet(),
        commentCounts: Map<String, Int> = emptyMap(),
        limit: Int = 48
    ): List<TrendingTopic> {
        data class MutableTagStats(
            val displayTag: String,
            val categoryVotes: MutableMap<String, Int> = mutableMapOf(),
            var videoCount: Int = 0,
            var engagementScore: Long = 0L,
            var isCurated: Boolean = false
        )

        val statsByKey = linkedMapOf<String, MutableTagStats>()

        curatedTrendingTopics.forEach { (curatedTag, categoryHint) ->
            val normalizedKey = normalizeTagKey(curatedTag)
            if (normalizedKey.isNotEmpty()) {
                statsByKey[normalizedKey] = MutableTagStats(
                    displayTag = curatedTag,
                    categoryVotes = mutableMapOf(categoryHint to 1),
                    isCurated = true
                )
            }
        }

        for (video in videos) {
            val likes = video.baseLikesCount.toLong() + if (video.id in likedIds) 1L else 0L
            val comments = maxOf(video.commentsCount, commentCounts[video.id] ?: 0).toLong()
            val shares = video.sharesCount.toLong()
            val videoPulse = likes + (shares * 3L) + (comments * 5L) + 100L

            val seenInVideo = mutableSetOf<String>()
            for (rawTag in video.tags) {
                val cleanTag = normalizeTagDisplay(rawTag)
                val key = normalizeTagKey(cleanTag)
                if (key.isEmpty() || !seenInVideo.add(key)) continue

                val existing = statsByKey[key]
                if (existing == null) {
                    statsByKey[key] = MutableTagStats(
                        displayTag = cleanTag,
                        categoryVotes = mutableMapOf(video.category to 1),
                        videoCount = 1,
                        engagementScore = videoPulse,
                        isCurated = false
                    )
                } else {
                    existing.videoCount += 1
                    existing.engagementScore += videoPulse
                    existing.categoryVotes[video.category] =
                        (existing.categoryVotes[video.category] ?: 0) + 1
                }
            }
        }

        val sorted = statsByKey.values
            .sortedWith(
                compareByDescending<MutableTagStats> { it.videoCount > 0 }
                    .thenByDescending { it.engagementScore }
                    .thenByDescending { it.videoCount }
                    .thenBy { it.displayTag.lowercase() }
            )
            .take(limit)

        var hotAssigned = 0
        return sorted.map { stats ->
            val topCategory = stats.categoryVotes.maxByOrNull { it.value }?.key ?: "All"
            val markHot = stats.videoCount > 0 && hotAssigned < 4
            if (markHot) hotAssigned += 1
            TrendingTopic(
                tag = stats.displayTag,
                categoryHint = topCategory,
                videoCount = stats.videoCount,
                engagementScore = stats.engagementScore,
                isHot = markHot,
                isCurated = stats.isCurated
            )
        }
    }

    companion object {
        val curatedTrendingTopics: List<Pair<String, String>> = listOf(
            "#NeonVibes" to "Dance",
            "#DeskSetup" to "Tech",
            "#MidnightChef" to "Food",
            "#SynthWave" to "Music",
            "#GoldenHour" to "Travel",
            "#CoreFlow" to "Fitness",
            "#ShaderArt" to "Art",
            "#BehindTheScenes" to "All",
            "#QuickTips" to "Tech",
            "#POV" to "Travel"
        )

        fun normalizeTagDisplay(raw: String): String {
            val trimmed = raw.trim().replace(" ", "")
            if (trimmed.isEmpty()) return ""
            return if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        }

        fun normalizeTagKey(raw: String): String {
            return raw.trim().removePrefix("#").lowercase()
        }
    }
}
