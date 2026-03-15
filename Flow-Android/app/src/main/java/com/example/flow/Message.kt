package com.ahmedprojects.flow.models

data class Message(
    var senderId: String,
    var receiverId: String,
    var messageText: String,
    var imageUrl: String,
    var timestamp: Long,
    var type: String,
    var messageId: String?,
    var isVanish: Boolean
)
