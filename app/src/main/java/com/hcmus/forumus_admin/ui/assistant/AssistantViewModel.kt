package com.hcmus.forumus_admin.ui.assistant

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.model.AiModerationResult
import com.hcmus.forumus_admin.data.model.FirestorePost
import com.hcmus.forumus_admin.data.model.Post
import com.hcmus.forumus_admin.data.model.StatusEscalationResult
import com.hcmus.forumus_admin.data.model.Tag
import com.hcmus.forumus_admin.data.repository.AiModerationRepository
import com.hcmus.forumus_admin.data.service.PushNotificationService
import kotlinx.coroutines.launch

class AiModerationViewModel : ViewModel() {
    
    private val _state = MutableLiveData(AiModerationState())
    val state: LiveData<AiModerationState> = _state
    
    // LiveData for status escalation events (for UI notification)
    private val _statusEscalationEvent = MutableLiveData<StatusEscalationResult?>()
    val statusEscalationEvent: LiveData<StatusEscalationResult?> = _statusEscalationEvent
    
    private val repository = AiModerationRepository()
    private val pushNotificationService = PushNotificationService.getInstance()

    init {
        loadPosts(true)
    }

    private fun loadPosts(isApproved: Boolean = true) {
        viewModelScope.launch {
            val currentState = _state.value ?: AiModerationState()
            _state.value = currentState.copy(isLoading = true)
            
            // Fetch approved posts by default
            val posts = if (isApproved) {
                repository.getApprovedPosts()
            } else {
                repository.getRejectedPosts()
            }.getOrDefault(emptyList())
            
            _state.value = currentState.copy(
                allPosts = posts,
                filteredPosts = applyFiltersAndSort(posts, currentState.searchQuery, currentState.sortOrder, currentState.selectedViolationIds),
                isLoading = false
            )
        }
    }

    fun selectTab(tab: TabType) {
        val currentState = _state.value ?: return

        viewModelScope.launch {
            _state.value = currentState.copy(isLoading = true)
            
            val posts = if (tab == TabType.AI_APPROVED) {
                repository.getApprovedPosts()
            } else {
                repository.getRejectedPosts()
            }.getOrDefault(emptyList())

            _state.value = currentState.copy(
                currentTab = tab,
                allPosts = posts,
                filteredPosts = applyFiltersAndSort(posts, currentState.searchQuery, currentState.sortOrder, currentState.selectedViolationIds),
                isLoading = false
            )
        }
    }
    
    fun searchPosts(query: String) {
        val currentState = _state.value ?: return
        
        _state.value = currentState.copy(
            searchQuery = query,
            filteredPosts = applyFiltersAndSort(currentState.allPosts, query, currentState.sortOrder, currentState.selectedViolationIds)
        )
    }
    
    fun setSortOrder(order: SortOrder) {
        val currentState = _state.value ?: return
        
        _state.value = currentState.copy(
            sortOrder = order,
            filteredPosts = applyFiltersAndSort(currentState.allPosts, currentState.searchQuery, order, currentState.selectedViolationIds)
        )
    }
    
    fun setViolationFilter(violationIds: Set<String>) {
        val currentState = _state.value ?: return
        
        _state.value = currentState.copy(
            selectedViolationIds = violationIds,
            filteredPosts = applyFiltersAndSort(currentState.allPosts, currentState.searchQuery, currentState.sortOrder, violationIds)
        )
    }
    
