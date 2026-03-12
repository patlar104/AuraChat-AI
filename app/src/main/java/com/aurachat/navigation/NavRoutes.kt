package com.aurachat.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class ChatRoute(val sessionId: Long)

@Serializable
object SettingsRoute
