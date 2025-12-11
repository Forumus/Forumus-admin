package com.hcmus.forumus_admin.ui.assistant

import android.util.Log
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
        loadPosts(true)
    }

    private fun loadPosts(isApproved: Boolean = true) {
        viewModelScope.launch {
            // Fetch approved posts by default
            val posts = if (isApproved) {
                repository.getApprovedPosts()
            } else {
                repository.getRejectedPosts()
            }.getOrDefault(emptyList())
            _state.value = _state.value?.copy(
                filteredPosts = posts
            )
        }
    }

    fun selectTab(tab: TabType) {
        val currentState = _state.value ?: return

        viewModelScope.launch {
            val filteredPosts = if (tab == TabType.AI_APPROVED) {
                repository.getApprovedPosts()
            } else {
                repository.getRejectedPosts()
            }.getOrDefault(emptyList())

            _state.value = currentState.copy(
                currentTab = tab,
                filteredPosts = filteredPosts
            )
        }

    }
    
    fun searchPosts(query: String) {
        val currentState = _state.value ?: return

        viewModelScope.launch {
            val filteredPosts = repository.searchModerationResults(query).getOrDefault(emptyList())

            _state.value = currentState.copy(
                searchQuery = query,
                filteredPosts = filteredPosts
            )
        }
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