    private fun applyFiltersAndSort(
        posts: List<AiModerationResult>,
        searchQuery: String,
        sortOrder: SortOrder,
        violationIds: Set<String>
    ): List<AiModerationResult> {
        var result = posts
        
        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            result = result.filter { post ->
                post.postData.title.contains(searchQuery, ignoreCase = true) ||
                post.postData.content.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // Apply violation type filter - check if post's violationTypes array contains any of the selected IDs
        if (violationIds.isNotEmpty()) {
            result = result.filter { post ->
                post.postData.violationTypes.any { violationType -> violationType in violationIds }
            }
        }
        
        // Apply sort
        result = when (sortOrder) {
            SortOrder.NEWEST_FIRST -> result.sortedByDescending { 
                it.postData.createdAt?.seconds ?: 0L
            }
            SortOrder.OLDEST_FIRST -> result.sortedBy { 
                it.postData.createdAt?.seconds ?: 0L
            }
        }
        
        return result
    }
    
    fun approvePost(postId: String) {
        viewModelScope.launch {
            val currentState = _state.value ?: return@launch
            
            // Show loading
            _state.value = currentState.copy(isLoading = true)
            
            try {
                // Call repository to override decision (approval doesn't trigger escalation)
                val result = repository.overrideModerationDecision(
                    postId = postId,
                    isApproved = true
                )
                
                result.onSuccess {
                    Log.d("AiModerationViewModel", "Approved post: $postId")
                    
                    // Send push notification about post approval
                    val post = currentState.allPosts.find { it.postData.id == postId }?.postData
                    if (post != null) {
                        try {
                            pushNotificationService.sendPostApprovedNotification(
                                postId = post.id,
                                postAuthorId = post.authorId,
                                postTitle = post.title,
                                isAiApproved = false // Admin approved
                            )
                        } catch (e: Exception) {
                            Log.w("AiModerationViewModel", "Failed to send notification (non-blocking)", e)
                        }
                    }
                    
                    // Reload posts after approval
                    loadPosts(currentState.currentTab == TabType.AI_APPROVED)
                }.onFailure { error ->
                    _state.value = currentState.copy(error = error.message, isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = currentState.copy(error = e.message, isLoading = false)
            }
        }
    }
    
    /**
     * Reject a post and escalate the author's status.
     * This will trigger the status escalation workflow.
     */
    fun rejectPost(postId: String, sendNotification: Boolean = true) {
        viewModelScope.launch {
            val currentState = _state.value ?: return@launch
            
            // Find the post to get author information
            val post = currentState.allPosts.find { it.postData.id == postId }?.postData
            
            // Show loading
            _state.value = currentState.copy(isLoading = true)
            
            try {
                // Call repository to override decision with author info for escalation
                val result = if (post != null) {
                    repository.overrideModerationDecisionWithPost(post, false)
                } else {
                    // Fallback if post not found in current state
                    repository.overrideModerationDecision(postId, false)
                }
                
                result.onSuccess { decisionResult ->
                    Log.d("AiModerationViewModel", "Rejected post: $postId")
                    
                    // Notify UI about status escalation if it happened
                    decisionResult.escalationResult?.let { escalation ->
                        if (escalation.wasEscalated) {
                            Log.d("AiModerationViewModel", 
                                "User status escalated: ${escalation.previousStatus.value} -> ${escalation.newStatus.value}")
                            _statusEscalationEvent.value = escalation
                        }
                    }
                    
                    // Send push notification about post rejection
                    if (sendNotification && post != null) {
                        try {
                            pushNotificationService.sendPostRejectedNotification(
                                postId = post.id,
                                postAuthorId = post.authorId,
                                postTitle = post.title,
                                postContent = post.content,
                                isAiRejected = false // Admin rejected
                            )
                        } catch (e: Exception) {
                            Log.w("AiModerationViewModel", "Failed to send notification (non-blocking)", e)
                        }
                    }

                    // Reload posts after rejection
                    loadPosts(currentState.currentTab == TabType.AI_APPROVED)
                }.onFailure { error ->
                    _state.value = currentState.copy(error = error.message, isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = currentState.copy(error = e.message, isLoading = false)
            }
        }
    }
    
    /**
     * Reject a post with full post data (preferred method).
     * This ensures proper status escalation with all author information.
     */
    fun rejectPostWithData(post: FirestorePost) {
        viewModelScope.launch {
            val currentState = _state.value ?: return@launch
            
            // Show loading
            _state.value = currentState.copy(isLoading = true)
            
            try {
                val result = repository.overrideModerationDecisionWithPost(post, false)
                
                result.onSuccess { decisionResult ->
                    Log.d("AiModerationViewModel", "Rejected post: ${post.id}")
                    
                    // Notify UI about status escalation if it happened
                    decisionResult.escalationResult?.let { escalation ->
                        if (escalation.wasEscalated) {
                            Log.d("AiModerationViewModel", 
                                "User status escalated: ${escalation.previousStatus.value} -> ${escalation.newStatus.value}")
                            _statusEscalationEvent.value = escalation
                        }
                    }
                    
                    // Send push notification about post rejection
                    try {
                        pushNotificationService.sendPostRejectedNotification(
                            postId = post.id,
                            postAuthorId = post.authorId,
                            postTitle = post.title,
                            postContent = post.content,
                            isAiRejected = false // Admin rejected
                        )
                    } catch (e: Exception) {
                        Log.w("AiModerationViewModel", "Failed to send notification (non-blocking)", e)
                    }

                    // Reload posts after rejection
                    loadPosts(currentState.currentTab == TabType.AI_APPROVED)
                }.onFailure { error ->
                    _state.value = currentState.copy(error = error.message, isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = currentState.copy(error = e.message, isLoading = false)
            }
        }
    }
    
    /**
     * Clear the status escalation event after it's been handled by the UI.
     */
    fun clearStatusEscalationEvent() {
        _statusEscalationEvent.value = null
    }
}

