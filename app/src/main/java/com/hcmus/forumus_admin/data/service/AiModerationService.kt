package com.hcmus.forumus_admin.data.service

import com.hcmus.forumus_admin.data.model.AiModerationRequest
import com.hcmus.forumus_admin.data.model.AiModerationResponse
import com.hcmus.forumus_admin.data.model.AiModerationResult

/**
 * Service interface for AI moderation API
 * This will be implemented by both mock service and real API service
 */
interface AiModerationService {
    
    /**
     * Analyze a post for content moderation
     * @param request The moderation request containing post details
     * @return Response containing moderation results
     */
    suspend fun analyzePost(request: AiModerationRequest): AiModerationResponse
    
    /**
     * Get all posts that have been analyzed by AI
     * @param limit Maximum number of results to return
     * @param offset Pagination offset
     * @return List of moderation results
     */
    suspend fun getModerationResults(
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AiModerationResult>>
    
    /**
     * Get moderation results filtered by approval status
     * @param isApproved Filter by approval status (true = approved, false = rejected)
     * @param limit Maximum number of results to return
     * @param offset Pagination offset
     * @return List of filtered moderation results
     */
    suspend fun getFilteredResults(
        isApproved: Boolean,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AiModerationResult>>
    
    /**
     * Manually override AI moderation decision
     * @param postId The post ID to update
     * @param isApproved New approval status
     * @return Updated moderation result
     */
    suspend fun overrideDecision(
        postId: String,
        isApproved: Boolean
    ): Result<AiModerationResult>
}
