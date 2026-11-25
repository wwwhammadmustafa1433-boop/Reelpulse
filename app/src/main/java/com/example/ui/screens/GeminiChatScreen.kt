package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCrimsonDark
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseOutline
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceCard
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.ChatRole
import com.example.ui.viewmodel.GeminiChatScreenState
import com.example.ui.viewmodel.GeminiChatUiState

private data class StarterPrompt(
    val title: String,
    val prompt: String
)

private val geminiStarterPrompts = listOf(
    StarterPrompt(
        title = "Viral 15s Hook",
        prompt = "Give me 3 scroll-stopping 15-second video hooks for a short creator vlog."
    ),
    StarterPrompt(
        title = "Caption & Hashtags",
        prompt = "Write 3 catchy short-form video captions with 5 trending hashtags each."
    ),
    StarterPrompt(
        title = "Edit & Shot Ideas",
        prompt = "Suggest a dynamic 4-shot sequence with camera transitions for a nighttime street clip."
    ),
    StarterPrompt(
        title = "Ask Anything",
        prompt = "What are 5 quick ways to make everyday short videos look more cinematic?"
    )
)

@Composable
fun GeminiChatScreen(
    state: GeminiChatScreenState,
    onSendMessage: (String) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onNewChat: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isBusy = state.uiState is GeminiChatUiState.Loading ||
        state.uiState is GeminiChatUiState.Streaming

    // Auto-scroll smoothly as new messages or tokens stream in
    val lastMessageLength = state.messages.lastOrNull()?.text?.length ?: 0
    LaunchedEffect(state.messages.size, lastMessageLength) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    fun submitCurrentInput() {
        val trimmed = inputText.trim()
        if (trimmed.isNotEmpty() && !isBusy) {
            onSendMessage(trimmed)
            inputText = ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top App Bar
            Surface(
                color = PulseSurface.copy(alpha = 0.92f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("gemini_chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PulseTextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(PulseCyan, PulseCrimson))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.gemini_chat_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PulseTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.gemini_chat_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (state.messages.isNotEmpty()) {
                        IconButton(
                            onClick = onNewChat,
                            enabled = !isBusy,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("gemini_new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddComment,
                                contentDescription = stringResource(R.string.gemini_chat_new),
                                tint = if (isBusy) PulseTextSecondary else PulseTextPrimary
                            )
                        }
                    }
                }
            }

            // Messages or Welcome Starter View
            if (state.messages.isEmpty()) {
                GeminiWelcomeEmptyState(
                    onPromptSelected = { prompt ->
                        onSendMessage(prompt)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("gemini_message_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        ChatMessageBubble(message = message)
                    }
                }
            }

            // Error Banner (if inference fails or times out)
            val errorState = state.uiState as? GeminiChatUiState.Error
            AnimatedVisibility(visible = errorState != null) {
                errorState?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PulseCrimson.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .border(1.dp, PulseCrimson.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .testTag("gemini_error_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = PulseCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = err.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (state.lastFailedPrompt != null) {
                                IconButton(
                                    onClick = onRetry,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("gemini_retry_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Retry",
                                        tint = PulseCyan
                                    )
                                }
                            }
                            TextButton(onClick = onDismissError) {
                                Text("Dismiss", color = PulseTextSecondary)
                            }
                        }
                    }
                }
            }

            // Bottom Message Composer
            Surface(
                color = PulseSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.gemini_chat_placeholder),
                                color = PulseTextSecondary
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { submitCurrentInput() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PulseSurfaceElevated,
                            unfocusedContainerColor = PulseSurfaceElevated,
                            focusedBorderColor = PulseCyan,
                            unfocusedBorderColor = PulseOutline,
                            focusedTextColor = PulseTextPrimary,
                            unfocusedTextColor = PulseTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_chat_input")
                    )

                    val canSend = inputText.isNotBlank() && !isBusy
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) {
                                    Brush.linearGradient(listOf(PulseCrimson, PulseCyan))
                                } else {
                                    Brush.linearGradient(listOf(PulseSurfaceCard, PulseSurfaceCard))
                                }
                            )
                            .clickable(enabled = canSend) { submitCurrentInput() }
                            .testTag("gemini_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                color = PulseCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = if (canSend) Color.White else PulseTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiWelcomeEmptyState(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(PulseCyan.copy(alpha = 0.28f), PulseCrimson.copy(alpha = 0.28f))
                    )
                )
                .border(1.5.dp, PulseCyan.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = PulseCyan,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Chat with Gemini",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PulseTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Brainstorm viral video hooks, captions, shot ideas, or ask anything you want.",
            style = MaterialTheme.typography.bodyMedium,
            color = PulseTextSecondary
        )

        Spacer(modifier = Modifier.height(22.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            geminiStarterPrompts.forEach { starter ->
                Surface(
                    onClick = { onPromptSelected(starter.prompt) },
                    shape = RoundedCornerShape(16.dp),
                    color = PulseSurfaceElevated,
                    modifier = Modifier
                        .border(1.dp, PulseOutline, RoundedCornerShape(16.dp))
                        .testTag("gemini_starter_${starter.title.lowercase().replace(" ", "_")}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = PulseCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = starter.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = PulseTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    val clipboardManager = LocalClipboardManager.current
    var copiedFeedback by remember(message.id) { mutableStateOf(false) }

    LaunchedEffect(copiedFeedback) {
        if (copiedFeedback) {
            kotlinx.coroutines.delay(1400L)
            copiedFeedback = false
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Box(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(PulseCrimson, PulseCrimsonDark)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
                color = PulseSurfaceElevated,
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .border(
                        width = 1.dp,
                        color = PulseCyan.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = PulseCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Gemini",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PulseCyan
                            )
                        }

                        if (!message.isStreaming && message.text.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(message.text))
                                        copiedFeedback = true
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy response",
                                    tint = if (copiedFeedback) PulseCyan else PulseTextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (copiedFeedback) "Copied" else "Copy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (copiedFeedback) PulseCyan else PulseTextSecondary
                                )
                            }
                        }
                    }

                    if (message.text.isEmpty() && message.isStreaming) {
                        ThinkingPulseIndicator()
                    } else {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PulseTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingPulseIndicator() {
    val transition = rememberInfiniteTransition(label = "gemini_thinking")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinking_alpha"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.alpha(alpha)
    ) {
        CircularProgressIndicator(
            color = PulseCyan,
            strokeWidth = 2.dp,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "Gemini is thinking...",
            style = MaterialTheme.typography.bodySmall,
            color = PulseTextSecondary
        )
    }
}
