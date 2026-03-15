package com.ahmedprojects.flow

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val assignedBy: Int,
    val assignedTo: Int,
    var assignedByName: String,
    val organisationName: String,
    val updateRequested: Boolean,
    val percentageCompleted: Int,
    val dueDate: String
)

