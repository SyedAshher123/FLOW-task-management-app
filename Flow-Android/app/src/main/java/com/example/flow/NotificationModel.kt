package com.ahmedprojects.flow

data class NotificationModel(
    val inviteId: Int,
    val projectId: Int,
    val senderId: Int,
    val receiverId: Int,
    val senderName: String,
    val projectName: String,
    val timestamp: String
)


