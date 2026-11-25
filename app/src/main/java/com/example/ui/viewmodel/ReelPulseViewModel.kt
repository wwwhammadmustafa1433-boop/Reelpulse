package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.camera.CameraFilterPreset
import com.example.data.local.AppDatabase
import com.example.data.local.CommentEntity
import com.example.data.local.SeedData
import com.example.data.local.UserProfileEntity
import com.example.data.local.VideoEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.remote.AccountVisibility
import com.example.data.remote.BlockedUser
import com.example.data.remote.BlockedUserRepository
import com.example.data.remote.UserAccount
import com.example.data.remote.UserAccountRepository
import com.example.data.remote.VideoMetadataRepository
import com.example.data.repository.ReelPulseRepository
import com.example.recommendation.AlgorithmRoadmapStage
import com.example.recommendation.RecommendationEngine
import com.example.recommendation.ScoredVideo
import com.example.recommendation.TrendingTopic
import com.example.recommendation.UserInterestProfile
import com.example.ui.auth.PendingSignUpProfile
import com.example.ui.screens.SAMPLE_STUDIO_VIDEO_URL
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FeedTab {
    FOLLOWING,
    FOR_YOU
}

sealed interface AccountState {
    data object Loading : AccountState
    data class Success(val account: UserAccount?) : AccountState
    data class Error(val message: String) : AccountState
}

data class CreateDraftState(
    val videoUri: String? = null,
    val filterPreset: CameraFilterPreset = CameraFilterPreset.NORMAL,
    val sourceType: String = "gallery",
    val durationMs: Long = 15_000L,
    val tagsCsv: String = "",
    val isPublishing: Boolean = false,
    val publishError: String? = null
)

data class CreatorSummary(
    val creatorId: String,
    val creatorName: String,
    val creatorHandle: String,
    val creatorAvatarKey: String,
    val videoCount: Int
)

data class ReelPulseUiState(
    val isLoading: Boolean = true,
    val isRefreshingFeed: Boolean = false,
    val feedRefreshCount: Long = 0L,
    val selectedFeedTab: FeedTab = FeedTab.FOR_YOU,
    val forYouFeed: List<ScoredVideo> = emptyList(),
    val followingFeed: List<ScoredVideo> = emptyList(),
    val allScoredVideos: List<ScoredVideo> = emptyList(),
    val likedIds: Set<String> = emptySet(),
    val savedIds: Set<String> = emptySet(),
    val followedCreatorIds: Set<String> = emptySet(),
    val blockedUsers: List<BlockedUser> = emptyList(),
    val blockedCreatorIds: Set<String> = emptySet(),
    val knownCreators: List<CreatorSummary> = emptyList(),
    val isUpdatingBlockState: Boolean = false,
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
    val trendingTopics: List<TrendingTopic> = emptyList(),
    val explorationRate: Float = 0.15f,
    val roadmapStages: List<AlgorithmRoadmapStage> = emptyList(),
    val userProfile: UserProfileEntity = SeedData.defaultProfile,
    val userAccount: UserAccount? = null,
    val accountVisibility: AccountVisibility = AccountVisibility.PUBLIC,
    val isUpdatingVisibility: Boolean = false,
    val accountErrorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategoryFilter: String = "All",
    val selectedTrendingTag: String? = null,
    val activeVideoJumpId: String? = null,
    val createDraft: CreateDraftState = CreateDraftState()
)

