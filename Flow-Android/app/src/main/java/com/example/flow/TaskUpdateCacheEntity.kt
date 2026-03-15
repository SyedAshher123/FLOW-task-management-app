package com.ahmedprojects.flow

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_update_cache")
data class TaskUpdateCacheEntity(
    @PrimaryKey val id: Int,
    val taskId: Int,
    val userName: String,
    val message: String,
    val imageUrl: String,
    val createdAt: String
)