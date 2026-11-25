package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val TAG = "GeminiChatViewModel"

enum class ChatRole {
    USER,
    MODEL
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val isStreaming: Boolean = false
)

sealed interface GeminiChatUiState {
    data object Idle : GeminiChatUiState
    data object Loading : GeminiChatUiState
    data class Streaming(val partialText: String) : GeminiChatUiState
    data class Success(val lastResponse: String) : GeminiChatUiState
    data class Error(val message: String) : GeminiChatUiState
}

data class GeminiChatScreenState(
    val messages: List<ChatMessage> = emptyList(),
    val uiState: GeminiChatUiState = GeminiChatUiState.Idle,
    val lastFailedPrompt: String? = null
)

class GeminiChatViewModel : ViewModel() {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are Gemini inside ReelPulse, a creative short-form vertical video app. " +
                        "Help users brainstorm viral video hooks, write punchy captions and hashtags, " +
                        "plan shot lists and edits, or chat helpfully about any topic they ask. " +
                        "Keep responses clear, energetic, and easy to read on mobile."
                )
            }
        )
    }

    private var chatSession: Chat? = null

    private val _screenState = MutableStateFlow(GeminiChatScreenState())
    val screenState: StateFlow<GeminiChatScreenState> = _screenState.asStateFlow()

    private fun getOrCreateChat(): Chat {
        return chatSession ?: model.startChat().also { chatSession = it }
    }

    fun sendMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return
        val currentUiState = _screenState.value.uiState
        if (currentUiState is GeminiChatUiState.Loading || currentUiState is GeminiChatUiState.Streaming) {
            return
        }

        val userMessage = ChatMessage(
            role = ChatRole.USER,
            text = prompt
        )
        val assistantMessageId = UUID.randomUUID().toString()

        _screenState.update { state ->
            state.copy(
                messages = state.messages + userMessage + ChatMessage(
                    id = assistantMessageId,
                    role = ChatRole.MODEL,
                    text = "",
                    isStreaming = true
                ),
                uiState = GeminiChatUiState.Loading,
                lastFailedPrompt = null
            )
        }

        streamReply(prompt = prompt, assistantMessageId = assistantMessageId)
    }

    fun retryLastPrompt() {
        val failedPrompt = _screenState.value.lastFailedPrompt ?: return
        val currentUiState = _screenState.value.uiState
        if (currentUiState is GeminiChatUiState.Loading || currentUiState is GeminiChatUiState.Streaming) {
            return
        }

        val assistantMessageId = UUID.randomUUID().toString()
        _screenState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    id = assistantMessageId,
                    role = ChatRole.MODEL,
                    text = "",
                    isStreaming = true
                ),
                uiState = GeminiChatUiState.Loading,
                lastFailedPrompt = null
            )
        }

        streamReply(prompt = failedPrompt, assistantMessageId = assistantMessageId)
    }

    fun dismissError() {
        _screenState.update { state ->
            if (state.uiState is GeminiChatUiState.Error) {
                state.copy(uiState = GeminiChatUiState.Idle)
            } else {
                state
            }
        }
    }

    fun startNewChat() {
        chatSession = null
        _screenState.value = GeminiChatScreenState()
    }

    private fun streamReply(prompt: String, assistantMessageId: String) {
        viewModelScope.launch {
            val accumulatedText = StringBuilder()
            try {
                val chat = getOrCreateChat()
                withTimeout(60_000L) {
                    chat.sendMessageStream(prompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        if (chunkText.isNotEmpty()) {
                            accumulatedText.append(chunkText)
                            val updatedText = accumulatedText.toString()
                            _screenState.update { state ->
                                state.copy(
                                    messages = state.messages.map { msg ->
                                        if (msg.id == assistantMessageId) {
                                            msg.copy(text = updatedText, isStreaming = true)
                                        } else {
                                            msg
                                        }
                                    },
                                    uiState = GeminiChatUiState.Streaming(updatedText)
                                )
                            }
                        }
                    }
                }

                val finalText = accumulatedText.toString().trim()
                if (finalText.isNotEmpty()) {
                    _screenState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(text = finalText, isStreaming = false)
                                } else {
                                    msg
                                }
                            },
                            uiState = GeminiChatUiState.Success(finalText),
                            lastFailedPrompt = null
                        )
                    }
                } else {
                    removeAssistantPlaceholder(
                        assistantMessageId = assistantMessageId,
                        errorMessage = "Gemini returned an empty response. Please try again.",
                        failedPrompt = prompt
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini chat request timed out", e)
                removeAssistantPlaceholder(
                    assistantMessageId = assistantMessageId,
                    errorMessage = "Request timed out after 60 seconds. Check your connection and retry.",
                    failedPrompt = prompt
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini chat inference failed", e)
                removeAssistantPlaceholder(
                    assistantMessageId = assistantMessageId,
                    errorMessage = e.localizedMessage ?: "Failed to get a response from Gemini.",
                    failedPrompt = prompt
                )
            }
        }
    }

    private fun removeAssistantPlaceholder(
        assistantMessageId: String,
        errorMessage: String,
        failedPrompt: String
    ) {
        _screenState.update { state ->
            val cleanedMessages = state.messages.filterNot { msg ->
                msg.id == assistantMessageId && msg.text.isBlank()
            }.map { msg ->
                if (msg.id == assistantMessageId) msg.copy(isStreaming = false) else msg
            }
            state.copy(
                messages = cleanedMessages,
                uiState = GeminiChatUiState.Error(errorMessage),
                lastFailedPrompt = failedPrompt
            )
        }
    }
}
