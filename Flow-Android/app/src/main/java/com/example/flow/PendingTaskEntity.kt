package com.ahmedprojects.flow

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_tasks")
data class PendingTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val assignedTo: Int,
    val assignedBy: Int,
    val title: String,
    val description: String,
    val priority: String,
    val deadline: String
)
