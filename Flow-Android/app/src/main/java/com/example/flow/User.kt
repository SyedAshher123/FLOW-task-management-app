package com.ahmedprojects.flow

data class User(
    var id: Int = 0,
    var name: String = "",
    var email: String = "",
    var profilePhoto: String? = null // base64 or URL
)
