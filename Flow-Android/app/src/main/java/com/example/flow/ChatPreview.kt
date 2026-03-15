package com.ahmedprojects.flow.models

data class ChatPreview(
    var userId: String = "",             // partner's userId — important for chat logic
    var username: String = "",           // partner's username (e.g. "laiq_34")
    var displayName: String = "",        // partner's full name (e.g. "Ahmed Laiq")
    var lastMessage: String = "",        // last message text
    var profileImageBase64: String = "", // profile picture in Base64
    var lastMessageTime: String = ""     // e.g., "5m", "2h"
)
