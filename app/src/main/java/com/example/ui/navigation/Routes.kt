package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Feed : AppRoute

    @Serializable
    data object Discover : AppRoute

    @Serializable
    data object Create : AppRoute

    @Serializable
    data object Algorithm : AppRoute

    @Serializable
    data object Profile : AppRoute
}
