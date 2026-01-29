package com.hcmus.forumus_admin.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_admin.data.model.*
import com.hcmus.forumus_admin.data.model.FirestorePost
import com.hcmus.forumus_admin.data.service.AiModerationService
import com.hcmus.forumus_admin.data.service.UserStatusEscalationService
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing AI moderation data
 * Acts as a single source of truth for AI moderation operations
 */
class AiModerationRepository(
//    private val service: AiModerationService = AiModerationService()
    private val statusEscalationService: UserStatusEscalationService = UserStatusEscalationService.getInstance()
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")
    
    /**
     * Get approved posts
     */
    suspend fun getApprovedPosts(
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AiModerationResult>> {
        return try {
            val approvedPosts = postsCollection
                .whereEqualTo("status", PostStatus.APPROVED)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val post = doc.toObject(FirestorePost::class.java)?.copy(id = doc.id)
                    Log.d("AiModerationRepository", "Fetched approved post: $post")
                    if (post != null) {
                        // Parse violation types from document
                        val violationTypeStrings = (doc.get("violation_type") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val violations = parseViolationTypes(violationTypeStrings)
                        
                        AiModerationResult(
                            postData = post,
                            isApproved = true,
                            overallScore = 0.0,
                            violations = violations
                        )
                    } else null
                }

            Log.d("AiModerationRepository", "Fetched ${approvedPosts.size} approved posts")

            Result.success(approvedPosts)
        } catch (e: Exception) {
            Log.e("AiModerationRepository", "Error fetching approved posts", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Get rejected posts
     */
    suspend fun getRejectedPosts(
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AiModerationResult>> {
        return try {
            val rejectedPosts = postsCollection
                .whereEqualTo("status", PostStatus.REJECTED)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val post = doc.toObject(FirestorePost::class.java)?.copy(id = doc.id)
                    if (post != null) {
                        // Parse violation types from document
                        val violationTypeStrings = (doc.get("violation_type") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val violations = parseViolationTypes(violationTypeStrings)
                        
                        AiModerationResult(
                            postData = post,
                            isApproved = false,
                            overallScore = 0.0,
                            violations = violations
                        )
                    } else null
                }

            Result.success(rejectedPosts)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Parse violation type strings from Firebase to ViolationType objects
     */
    private fun parseViolationTypes(violationStrings: List<String>): List<ViolationType> {
        return violationStrings.mapNotNull { typeString ->
            ViolationCategory.fromString(typeString)?.let { category ->
                ViolationType(
                    type = category,
                    score = 0.8, // Default score since Firebase may not have this
                    description = category.displayName
                )
            }
        }
    }
    
    /**
     * Override AI decision manually.
     * When rejecting a post (isApproved = false), this will also escalate the author's status.
     * 
     * @param postId The ID of the post to update
     * @param isApproved Whether the post is approved (true) or rejected/deleted (false)
     * @param authorId Optional author ID for status escalation (required when rejecting)
     * @param postTitle Optional post title for audit purposes
     * @param violationTypes List of violation types for audit purposes
     * @return Result containing success status and optional escalation result
     */
    suspend fun overrideModerationDecision(
        postId: String,
        isApproved: Boolean,
        authorId: String? = null,
        postTitle: String? = null,
        violationTypes: List<String> = emptyList()
    ): Result<ModerationDecisionResult> {
        return try {
            // Update post status in Firestore
            val newStatus = if (isApproved) PostStatus.APPROVED else PostStatus.DELETED
            postsCollection.document(postId)
                .update("status", newStatus)
                .await()
            
            var escalationResult: StatusEscalationResult? = null
            
            // If post is being rejected/deleted and we have author info, escalate user status
            if (!isApproved && authorId != null && authorId.isNotEmpty()) {
                Log.d("AiModerationRepository", "Post rejected, escalating status for author: $authorId")
                escalationResult = statusEscalationService.escalateUserStatus(authorId)
                
                if (escalationResult.success && escalationResult.wasEscalated) {
                    Log.d("AiModerationRepository", 
                        "Status escalated: ${escalationResult.previousStatus.value} -> ${escalationResult.newStatus.value}")
                } else if (!escalationResult.wasEscalated) {
                    Log.d("AiModerationRepository", "User already at maximum status (BANNED)")
                } else {
                    Log.w("AiModerationRepository", 
                        "Failed to escalate status: ${escalationResult.error}")
                }
            }

            Result.success(ModerationDecisionResult(
                success = true,
                postId = postId,
                newStatus = newStatus,
                escalationResult = escalationResult
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Override AI decision with full post data.
     * This version automatically extracts author info from the post.
     * 
     * @param post The FirestorePost to update
     * @param isApproved Whether the post is approved (true) or rejected/deleted (false)
     * @return Result containing success status and optional escalation result
     */
    suspend fun overrideModerationDecisionWithPost(
        post: FirestorePost,
        isApproved: Boolean
    ): Result<ModerationDecisionResult> {
        return overrideModerationDecision(
            postId = post.id,
            isApproved = isApproved,
            authorId = post.authorId,
            postTitle = post.title,
            violationTypes = post.violationTypes
        )
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
            val allResults = postsCollection
                .whereEqualTo("status",
                    when (isApproved) {
                        true -> PostStatus.APPROVED
                        false -> PostStatus.REJECTED
                        null -> null
                    }
                )
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val post = doc.toObject(FirestorePost::class.java)?.copy(id = doc.id)
                    if (post != null) {
                        val violationTypeStrings = (doc.get("violation_type") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val violations = parseViolationTypes(violationTypeStrings)
                        
                        AiModerationResult(
                            postData = post,
                            isApproved = doc.getString("status") == PostStatus.APPROVED.name,
                            overallScore = 0.0,
                            violations = violations
                        )
                    } else null
                }

            val filteredResults = allResults.filter { result ->
                result.postData.title.contains(query, ignoreCase = true) ||
                result.postData.content.contains(query, ignoreCase = true)
            }

            Result.success(filteredResults)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
