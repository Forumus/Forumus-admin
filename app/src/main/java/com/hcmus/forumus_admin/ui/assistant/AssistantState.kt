package com.hcmus.forumus_admin.ui.assistant

import com.hcmus.forumus_admin.data.model.AiModerationResult
import com.hcmus.forumus_admin.data.model.Post

data class AiModerationState(
    val filteredPosts: List<AiModerationResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTab: TabType = TabType.AI_APPROVED,
    val searchQuery: String = ""
)

enum class TabType {
    AI_APPROVED,
    AI_REJECTED
}
