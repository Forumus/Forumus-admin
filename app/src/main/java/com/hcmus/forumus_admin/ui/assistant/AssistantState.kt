package com.hcmus.forumus_admin.ui.assistant

import com.hcmus.forumus_admin.data.model.AiModerationResult
import com.hcmus.forumus_admin.data.model.Post
import com.hcmus.forumus_admin.data.model.ViolationCategory

data class AiModerationState(
    val filteredPosts: List<AiModerationResult> = emptyList(),
    val allPosts: List<AiModerationResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTab: TabType = TabType.AI_APPROVED,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val selectedViolationTypes: Set<ViolationCategory> = emptySet()
)

enum class TabType {
    AI_APPROVED,
    AI_REJECTED
}

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}
