package com.ahmedprojects.flow

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_members")
data class ProjectMemberEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val projectLocalId: Int,
    val userId: Int,
    val role: String
)
