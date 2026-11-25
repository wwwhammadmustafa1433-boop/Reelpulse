package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.camera.CameraFilterPreset
import com.example.data.remote.AccountVisibility
import com.example.data.remote.BlockedUserRepository
import com.example.data.remote.LiveChatMessage
import com.example.data.remote.LiveStreamRepository
import com.example.data.remote.LiveStreamSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LiveRoomMode {
    HUB,
    PRE_LIVE_SETUP,
    HOST_BROADCASTING,
    VIEWING_STREAM,
    ENDED_SUMMARY
}

data class LiveStreamUiState(
    val mode: LiveRoomMode = LiveRoomMode.HUB,
    val isLoadingStreams: Boolean = true,
    val liveStreams: List<LiveStreamSession> = emptyList(),
    val activeStream: LiveStreamSession? = null,
    val chatMessages: List<LiveChatMessage> = emptyList(),
    val selectedFilter: CameraFilterPreset = CameraFilterPreset.NORMAL,
    val draftTitle: String = "",
    val draftCategory: String = "Music",
    val isStartingStream: Boolean = false,
    val isEndingStream: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val summaryLikes: Int = 0,
    val summaryDurationSeconds: Long = 0L,
    val summaryTitle: String = "",
    val heartPulseTick: Long = 0L,
    val errorMessage: String? = null
)

