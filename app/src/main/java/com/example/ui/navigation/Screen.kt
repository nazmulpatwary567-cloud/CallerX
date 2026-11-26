package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Dialer : Screen
    @Serializable data object History : Screen
    @Serializable data object Contacts : Screen
    @Serializable data object Settings : Screen

    @Serializable data class ContactDetail(val contactId: Long) : Screen
    @Serializable data object SmsInbox : Screen
    @Serializable data class Conversation(val threadId: Long, val address: String) : Screen
    @Serializable data object Recordings : Screen
    @Serializable data class Player(val recordingId: Long) : Screen
    @Serializable data object SipAccounts : Screen
    @Serializable data object BlockList : Screen
    @Serializable data object PinSetup : Screen
    @Serializable data object CallScreenRoute : Screen
    @Serializable data object IncomingCallRoute : Screen
}
