package com.hcmus.forumus_admin.ui.assistant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.model.Post
import com.hcmus.forumus_admin.data.model.Tag
import com.hcmus.forumus_admin.data.repository.AiModerationRepository
import kotlinx.coroutines.launch

class AiModerationViewModel : ViewModel() {
    
    private val _state = MutableLiveData(AiModerationState())
    val state: LiveData<AiModerationState> = _state
    
    private val repository = AiModerationRepository()
    
    init {
        loadPosts()
    }
    
    private fun loadPosts() {
        viewModelScope.launch {
            _state.value = _state.value?.copy(isLoading = true)
            
            try {
                // Get moderation results from repository
                val result = repository.getAllModerationResults()
                
                result.onSuccess { moderationResults ->
                    // Convert moderation results to Post objects with sample data
                    val posts = moderationResults.mapNotNull { moderationResult ->
                        val postInfo = repository.getSamplePostInfo(moderationResult.postId)
                        if (postInfo != null) {
                            val (id, title, content) = postInfo
                            Post(
                                id = id,
                                title = title,
                                author = "Sample Author",
                                date = formatDate(moderationResult.analyzedAt),
                                description = content,
                                tags = getTagsForPost(id),
                                isAiApproved = moderationResult.isApproved
                            )
                        } else {
                            null
                        }
                    }
                    
                    _state.value = _state.value?.copy(
                        posts = posts,
                        filteredPosts = posts.filter { it.isAiApproved },
                        isLoading = false
                    )
                }.onFailure { error ->
                    _state.value = _state.value?.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value?.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        return format.format(date)
    }
    
    private fun getTagsForPost(postId: String): List<Tag> {
        // Map post IDs to appropriate tags
        return when (postId) {
            "1", "4" -> listOf(
                Tag("Programming", R.color.tag_programming_bg, R.color.tag_programming_text),
                Tag("React", R.color.tag_react_bg, R.color.tag_react_text)
            )
            "2", "5" -> listOf(
                Tag("Programming", R.color.tag_programming_bg, R.color.tag_programming_text),
                Tag("TypeScript", R.color.tag_typescript_bg, R.color.tag_typescript_text)
            )
            "3", "6" -> listOf(
                Tag("Design", R.color.tag_design_bg, R.color.tag_design_text),
                Tag("CSS", R.color.tag_css_bg, R.color.tag_css_text)
            )
            "7" -> listOf(
                Tag("Programming", R.color.tag_programming_bg, R.color.tag_programming_text)
            )
            "8" -> listOf(
                Tag("Programming", R.color.tag_programming_bg, R.color.tag_programming_text),
                Tag("Design", R.color.tag_design_bg, R.color.tag_design_text)
            )
            else -> listOf(
                Tag("Programming", R.color.tag_programming_bg, R.color.tag_programming_text)
            )
        }
    }
    
    fun selectTab(tab: TabType) {
        val currentState = _state.value ?: return
        val filteredPosts = currentState.posts.filter {
            when (tab) {
                TabType.AI_APPROVED -> it.isAiApproved
                TabType.AI_REJECTED -> !it.isAiApproved
            }
        }.filter { post ->
            if (currentState.searchQuery.isEmpty()) {
                true
            } else {
                post.title.contains(currentState.searchQuery, ignoreCase = true) ||
                        post.description.contains(currentState.searchQuery, ignoreCase = true)
            }
        }
        
        _state.value = currentState.copy(
            currentTab = tab,
            filteredPosts = filteredPosts
        )
    }
    
    fun searchPosts(query: String) {
        val currentState = _state.value ?: return
        val filteredPosts = currentState.posts.filter {
            when (currentState.currentTab) {
                TabType.AI_APPROVED -> it.isAiApproved
                TabType.AI_REJECTED -> !it.isAiApproved
            }
        }.filter { post ->
            if (query.isEmpty()) {
                true
            } else {
                post.title.contains(query, ignoreCase = true) ||
                        post.description.contains(query, ignoreCase = true) ||
                        post.author.contains(query, ignoreCase = true)
            }
        }
        
        _state.value = currentState.copy(
            searchQuery = query,
            filteredPosts = filteredPosts
        )
    }
    
    fun approvePost(postId: String) {
        viewModelScope.launch {
            val currentState = _state.value ?: return@launch
            
            try {
                // Call repository to override decision
                val result = repository.overrideModerationDecision(postId, true)
                
                result.onSuccess {
                    // Update local state
                    val updatedPosts = currentState.posts.map {
                        if (it.id == postId) it.copy(isAiApproved = true) else it
                    }
                    
                    _state.value = currentState.copy(
                        posts = updatedPosts,
                        filteredPosts = updatedPosts.filter {
                            when (currentState.currentTab) {
                                TabType.AI_APPROVED -> it.isAiApproved
                                TabType.AI_REJECTED -> !it.isAiApproved
                            }
                        }
                    )
                }.onFailure { error ->
                    _state.value = currentState.copy(error = error.message)
                }
            } catch (e: Exception) {
                _state.value = currentState.copy(error = e.message)
            }
        }
    }
    
    fun rejectPost(postId: String) {
        viewModelScope.launch {
            val currentState = _state.value ?: return@launch
            
            try {
                // Call repository to override decision
                val result = repository.overrideModerationDecision(postId, false)
                
                result.onSuccess {
                    // Update local state
                    val updatedPosts = currentState.posts.map {
                        if (it.id == postId) it.copy(isAiApproved = false) else it
                    }
                    
                    _state.value = currentState.copy(
                        posts = updatedPosts,
                        filteredPosts = updatedPosts.filter {
                            when (currentState.currentTab) {
                                TabType.AI_APPROVED -> it.isAiApproved
                                TabType.AI_REJECTED -> !it.isAiApproved
                            }
                        }
                    )
                }.onFailure { error ->
                    _state.value = currentState.copy(error = error.message)
                }
            } catch (e: Exception) {
                _state.value = currentState.copy(error = e.message)
            }
        }
    }
}
