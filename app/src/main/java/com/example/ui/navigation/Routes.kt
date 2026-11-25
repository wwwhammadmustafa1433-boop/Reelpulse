package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object Feed : AppRoute

    @Serializable
    data object Discover : AppRoute

    @Serializable
    data object Create : AppRoute

    @Serializable
    data object Camera : AppRoute

    @Serializable
    data object Algorithm : AppRoute

    @Serializable
    data object Profile : AppRoute

    @Serializable
    data object GeminiChat : AppRoute

    @Serializable
    data class Live(
        val startInBroadcastSetup: Boolean = false,
        val initialFilterId: String = "normal"
    ) : AppRoute

    @Serializable
    data class CreatorProfile(
        val creatorId: String,
        val creatorName: String,
        val creatorHandle: String,
        val creatorAvatarKey: String
    ) : AppRoute
}
