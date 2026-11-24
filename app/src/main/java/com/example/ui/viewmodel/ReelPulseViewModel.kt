package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.AppDatabase
import com.example.data.local.CommentEntity
import com.example.data.local.SeedData
import com.example.data.local.UserProfileEntity
import com.example.data.local.VideoEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.repository.ReelPulseRepository
import com.example.recommendation.AlgorithmRoadmapStage
import com.example.recommendation.RecommendationEngine
import com.example.recommendation.ScoredVideo
import com.example.recommendation.UserInterestProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FeedTab {
    FOLLOWING,
    FOR_YOU
}

data class ReelPulseUiState(
    val isLoading: Boolean = true,
    val selectedFeedTab: FeedTab = FeedTab.FOR_YOU,
    val forYouFeed: List<ScoredVideo> = emptyList(),
    val followingFeed: List<ScoredVideo> = emptyList(),
    val allScoredVideos: List<ScoredVideo> = emptyList(),
    val likedIds: Set<String> = emptySet(),
    val savedIds: Set<String> = emptySet(),
    val followedCreatorIds: Set<String> = emptySet(),
    val watchHistory: List<WatchHistoryEntity> = emptyList(),
    val interestProfile: UserInterestProfile = UserInterestProfile(
        topCategories = emptyList(),
        topTags = emptyList(),
        totalWatches = 0,
        totalWatchSeconds = 0L,
        avgCompletionRate = 0f,
        totalLikes = 0,
        totalFollows = 0
    ),
    val explorationRate: Float = 0.15f,
    val roadmapStages: List<AlgorithmRoadmapStage> = emptyList(),
    val userProfile: UserProfileEntity = SeedData.defaultProfile,
    val searchQuery: String = "",
    val selectedCategoryFilter: String = "All",
    val activeVideoJumpId: String? = null
)

