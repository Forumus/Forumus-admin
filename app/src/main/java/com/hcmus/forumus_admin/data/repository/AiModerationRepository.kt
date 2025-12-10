package com.hcmus.forumus_admin.data.repository

import com.hcmus.forumus_admin.data.model.*
import com.hcmus.forumus_admin.data.service.AiModerationService
import com.hcmus.forumus_admin.data.service.MockAiModerationService

/**
 * Repository for managing AI moderation data
 * Acts as a single source of truth for AI moderation operations
 */
class AiModerationRepository(
    private val service: AiModerationService = MockAiModerationService()
) {
    
    /**
     * Analyze a post for content moderation
     */
    suspend fun analyzePost(
        postId: String,
        content: String,
        title: String,
        authorId: String
    ): AiModerationResponse {
        val request = AiModerationRequest(
            postId = postId,
            content = content,
            title = title,
            authorId = authorId
        )
        return service.analyzePost(request)
    }
    
    /**
     * Get all moderation results with pagination
     */
    suspend fun getAllModerationResults(
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AiModerationResult>> {
        return service.getModerationResults(limit, offset)
    }
    
    /**
     * Get approved posts
     */
    suspend fun getApprovedPosts(
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AiModerationResult>> {
        return service.getFilteredResults(
            isApproved = true,
            limit = limit,
            offset = offset
        )
    }
    
    /**
     * Get rejected posts
     */
    suspend fun getRejectedPosts(
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AiModerationResult>> {
        return service.getFilteredResults(
            isApproved = false,
            limit = limit,
            offset = offset
        )
    }
    
    /**
     * Override AI decision manually
     */
    suspend fun overrideModerationDecision(
        postId: String,
        isApproved: Boolean
    ): Result<AiModerationResult> {
        return service.overrideDecision(postId, isApproved)
    }
    
    /**
     * Get sample post information (for mock service)
     */
    fun getSamplePostInfo(postId: String): Triple<String, String, String>? {
        return if (service is MockAiModerationService) {
            service.getSamplePostInfo(postId)
        } else {
            null
        }
    }
    
    /**
     * Search posts by query in title or content
     */
    suspend fun searchModerationResults(
        query: String,
        isApproved: Boolean? = null,
        limit: Int = 50
    ): Result<List<AiModerationResult>> {
        return try {
            // Get all results
            val allResults = if (isApproved != null) {
                service.getFilteredResults(isApproved, limit = 100, offset = 0)
            } else {
                service.getModerationResults(limit = 100, offset = 0)
            }
            
            allResults.map { results ->
                // Filter based on query
                // Note: In a real API, this would be done server-side
                results.filter { result ->
                    val postInfo = getSamplePostInfo(result.postId)
                    if (postInfo != null) {
                        val (_, title, content) = postInfo
                        title.contains(query, ignoreCase = true) ||
                        content.contains(query, ignoreCase = true)
                    } else {
                        true
                    }
                }.take(limit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get moderation statistics
     */
    suspend fun getModerationStats(): Result<ModerationStats> {
        return try {
            val allResults = service.getModerationResults(limit = 1000, offset = 0)
            
            allResults.map { results ->
                val approved = results.count { it.isApproved }
                val rejected = results.count { !it.isApproved }
                val totalViolations = results.sumOf { it.violations.size }
                val averageScore = if (results.isNotEmpty()) {
                    results.map { it.overallScore }.average()
                } else 0.0
                
                ModerationStats(
                    totalPosts = results.size,
                    approvedCount = approved,
                    rejectedCount = rejected,
                    totalViolations = totalViolations,
                    averageScore = averageScore
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Statistics about AI moderation
 */
data class ModerationStats(
    val totalPosts: Int,
    val approvedCount: Int,
    val rejectedCount: Int,
    val totalViolations: Int,
    val averageScore: Double
)
