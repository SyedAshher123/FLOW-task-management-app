package com.ahmedprojects.flow

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_projects")
data class PendingProjectEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val ownerId: Int,
    val name: String,
    val description: String,
    val joinCode: String,
    val picturePath: String = ""    // store file path, not base64
)

