package com.ahmedprojects.flow

data class TaskModel(
    val id: Int,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val assignedBy: Int,
    var assignedByName: String,
    val organisationName: String,
    val updateRequested: Boolean,
    val percentageCompleted: Int,   // NEW
    val dueDate: String             // NEW
)


