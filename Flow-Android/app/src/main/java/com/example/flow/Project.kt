package com.ahmedprojects.flow

data class Project(
    val id: Int,
    val name: String,
    val description: String,
    val role: String,
    val membersCount: Int,
    val pictureUrl: String? = null // add this
)

