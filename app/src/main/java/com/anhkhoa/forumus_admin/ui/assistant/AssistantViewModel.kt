package com.anhkhoa.forumus_admin.ui.assistant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.anhkhoa.forumus_admin.R
import com.anhkhoa.forumus_admin.data.model.Post
import com.anhkhoa.forumus_admin.data.model.Tag

class AiModerationViewModel : ViewModel() {
    
    private val _state = MutableLiveData(AiModerationState())
    val state: LiveData<AiModerationState> = _state
    
    init {
        loadPosts()
    }
    
    private fun loadPosts() {
        // TODO: Replace with actual data from repository/database
        val samplePosts = getSamplePosts()
        _state.value = _state.value?.copy(
            posts = samplePosts,
            filteredPosts = samplePosts.filter { it.isAiApproved }
        )
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
        // TODO: Implement API call to approve post
        val currentState = _state.value ?: return
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
    }
    
    fun rejectPost(postId: String) {
        // TODO: Implement API call to reject post
        val currentState = _state.value ?: return
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
    }
    
    private fun getSamplePosts(): List<Post> {
        return listOf(
            Post(
                id = "1",
                title = "Getting Started with React Hooks",
                author = "Sarah Johnson",
                date = "November 17, 2025",
                description = "A comprehensive guide to understanding and implementing React Hooks in your applications. Learn about useState, useEffect, and custom hooks.",
                tags = listOf(
                    Tag("Programming", R.color.tag_programming_bg, R.color.tag_programming_text),
                    Tag("React", R.color.tag_react_bg, R.color.tag_react_text)
                ),
                isAiApproved = true
            ),
            Post(
                id = "2",
                title = "Best Practices for TypeScript",
                author = "Michael Chen",
                date = "November 16, 2025",
                description = "Explore the best practices for writing clean, maintainable TypeScript code. Cover type safety, interfaces, and advanced patterns.",
                tags = listOf(
                    Tag("Programming", R.color.tag_programming_bg, R.color.tag_programming_text),
                    Tag("TypeScript", R.color.tag_typescript_bg, R.color.tag_typescript_text)
                ),
                isAiApproved = true
            ),
            Post(
                id = "3",
                title = "Modern CSS Techniques",
                author = "Emma Davis",
                date = "November 16, 2025",
                description = "Discover the latest CSS techniques including Grid, Flexbox, and custom properties to create responsive layouts.",
                tags = listOf(
                    Tag("Design", R.color.tag_design_bg, R.color.tag_design_text),
                    Tag("CSS", R.color.tag_css_bg, R.color.tag_css_text)
                ),
                isAiApproved = true
            )
        )
    }
}
