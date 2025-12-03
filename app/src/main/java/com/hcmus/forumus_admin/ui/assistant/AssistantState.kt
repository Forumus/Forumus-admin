package com.hcmus.forumus_admin.ui.assistant

import com.hcmus.forumus_admin.data.model.Post

data class AiModerationState(
    val posts: List<Post> = emptyList(),
    val filteredPosts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTab: TabType = TabType.AI_APPROVED,
    val searchQuery: String = ""
)

enum class TabType {
    AI_APPROVED,
    AI_REJECTED
}
