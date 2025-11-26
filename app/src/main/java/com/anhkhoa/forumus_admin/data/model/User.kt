package com.anhkhoa.forumus_admin.data.model

data class User(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val status: UserStatus
)

enum class UserStatus {
    BAN,
    WARNING,
    REMIND,
    NORMAL
}
