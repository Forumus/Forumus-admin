package com.hcmus.forumus_admin.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_admin.data.model.*
import com.hcmus.forumus_admin.data.model.FirestorePost
import com.hcmus.forumus_admin.data.service.AiModerationService
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing AI moderation data
 * Acts as a single source of truth for AI moderation operations
 */
class AiModerationRepository(
//    private val service: AiModerationService = AiModerationService()
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
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    AiModerationResult(
                        postData = doc.toObject(FirestorePost::class.java)!!.copy(id = doc.id),
                        isApproved = true,
                        overallScore = 0.0,
                        violations = emptyList()
                    )
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
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    AiModerationResult(
                        postData = doc.toObject(FirestorePost::class.java)!!.copy(id = doc.id),
                        isApproved = false,
                        overallScore = 0.0,
                        violations = emptyList()
                    )
                }

            Result.success(rejectedPosts)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Override AI decision manually
     */
    suspend fun overrideModerationDecision(
        postId: String,
        isApproved: Boolean
    ): Result<Boolean> {
        return try {
            // Update post status in Firestore
            val newStatus = if (isApproved) PostStatus.APPROVED else PostStatus.DELETED
            postsCollection.document(postId)
                .update("status", newStatus)
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
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
                    AiModerationResult(
                        postData = doc.toObject(FirestorePost::class.java)!!.copy(id = doc.id),
                        isApproved = doc.getString("status") == PostStatus.APPROVED.name,
                        overallScore = 0.0,
                        violations = emptyList()
                    )
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