class ReelPulseViewModel(
    private val repository: ReelPulseRepository,
    private val engine: RecommendationEngine = RecommendationEngine()
) : ViewModel() {

    private val selectedFeedTab = MutableStateFlow(FeedTab.FOR_YOU)
    private val explorationRate = MutableStateFlow(0.15f)
    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("All")
    private val activeCommentsVideoId = MutableStateFlow<String?>(null)
    private val activeVideoJumpId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    private val coreSignalsFlow = combine(
        repository.videos,
        repository.watchHistory,
        repository.likedVideoIds,
        repository.savedVideoIds,
        repository.followedCreatorIds
    ) { videos, history, liked, saved, followed ->
        SignalBundle(videos, history, liked, saved, followed)
    }

    private val uiControlFlow = combine(
        selectedFeedTab,
        explorationRate,
        searchQuery,
        selectedCategory,
        activeVideoJumpId
    ) { tab, explore, query, category, jumpId ->
        UiControlBundle(tab, explore, query, category, jumpId)
    }

    val uiState: StateFlow<ReelPulseUiState> = combine(
        coreSignalsFlow,
        repository.commentCounts,
        repository.userProfile,
        uiControlFlow
    ) { signals, commentCounts, profile, controls ->
        val ranked = engine.rankForYouFeed(
            videos = signals.videos,
            watchHistory = signals.watchHistory,
            likedIds = signals.likedIds,
            savedIds = signals.savedIds,
            followedCreatorIds = signals.followedIds,
            commentCounts = commentCounts,
            explorationRate = controls.explorationRate
        )
        val followingOnly = ranked.filter { it.isFollowed }
        val interestProfile = engine.buildUserInterestProfile(
            videos = signals.videos,
            watchHistory = signals.watchHistory,
            likedIds = signals.likedIds,
            savedIds = signals.savedIds,
            followedCreatorIds = signals.followedIds
        )

        ReelPulseUiState(
            isLoading = false,
            selectedFeedTab = controls.selectedTab,
            forYouFeed = ranked,
            followingFeed = followingOnly,
            allScoredVideos = ranked,
            likedIds = signals.likedIds,
            savedIds = signals.savedIds,
            followedCreatorIds = signals.followedIds,
            watchHistory = signals.watchHistory,
            interestProfile = interestProfile,
            explorationRate = controls.explorationRate,
            roadmapStages = engine.roadmapStages(),
            userProfile = profile,
            searchQuery = controls.searchQuery,
            selectedCategoryFilter = controls.selectedCategory,
            activeVideoJumpId = controls.jumpVideoId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ReelPulseUiState(roadmapStages = engine.roadmapStages())
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeComments: StateFlow<List<CommentEntity>> = activeCommentsVideoId
        .flatMapLatest { videoId ->
            if (videoId == null) flowOf(emptyList())
            else repository.observeComments(videoId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun selectFeedTab(tab: FeedTab) {
        selectedFeedTab.value = tab
    }

    fun setExplorationRate(rate: Float) {
        explorationRate.value = rate.coerceIn(0.05f, 0.50f)
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectCategoryFilter(category: String) {
        selectedCategory.value = category
    }

    fun openComments(videoId: String?) {
        activeCommentsVideoId.value = videoId
    }

    fun requestJumpToVideo(videoId: String) {
        selectedFeedTab.value = FeedTab.FOR_YOU
        activeVideoJumpId.value = videoId
    }

    fun clearJumpRequest() {
        activeVideoJumpId.value = null
    }

    fun toggleLike(scoredVideo: ScoredVideo) {
        viewModelScope.launch {
            repository.toggleLike(scoredVideo.video.id, scoredVideo.isLiked)
        }
    }

    fun ensureLiked(scoredVideo: ScoredVideo) {
        if (!scoredVideo.isLiked) {
            viewModelScope.launch {
                repository.toggleLike(scoredVideo.video.id, currentlyLiked = false)
            }
        }
    }

    fun toggleSave(scoredVideo: ScoredVideo) {
        viewModelScope.launch {
            repository.toggleSave(scoredVideo.video.id, scoredVideo.isSaved)
        }
    }

    fun toggleFollow(scoredVideo: ScoredVideo) {
        viewModelScope.launch {
            repository.toggleFollow(
                creatorId = scoredVideo.video.creatorId,
                creatorName = scoredVideo.video.creatorName,
                currentlyFollowed = scoredVideo.isFollowed
            )
        }
    }

    fun recordWatchSession(video: VideoEntity, watchTimeMs: Long) {
        viewModelScope.launch {
            repository.recordWatchSession(video, watchTimeMs)
        }
    }

    fun postComment(videoId: String, text: String) {
        val profile = uiState.value.userProfile
        viewModelScope.launch {
            repository.addComment(
                videoId = videoId,
                authorName = profile.displayName,
                authorHandle = profile.handle,
                text = text
            )
        }
    }

    fun publishVideo(
        title: String,
        caption: String,
        category: String,
        tagsCsv: String,
        soundTitle: String,
        videoUrl: String,
        onComplete: () -> Unit
    ) {
        val profile = uiState.value.userProfile
        viewModelScope.launch {
            repository.publishVideo(
                title = title,
                caption = caption,
                category = category,
                tagsCsv = tagsCsv,
                soundTitle = soundTitle,
                videoUrl = videoUrl,
                profile = profile
            )
            selectedFeedTab.value = FeedTab.FOR_YOU
            onComplete()
        }
    }

    fun updateProfile(displayName: String, handle: String, bio: String) {
        viewModelScope.launch {
            repository.updateProfile(displayName, handle, bio)
        }
    }

    fun resetPersonalizationSignals() {
        viewModelScope.launch {
            repository.resetPersonalizationSignals()
        }
    }

    private data class SignalBundle(
        val videos: List<VideoEntity>,
        val watchHistory: List<WatchHistoryEntity>,
        val likedIds: Set<String>,
        val savedIds: Set<String>,
        val followedIds: Set<String>
    )

    private data class UiControlBundle(
        val selectedTab: FeedTab,
        val explorationRate: Float,
        val searchQuery: String,
        val selectedCategory: String,
        val jumpVideoId: String?
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = AppDatabase.getInstance(app)
                ReelPulseViewModel(ReelPulseRepository(db.reelPulseDao()))
            }
        }
    }
}
