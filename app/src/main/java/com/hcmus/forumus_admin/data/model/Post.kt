package com.hcmus.forumus_admin.data.model

data class Post(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val date: String = "",
    val description: String = "",
    val tags: List<Tag> = emptyList(),
    val isAiApproved: Boolean = true
)

enum class PostStatus {
    APPROVED,
    REJECTED,
    PENDING,
    DELETED
}