class ReelPulseViewModel(
    private val repository: ReelPulseRepository,
    private val userAccountRepository: UserAccountRepository,
    private val videoMetadataRepository: VideoMetadataRepository,
    private val blockedUserRepository: BlockedUserRepository,
    private val currentUserId: String,
    private val pendingSignUp: PendingSignUpProfile? = null,
    private val engine: RecommendationEngine = RecommendationEngine()
) : ViewModel() {

    private val selectedFeedTab = MutableStateFlow(FeedTab.FOR_YOU)
    private val explorationRate = MutableStateFlow(0.15f)
    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("All")
    private val selectedTrendingTag = MutableStateFlow<String?>(null)
    private val activeCommentsVideoId = MutableStateFlow<String?>(null)
    private val activeVideoJumpId = MutableStateFlow<String?>(null)
    private val mutationError = MutableStateFlow<String?>(null)
    private val createDraftState = MutableStateFlow(CreateDraftState())
    private val isUpdatingVisibility = MutableStateFlow(false)
    private val isUpdatingBlockState = MutableStateFlow(false)
    private val isRefreshingFeed = MutableStateFlow(false)
    private val feedRefreshCount = MutableStateFlow(0L)

    private val blockedUsersState: StateFlow<List<BlockedUser>> =
        blockedUserRepository.observeBlockedUsers(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing blocked users", rootError)
                mutationError.value = rootError.message ?: "Failed to load blocked users"
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    private val accountState: StateFlow<AccountState> =
        userAccountRepository.observeUserAccount(currentUserId)
            .map<UserAccount?, AccountState> { AccountState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing user account", rootError)
                emit(AccountState.Error(rootError.message ?: "Failed to load account profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AccountState.Loading
            )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            val firebaseUser = Firebase.auth.currentUser
            val initialName = pendingSignUp?.displayName
                ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() }
                ?: SeedData.defaultProfile.displayName
            val initialHandle = pendingSignUp?.handle
                ?: ("@" + initialName.lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.'))
                    .let { if (it.length >= 2) it else SeedData.defaultProfile.handle }
            val result = userAccountRepository.ensureOrUpsertAccount(
                userId = currentUserId,
                displayName = initialName,
                handle = initialHandle,
                email = firebaseUser?.email,
                mobileNumber = pendingSignUp?.mobileNumber,
                bio = SeedData.defaultProfile.bio,
                overwriteExisting = pendingSignUp != null
            )
            if (result.isSuccess && pendingSignUp != null) {
                repository.updateProfile(
                    displayName = pendingSignUp.displayName,
                    handle = pendingSignUp.handle,
                    bio = SeedData.defaultProfile.bio
                )
            } else if (result.isFailure) {
                mutationError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Could not sync account with Firestore"
            }
        }

        // Sync user-posted video metadata from Firestore into local feed catalog
        viewModelScope.launch {
            videoMetadataRepository.observeUserVideos(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing Firestore videos", firestoreError ?: error)
                }
                .collectLatest { remoteVideos ->
                    repository.syncFirestoreVideos(remoteVideos)
                }
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

    private val discoverFilterFlow = combine(
        searchQuery,
        selectedCategory,
        selectedTrendingTag
    ) { query, category, trendingTag ->
        DiscoverFilterBundle(query, category, trendingTag)
    }

    private val uiControlFlow = combine(
        selectedFeedTab,
        explorationRate,
        discoverFilterFlow,
        activeVideoJumpId
    ) { tab, explore, discoverFilter, jumpId ->
        UiControlBundle(
            selectedTab = tab,
            explorationRate = explore,
            searchQuery = discoverFilter.searchQuery,
            selectedCategory = discoverFilter.selectedCategory,
            selectedTrendingTag = discoverFilter.selectedTrendingTag,
            jumpVideoId = jumpId
        )
    }

    private val profileSyncFlow = combine(
        repository.userProfile,
        accountState,
        mutationError,
        createDraftState,
        isUpdatingVisibility
    ) { localProfile, remoteState, opError, draft, updatingVis ->
        ProfileSyncBundle(localProfile, remoteState, opError, draft, updatingVis)
    }

    private val feedRefreshFlow = combine(
        isRefreshingFeed,
        feedRefreshCount,
        blockedUsersState,
        isUpdatingBlockState
    ) { refreshing, refreshCount, blockedList, updatingBlock ->
        FeedModerationBundle(refreshing, refreshCount, blockedList, updatingBlock)
    }

    val uiState: StateFlow<ReelPulseUiState> = combine(
        coreSignalsFlow,
        repository.commentCounts,
        profileSyncFlow,
        uiControlFlow,
        feedRefreshFlow
    ) { signals, commentCounts, profileBundle, controls, moderation ->
        val refreshingFeed = moderation.isRefreshingFeed
        val refreshTick = moderation.feedRefreshCount
        val blockedList = moderation.blockedUsers
        val blockedIds = blockedList.map { it.blockedCreatorId }.toSet()
        val localProfile = profileBundle.localProfile
        val remoteState = profileBundle.remoteState
        val opError = profileBundle.mutationError
        val remoteAccount = (remoteState as? AccountState.Success)?.account
        val effectiveProfile = if (remoteAccount != null) {
            UserProfileEntity(
                id = "local_user",
                displayName = remoteAccount.displayName.ifBlank { localProfile.displayName },
                handle = remoteAccount.handle.ifBlank { localProfile.handle },
                bio = remoteAccount.bio ?: localProfile.bio
            )
        } else {
            localProfile
        }
        val combinedAccountError = opError ?: (remoteState as? AccountState.Error)?.message

        val visibleVideos = signals.videos.filter { video ->
            video.isUserCreated || video.creatorId !in blockedIds
        }

        val knownCreators = signals.videos
            .filter { !it.isUserCreated && it.creatorId != "local_user" }
            .groupBy { it.creatorId }
            .mapNotNull { (creatorId, vids) ->
                val sample = vids.firstOrNull() ?: return@mapNotNull null
                CreatorSummary(
                    creatorId = creatorId,
                    creatorName = sample.creatorName,
                    creatorHandle = sample.creatorHandle,
                    creatorAvatarKey = sample.creatorAvatarKey,
                    videoCount = vids.size
                )
            }
            .sortedBy { it.creatorName }

        val ranked = engine.rankForYouFeed(
            videos = visibleVideos,
            watchHistory = signals.watchHistory,
            likedIds = signals.likedIds,
            savedIds = signals.savedIds,
            followedCreatorIds = signals.followedIds,
            commentCounts = commentCounts,
            explorationRate = controls.explorationRate,
            refreshSeed = refreshTick
        )
        val followingOnly = ranked.filter { it.isFollowed }
        val interestProfile = engine.buildUserInterestProfile(
            videos = visibleVideos,
            watchHistory = signals.watchHistory,
            likedIds = signals.likedIds,
            savedIds = signals.savedIds,
            followedCreatorIds = signals.followedIds
        )
        val trendingTopics = engine.computeTrendingTopics(
            videos = visibleVideos,
            likedIds = signals.likedIds,
            commentCounts = commentCounts
        )

        ReelPulseUiState(
            isLoading = false,
            isRefreshingFeed = refreshingFeed,
            feedRefreshCount = refreshTick,
            selectedFeedTab = controls.selectedTab,
            forYouFeed = ranked,
            followingFeed = followingOnly,
            allScoredVideos = ranked,
            likedIds = signals.likedIds,
            savedIds = signals.savedIds,
            followedCreatorIds = signals.followedIds,
            blockedUsers = blockedList,
            blockedCreatorIds = blockedIds,
            knownCreators = knownCreators,
            isUpdatingBlockState = moderation.isUpdatingBlockState,
            watchHistory = signals.watchHistory,
            interestProfile = interestProfile,
            trendingTopics = trendingTopics,
            explorationRate = controls.explorationRate,
            roadmapStages = engine.roadmapStages(),
            userProfile = effectiveProfile,
            userAccount = remoteAccount,
            accountVisibility = remoteAccount?.resolvedVisibility() ?: AccountVisibility.PUBLIC,
            isUpdatingVisibility = profileBundle.isUpdatingVisibility,
            accountErrorMessage = combinedAccountError,
            searchQuery = controls.searchQuery,
            selectedCategoryFilter = controls.selectedCategory,
            selectedTrendingTag = controls.selectedTrendingTag,
            activeVideoJumpId = controls.jumpVideoId,
            createDraft = profileBundle.draft
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
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
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    fun selectFeedTab(tab: FeedTab) {
        selectedFeedTab.value = tab
    }

    fun refreshForYouFeed() {
        if (isRefreshingFeed.value) return
        viewModelScope.launch {
            isRefreshingFeed.value = true
            val startedAt = System.currentTimeMillis()
            try {
                repository.ensureSeeded()
                val remoteVideos = videoMetadataRepository.fetchUserVideosOnce(currentUserId)
                repository.syncFirestoreVideos(remoteVideos)
                feedRefreshCount.value = feedRefreshCount.value + 1L
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed < MIN_REFRESH_ANIMATION_MS) {
                    delay(MIN_REFRESH_ANIMATION_MS - elapsed)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to reload For You recommendation queue", e)
            } finally {
                isRefreshingFeed.value = false
            }
        }
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

    fun selectTrendingTag(tag: String?) {
        if (tag.isNullOrBlank()) {
            selectedTrendingTag.value = null
            return
        }
        val normalized = RecommendationEngine.normalizeTagDisplay(tag)
        val current = selectedTrendingTag.value
        selectedTrendingTag.value = if (current != null &&
            RecommendationEngine.normalizeTagKey(current) == RecommendationEngine.normalizeTagKey(normalized)
        ) {
            null
        } else {
            normalized
        }
    }

    fun openDiscoverWithTag(tag: String) {
        val normalized = RecommendationEngine.normalizeTagDisplay(tag)
        if (normalized.isEmpty()) return
        selectedCategory.value = "All"
        searchQuery.value = ""
        selectedTrendingTag.value = normalized
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

    fun dismissAccountError() {
        mutationError.value = null
    }

    fun setDraftRecordedVideo(
        videoUri: String,
        filterPreset: CameraFilterPreset,
        durationMs: Long
    ) {
        createDraftState.value = createDraftState.value.copy(
            videoUri = videoUri,
            filterPreset = filterPreset,
            sourceType = "camera",
            durationMs = durationMs,
            publishError = null
        )
    }

    fun setDraftGalleryVideo(
        videoUri: String,
        filterPreset: CameraFilterPreset? = null
    ) {
        createDraftState.value = createDraftState.value.copy(
            videoUri = videoUri,
            filterPreset = filterPreset ?: createDraftState.value.filterPreset,
            sourceType = "gallery",
            durationMs = 15_000L,
            publishError = null
        )
    }

    fun setDraftSampleVideo() {
        createDraftState.value = createDraftState.value.copy(
            videoUri = SAMPLE_STUDIO_VIDEO_URL,
            sourceType = "sample",
            durationMs = 15_000L,
            publishError = null
        )
    }

    fun clearDraftVideo() {
        createDraftState.value = createDraftState.value.copy(
            videoUri = null,
            sourceType = "gallery",
            durationMs = 15_000L,
            publishError = null
        )
    }

    fun setDraftFilter(filterPreset: CameraFilterPreset) {
        createDraftState.value = createDraftState.value.copy(
            filterPreset = filterPreset
        )
    }

    fun setDraftTags(tagsCsv: String) {
        createDraftState.value = createDraftState.value.copy(
            tagsCsv = tagsCsv
        )
    }

    fun toggleDraftTrendingTag(tag: String) {
        val normalized = RecommendationEngine.normalizeTagDisplay(tag)
        if (normalized.isEmpty()) return
        val key = RecommendationEngine.normalizeTagKey(normalized)
        val currentTags = parseTagList(createDraftState.value.tagsCsv)
        val exists = currentTags.any { RecommendationEngine.normalizeTagKey(it) == key }
        val updated = if (exists) {
            currentTags.filterNot { RecommendationEngine.normalizeTagKey(it) == key }
        } else {
            if (currentTags.size >= 10) currentTags else currentTags + normalized
        }
        createDraftState.value = createDraftState.value.copy(
            tagsCsv = updated.joinToString(",")
        )
    }

    fun prepareDraftWithTrendingTag(tag: String) {
        val normalized = RecommendationEngine.normalizeTagDisplay(tag)
        if (normalized.isEmpty()) return
        val key = RecommendationEngine.normalizeTagKey(normalized)
        val currentTags = parseTagList(createDraftState.value.tagsCsv)
        if (currentTags.none { RecommendationEngine.normalizeTagKey(it) == key }) {
            val updated = (listOf(normalized) + currentTags).take(10)
            createDraftState.value = createDraftState.value.copy(
                tagsCsv = updated.joinToString(",")
            )
        }
    }

    private fun parseTagList(tagsCsv: String): List<String> =
        tagsCsv.split(",", " ")
            .map { RecommendationEngine.normalizeTagDisplay(it) }
            .filter { it.isNotEmpty() && it != "#" }
            .distinctBy { RecommendationEngine.normalizeTagKey(it) }
            .take(10)

    fun dismissPublishError() {
        createDraftState.value = createDraftState.value.copy(publishError = null)
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

    fun toggleFollowCreator(creatorId: String, creatorName: String) {
        val currentlyFollowed = creatorId in uiState.value.followedCreatorIds
        viewModelScope.launch {
            repository.toggleFollow(
                creatorId = creatorId,
                creatorName = creatorName,
                currentlyFollowed = currentlyFollowed
            )
        }
    }

    fun blockUser(
        creatorId: String,
        creatorName: String,
        creatorHandle: String,
        creatorAvatarKey: String,
        onComplete: () -> Unit = {}
    ) {
        if (isUpdatingBlockState.value) return
        viewModelScope.launch {
            mutationError.value = null
            isUpdatingBlockState.value = true
            val result = blockedUserRepository.blockUser(
                userId = currentUserId,
                creatorId = creatorId,
                creatorName = creatorName,
                creatorHandle = creatorHandle,
                creatorAvatarKey = creatorAvatarKey
            )
            if (result.isSuccess) {
                if (creatorId in uiState.value.followedCreatorIds) {
                    repository.toggleFollow(
                        creatorId = creatorId,
                        creatorName = creatorName,
                        currentlyFollowed = true
                    )
                }
                isUpdatingBlockState.value = false
                onComplete()
            } else {
                isUpdatingBlockState.value = false
                mutationError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Failed to block user"
            }
        }
    }

    fun unblockUser(creatorId: String) {
        if (isUpdatingBlockState.value) return
        viewModelScope.launch {
            mutationError.value = null
            isUpdatingBlockState.value = true
            val result = blockedUserRepository.unblockUser(
                userId = currentUserId,
                blockedCreatorId = creatorId
            )
            isUpdatingBlockState.value = false
            if (result.isFailure) {
                mutationError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Failed to unblock user"
            }
        }
    }

    fun shareVideo(scoredVideo: ScoredVideo) {
        viewModelScope.launch {
            repository.recordShare(scoredVideo.video.id)
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

    fun publishDraftVideo(
        title: String,
        caption: String,
        category: String,
        tagsCsv: String,
        soundTitle: String,
        onComplete: () -> Unit
    ) {
        val draft = createDraftState.value
        val videoUri = draft.videoUri
        if (videoUri.isNullOrBlank()) {
            createDraftState.value = draft.copy(
                publishError = "Please record or select a video before posting."
            )
            return
        }
        val profile = uiState.value.userProfile

        viewModelScope.launch {
            createDraftState.value = createDraftState.value.copy(
                isPublishing = true,
                publishError = null
            )

            val result = videoMetadataRepository.saveVideoMetadata(
                userId = currentUserId,
                title = title,
                caption = caption,
                category = category,
                tagsCsv = tagsCsv,
                soundTitle = soundTitle,
                videoUri = videoUri,
                filterPreset = draft.filterPreset,
                sourceType = draft.sourceType,
                durationMs = draft.durationMs,
                creatorName = profile.displayName,
                creatorHandle = profile.handle
            )

            result.onSuccess { savedMetadata ->
                repository.publishVideo(
                    id = savedMetadata.id,
                    title = savedMetadata.title,
                    caption = savedMetadata.caption,
                    category = savedMetadata.category,
                    tagsCsv = savedMetadata.tags.joinToString(","),
                    soundTitle = savedMetadata.soundTitle,
                    videoUrl = savedMetadata.videoUri,
                    filterId = savedMetadata.filterId,
                    durationMs = savedMetadata.durationMs,
                    profile = profile
                )
                createDraftState.value = CreateDraftState()
                selectedFeedTab.value = FeedTab.FOR_YOU
                onComplete()
            }.onFailure { error ->
                createDraftState.value = createDraftState.value.copy(
                    isPublishing = false,
                    publishError = error.localizedMessage ?: "Failed to save video metadata to Firestore."
                )
            }
        }
    }

    fun updateProfile(
        displayName: String,
        handle: String,
        mobileNumber: String?,
        bio: String
    ) {
        viewModelScope.launch {
            mutationError.value = null
            repository.updateProfile(displayName, handle, bio)
            val result = userAccountRepository.updateAccountProfile(
                userId = currentUserId,
                displayName = displayName,
                handle = handle,
                mobileNumber = mobileNumber,
                bio = bio
            )
            if (result.isFailure) {
                mutationError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Failed to update Firestore profile"
            }
        }
    }

    fun updateAccountVisibility(visibility: AccountVisibility) {
        viewModelScope.launch {
            mutationError.value = null
            isUpdatingVisibility.value = true
            val result = userAccountRepository.updateAccountVisibility(
                userId = currentUserId,
                visibility = visibility
            )
            isUpdatingVisibility.value = false
            if (result.isFailure) {
                mutationError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Failed to update account visibility"
            }
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

    private data class DiscoverFilterBundle(
        val searchQuery: String,
        val selectedCategory: String,
        val selectedTrendingTag: String?
    )

    private data class UiControlBundle(
        val selectedTab: FeedTab,
        val explorationRate: Float,
        val searchQuery: String,
        val selectedCategory: String,
        val selectedTrendingTag: String?,
        val jumpVideoId: String?
    )

    private data class ProfileSyncBundle(
        val localProfile: UserProfileEntity,
        val remoteState: AccountState,
        val mutationError: String?,
        val draft: CreateDraftState,
        val isUpdatingVisibility: Boolean
    )

    private data class FeedModerationBundle(
        val isRefreshingFeed: Boolean,
        val feedRefreshCount: Long,
        val blockedUsers: List<BlockedUser>,
        val isUpdatingBlockState: Boolean
    )

    companion object {
        private const val TAG = "ReelPulseVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
        private const val MIN_REFRESH_ANIMATION_MS = 550L

        fun provideFactory(
            currentUserId: String,
            pendingSignUp: PendingSignUpProfile?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = AppDatabase.getInstance(app)
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                ReelPulseViewModel(
                    repository = ReelPulseRepository(db.reelPulseDao()),
                    userAccountRepository = UserAccountRepository(firestore),
                    videoMetadataRepository = VideoMetadataRepository(firestore),
                    blockedUserRepository = BlockedUserRepository(firestore),
                    currentUserId = currentUserId,
                    pendingSignUp = pendingSignUp
                )
            }
        }
    }
}
