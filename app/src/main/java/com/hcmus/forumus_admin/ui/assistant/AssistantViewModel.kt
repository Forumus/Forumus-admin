package com.hcmus.forumus_admin.ui.assistant

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.model.AiModerationResult
import com.hcmus.forumus_admin.data.model.Post
import com.hcmus.forumus_admin.data.model.Tag
import com.hcmus.forumus_admin.data.model.ViolationCategory
import com.hcmus.forumus_admin.data.repository.AiModerationRepository
import kotlinx.coroutines.launch

class AiModerationViewModel : ViewModel() {
    
    private val _state = MutableLiveData(AiModerationState())
    val state: LiveData<AiModerationState> = _state
    
    private val repository = AiModerationRepository()

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
                filteredPosts = applyFiltersAndSort(posts, currentState.searchQuery, currentState.sortOrder, currentState.selectedViolationTypes),
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
                filteredPosts = applyFiltersAndSort(posts, currentState.searchQuery, currentState.sortOrder, currentState.selectedViolationTypes),
                isLoading = false
            )
        }
    }
    
    fun searchPosts(query: String) {
        val currentState = _state.value ?: return
        
        _state.value = currentState.copy(
            searchQuery = query,
            filteredPosts = applyFiltersAndSort(currentState.allPosts, query, currentState.sortOrder, currentState.selectedViolationTypes)
        )
    }
    
    fun setSortOrder(order: SortOrder) {
        val currentState = _state.value ?: return
        
        _state.value = currentState.copy(
            sortOrder = order,
            filteredPosts = applyFiltersAndSort(currentState.allPosts, currentState.searchQuery, order, currentState.selectedViolationTypes)
        )
    }
    
    fun setViolationFilter(violationTypes: Set<ViolationCategory>) {
        val currentState = _state.value ?: return
        
        _state.value = currentState.copy(
            selectedViolationTypes = violationTypes,
            filteredPosts = applyFiltersAndSort(currentState.allPosts, currentState.searchQuery, currentState.sortOrder, violationTypes)
        )
    }
    
    private fun applyFiltersAndSort(
        posts: List<AiModerationResult>,
        searchQuery: String,
        sortOrder: SortOrder,
        violationTypes: Set<ViolationCategory>
    ): List<AiModerationResult> {
        var result = posts
        
        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            result = result.filter { post ->
                post.postData.title.contains(searchQuery, ignoreCase = true) ||
                post.postData.content.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // Apply violation type filter
        if (violationTypes.isNotEmpty()) {
            result = result.filter { post ->
                post.violations.any { violation -> violation.type in violationTypes }
            }
        }
        
        // Apply sort
        result = when (sortOrder) {
            SortOrder.NEWEST_FIRST -> result.sortedByDescending { it.postData.createdAt?.seconds ?: 0L }
            SortOrder.OLDEST_FIRST -> result.sortedBy { it.postData.createdAt?.seconds ?: 0L }
        }
        
        return result
    }
    
    fun approvePost(postId: String) {
        viewModelScope.launch {
            val currentState = _state.value ?: return@launch
            
            // Show loading
            _state.value = currentState.copy(isLoading = true)
            
            try {
                // Call repository to override decision
                val result = repository.overrideModerationDecision(postId, true)
                
                result.onSuccess {
                    Log.d("AiModerationViewModel", "Approved post: $postId")
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
    
    fun rejectPost(postId: String) {
        viewModelScope.launch {
            val currentState = _state.value ?: return@launch
            
            // Show loading
            _state.value = currentState.copy(isLoading = true)
            
            try {
                // Call repository to override decision
                val result = repository.overrideModerationDecision(postId, false)
                
                result.onSuccess {
                    Log.d("AiModerationViewModel", "Rejected post: $postId")
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
}