class LiveStreamViewModel(
    application: Application,
    private val currentUserId: String,
    initialStartInSetup: Boolean,
    initialFilterId: String
) : AndroidViewModel(application) {

    private val repository = LiveStreamRepository(application.applicationContext)
    private val blockedUserRepository = BlockedUserRepository(application.applicationContext)

    private val modeState = MutableStateFlow(
        if (initialStartInSetup) LiveRoomMode.PRE_LIVE_SETUP else LiveRoomMode.HUB
    )
    private val activeStreamId = MutableStateFlow<String?>(null)
    private val activeStreamSnapshotSeed = MutableStateFlow<LiveStreamSession?>(null)
    private val selectedFilter = MutableStateFlow(CameraFilterPreset.fromId(initialFilterId))
    private val draftTitle = MutableStateFlow("")
    private val draftCategory = MutableStateFlow("Music")
    private val isStartingStream = MutableStateFlow(false)
    private val isEndingStream = MutableStateFlow(false)
    private val elapsedSeconds = MutableStateFlow(0L)
    private val summaryLikes = MutableStateFlow(0)
    private val summaryDurationSeconds = MutableStateFlow(0L)
    private val summaryTitle = MutableStateFlow("")
    private val heartPulseTick = MutableStateFlow(0L)
    private val errorMessage = MutableStateFlow<String?>(null)

    private var timerJob: Job? = null
    private var lastObservedLikeCount = -1

    private val discoveredStreamsFlow = combine(
        repository.observePublicLiveStreams().catch { emit(emptyList()) },
        repository.observeMyLiveStreams().catch { emit(emptyList()) },
        blockedUserRepository.observeBlockedUsers(currentUserId).catch { emit(emptyList()) }
    ) { publicStreams, myStreams, blockedUsers ->
        val blockedIds = blockedUsers.map { it.blockedCreatorId }.toSet()
        val blockedHandles = blockedUsers.map { it.blockedCreatorHandle.lowercase() }.toSet()
        (myStreams + publicStreams)
            .distinctBy { it.id }
            .filter { stream ->
                stream.hostUserId == currentUserId ||
                    (stream.hostUserId !in blockedIds && stream.hostHandle.lowercase() !in blockedHandles)
            }
            .sortedByDescending { it.startedAt?.toDate()?.time ?: 0L }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeStreamFlow = activeStreamId.flatMapLatest { streamId ->
        if (streamId == null) {
            flowOf(null)
        } else {
            repository.observeStream(streamId).catch { err ->
                errorMessage.value = err.localizedMessage ?: "Connection to live stream interrupted."
                emit(activeStreamSnapshotSeed.value)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeChatFlow = combine(activeStreamId, activeStreamSnapshotSeed, activeStreamFlow) { id, seed, live ->
        id?.let { live ?: seed }
    }.flatMapLatest { stream ->
        if (stream == null) {
            flowOf(emptyList())
        } else {
            repository.observeStreamChat(
                streamId = stream.id,
                streamHostId = stream.hostUserId,
                streamVisibility = stream.visibility
            ).catch { emit(emptyList()) }
        }
    }

    private data class DraftControlsBundle(
        val mode: LiveRoomMode,
        val filter: CameraFilterPreset,
        val title: String,
        val category: String,
        val isStarting: Boolean,
        val isEnding: Boolean,
        val elapsed: Long,
        val summaryLikes: Int,
        val summaryDuration: Long,
        val summaryTitle: String,
        val heartTick: Long,
        val error: String?
    )

    private val draftControlsFlow = combine(
        combine(modeState, selectedFilter, draftTitle, draftCategory) { m, f, t, c ->
            listOf(m, f, t, c)
        },
        combine(isStartingStream, isEndingStream, elapsedSeconds, heartPulseTick) { s, e, el, h ->
            listOf(s, e, el, h)
        },
        combine(summaryLikes, summaryDurationSeconds, summaryTitle, errorMessage) { sl, sd, st, err ->
            listOf(sl, sd, st, err)
        }
    ) { g1, g2, g3 ->
        DraftControlsBundle(
            mode = g1[0] as LiveRoomMode,
            filter = g1[1] as CameraFilterPreset,
            title = g1[2] as String,
            category = g1[3] as String,
            isStarting = g2[0] as Boolean,
            isEnding = g2[1] as Boolean,
            elapsed = g2[2] as Long,
            heartTick = g2[3] as Long,
            summaryLikes = g3[0] as Int,
            summaryDuration = g3[1] as Long,
            summaryTitle = g3[2] as String,
            error = g3[3] as String?
        )
    }

    val uiState: StateFlow<LiveStreamUiState> = combine(
        discoveredStreamsFlow,
        activeStreamFlow,
        activeChatFlow,
        draftControlsFlow
    ) { streams, active, chat, controls ->
        val resolvedActive = active ?: activeStreamSnapshotSeed.value
        if (resolvedActive != null && resolvedActive.likeCount > lastObservedLikeCount && lastObservedLikeCount >= 0) {
            heartPulseTick.value += 1L
        }
        if (resolvedActive != null) {
            lastObservedLikeCount = resolvedActive.likeCount
        }

        // If viewer is watching a stream that host just ended, move cleanly to summary
        val effectiveMode = if (
            controls.mode == LiveRoomMode.VIEWING_STREAM &&
            resolvedActive != null &&
            resolvedActive.status == "ended"
        ) {
            summaryTitle.value = resolvedActive.title
            summaryLikes.value = resolvedActive.likeCount
            summaryDurationSeconds.value = controls.elapsed
            stopTimer()
            LiveRoomMode.ENDED_SUMMARY
        } else {
            controls.mode
        }

        LiveStreamUiState(
            mode = effectiveMode,
            isLoadingStreams = false,
            liveStreams = streams,
            activeStream = resolvedActive,
            chatMessages = chat,
            selectedFilter = controls.filter,
            draftTitle = controls.title,
            draftCategory = controls.category,
            isStartingStream = controls.isStarting,
            isEndingStream = controls.isEnding,
            elapsedSeconds = controls.elapsed,
            summaryLikes = controls.summaryLikes,
            summaryDurationSeconds = controls.summaryDuration,
            summaryTitle = controls.summaryTitle,
            heartPulseTick = controls.heartTick,
            errorMessage = controls.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LiveStreamUiState(
            mode = if (initialStartInSetup) LiveRoomMode.PRE_LIVE_SETUP else LiveRoomMode.HUB,
            selectedFilter = CameraFilterPreset.fromId(initialFilterId)
        )
    )

    fun openGoLiveSetup() {
        errorMessage.value = null
        modeState.value = LiveRoomMode.PRE_LIVE_SETUP
    }

    fun openHub() {
        errorMessage.value = null
        activeStreamId.value = null
        activeStreamSnapshotSeed.value = null
        lastObservedLikeCount = -1
        stopTimer()
        modeState.value = LiveRoomMode.HUB
    }

    fun updateDraftTitle(title: String) {
        if (title.length <= 100) {
            draftTitle.value = title
        }
    }

    fun updateDraftCategory(category: String) {
        draftCategory.value = category
    }

    fun selectFilter(filterPreset: CameraFilterPreset) {
        selectedFilter.value = filterPreset
        val currentId = activeStreamId.value
        if (currentId != null && modeState.value == LiveRoomMode.HOST_BROADCASTING) {
            viewModelScope.launch {
                repository.updateStreamFilter(currentId, filterPreset.id)
            }
        }
    }

    fun startBroadcast(
        hostName: String,
        hostHandle: String,
        visibility: AccountVisibility
    ) {
        if (isStartingStream.value) return
        viewModelScope.launch {
            isStartingStream.value = true
            errorMessage.value = null
            val titleToUse = draftTitle.value.trim().ifBlank {
                "${draftCategory.value} Live Session"
            }
            val result = repository.startLiveStream(
                hostName = hostName,
                hostHandle = hostHandle,
                title = titleToUse,
                category = draftCategory.value,
                filterId = selectedFilter.value.id,
                visibility = visibility
            )
            isStartingStream.value = false
            result.fold(
                onSuccess = { session ->
                    lastObservedLikeCount = session.likeCount
                    activeStreamSnapshotSeed.value = session
                    activeStreamId.value = session.id
                    modeState.value = LiveRoomMode.HOST_BROADCASTING
                    startTimer()
                },
                onFailure = { err ->
                    errorMessage.value = err.localizedMessage ?: "Could not start live broadcast."
                }
            )
        }
    }

    fun joinStreamAsViewer(session: LiveStreamSession) {
        if (session.hostUserId == currentUserId && session.status == "live") {
            lastObservedLikeCount = session.likeCount
            activeStreamSnapshotSeed.value = session
            activeStreamId.value = session.id
            selectedFilter.value = CameraFilterPreset.fromId(session.filterId)
            modeState.value = LiveRoomMode.HOST_BROADCASTING
            startTimer()
            return
        }
        lastObservedLikeCount = session.likeCount
        activeStreamSnapshotSeed.value = session
        activeStreamId.value = session.id
        selectedFilter.value = CameraFilterPreset.fromId(session.filterId)
        modeState.value = LiveRoomMode.VIEWING_STREAM
        startTimer()
        viewModelScope.launch {
            repository.adjustViewerCount(session.id, +1)
        }
    }

    fun endOrLeaveStream(onExitToPrevious: () -> Unit) {
        val currentMode = modeState.value
        val currentId = activeStreamId.value
        val currentSession = uiState.value.activeStream

        when (currentMode) {
            LiveRoomMode.HOST_BROADCASTING -> {
                if (currentId == null) {
                    openHub()
                    return
                }
                viewModelScope.launch {
                    isEndingStream.value = true
                    summaryTitle.value = currentSession?.title ?: draftTitle.value.ifBlank { "Live Session" }
                    summaryLikes.value = currentSession?.likeCount ?: 0
                    summaryDurationSeconds.value = elapsedSeconds.value
                    stopTimer()
                    withContext(NonCancellable) {
                        repository.endLiveStream(currentId)
                    }
                    isEndingStream.value = false
                    activeStreamId.value = null
                    activeStreamSnapshotSeed.value = null
                    lastObservedLikeCount = -1
                    modeState.value = LiveRoomMode.ENDED_SUMMARY
                }
            }
            LiveRoomMode.VIEWING_STREAM -> {
                stopTimer()
                if (currentId != null) {
                    viewModelScope.launch {
                        withContext(NonCancellable) {
                            repository.adjustViewerCount(currentId, -1)
                        }
                    }
                }
                openHub()
            }
            else -> onExitToPrevious()
        }
    }

    fun sendChatMessage(
        senderName: String,
        senderHandle: String,
        text: String
    ) {
        val stream = uiState.value.activeStream ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val result = repository.sendChatMessage(
                stream = stream,
                senderName = senderName,
                senderHandle = senderHandle,
                rawText = trimmed
            )
            result.onFailure { err ->
                errorMessage.value = err.localizedMessage ?: "Failed to send chat message."
            }
        }
    }

    fun sendHeartReaction() {
        val streamId = activeStreamId.value ?: return
        heartPulseTick.update { it + 1L }
        viewModelScope.launch {
            repository.sendHeartReaction(streamId)
        }
    }

    fun dismissError() {
        errorMessage.value = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        elapsedSeconds.value = 0L
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                elapsedSeconds.value += 1L
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    companion object {
        fun provideFactory(
            application: Application,
            currentUserId: String,
            initialStartInSetup: Boolean,
            initialFilterId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LiveStreamViewModel(
                    application = application,
                    currentUserId = currentUserId,
                    initialStartInSetup = initialStartInSetup,
                    initialFilterId = initialFilterId
                ) as T
            }
        }
    }
}
