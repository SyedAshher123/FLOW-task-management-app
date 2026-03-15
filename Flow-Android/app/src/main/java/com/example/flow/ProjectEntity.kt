package com.ahmedprojects.flow

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val ownerId: Int,
    val joinCode: String,
    val role: String,
    val membersCount: Int,
    val picturePath: String? = null
)

