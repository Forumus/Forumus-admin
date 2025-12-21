package com.hcmus.forumus_admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_admin.data.model.Report
import com.hcmus.forumus_admin.data.model.ViolationDescription
import kotlinx.coroutines.tasks.await

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = db.collection("reports")

    suspend fun getReportsForPost(postId: String): Result<List<Report>> {
        return try {
            val snapshot = reportsCollection
                .whereEqualTo("postId", postId)
                .get()
                .await()
            
            val reports = snapshot.documents.mapNotNull { doc ->
                try {
                    val descriptionViolationMap = doc.get("descriptionViolation") as? Map<*, *>
                    val violationDescription = ViolationDescription(
                        description = descriptionViolationMap?.get("description") as? String ?: "",
                        id = descriptionViolationMap?.get("id") as? String ?: "",
                        name = descriptionViolationMap?.get("name") as? String ?: ""
                    )
                    
                    Report(
                        id = doc.id,
                        authorId = doc.getString("authorId") ?: "",
                        descriptionViolation = violationDescription,
                        nameViolation = doc.getString("nameViolation") ?: "",
                        postId = doc.getString("postId") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getViolationCountsForPosts(postIds: List<String>): Result<Map<String, Int>> {
        return try {
            if (postIds.isEmpty()) {
                return Result.success(emptyMap())
            }
            
            // Firestore whereIn() has a limit of 10 items, so we need to batch
            val violationCounts = mutableMapOf<String, Int>()
            
            postIds.chunked(10).forEach { batch ->
                val snapshot = reportsCollection
                    .whereIn("postId", batch)
                    .get()
                    .await()
                
                // Group reports by postId and count TOTAL reports (not just unique violations)
                val batchCounts = snapshot.documents
                    .mapNotNull { doc ->
                        try {
                            doc.getString("postId")
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .groupBy { it }
                    .mapValues { entry ->
                        // Count total number of reports for each post
                        entry.value.size
                    }
                
                violationCounts.putAll(batchCounts)
            }
            
            Result.success(violationCounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get violation breakdown for each post, grouped by violation type.
     * Returns a map of postId to a map of violation type to count.
     * Example: {"post123": {"SPAM": 2, "HARASSMENT": 1}, "post456": {"SPAM": 3}}
     */
    suspend fun getViolationBreakdownForPosts(postIds: List<String>): Result<Map<String, Map<String, Int>>> {
        return try {
            if (postIds.isEmpty()) {
                return Result.success(emptyMap())
            }
            
            val violationBreakdown = mutableMapOf<String, MutableMap<String, Int>>()
            
            postIds.chunked(10).forEach { batch ->
                val snapshot = reportsCollection
                    .whereIn("postId", batch)
                    .get()
                    .await()
                
                snapshot.documents.forEach { doc ->
                    try {
                        val postId = doc.getString("postId") ?: return@forEach
                        val nameViolation = doc.getString("nameViolation") ?: "Unknown"
                        
                        // Initialize map for this post if not exists
                        val postBreakdown = violationBreakdown.getOrPut(postId) { mutableMapOf() }
                        
                        // Increment count for this violation type
                        postBreakdown[nameViolation] = (postBreakdown[nameViolation] ?: 0) + 1
                    } catch (e: Exception) {
                        // Skip malformed documents
                    }
                }
            }
            
            Result.success(violationBreakdown)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Atomically dismiss all reports for a post.
     * 
     * This operation uses explicit document reads followed by batch writes to ensure:
     * 1. Document existence is verified BEFORE attempting writes
     * 2. All operations are atomic (all succeed or all fail)
     * 3. No partial updates can occur
     * 
     * Why explicit reads + batch writes (not transactions)?
     * - Transactions require reads INSIDE the transaction, causing conflicts
     * - We don't need transactional read-modify-write logic
     * - Batch writes are simpler, faster, and sufficient for write-only operations
     * - Verifying existence first prevents "document not found" errors
     */
    suspend fun dismissReportsForPost(postId: String): Result<Unit> {
        return try {
            // Step 1: Find the post document reference (try both query and direct ID)
            val postsCollection = db.collection("posts")
            
            // Try finding by post_id field first
            var postSnapshot = postsCollection
                .whereEqualTo("post_id", postId)
                .get()
                .await()
            
            val postDocRef = if (postSnapshot.documents.isNotEmpty()) {
                // Found by field query
                postSnapshot.documents.first().reference
            } else {
                // Try using postId as document ID directly
                val directRef = postsCollection.document(postId)
                val directDoc = directRef.get().await()
                
                if (directDoc.exists()) {
                    directRef
                } else {
                    // Post truly doesn't exist
                    return Result.failure(Exception("Post not found: $postId. Checked both post_id field and document ID."))
                }
            }
            
            // Step 2: Get all report documents for this post
            val reportsSnapshot = reportsCollection
                .whereEqualTo("postId", postId)
                .get()
                .await()
            
            // Step 3: Create batch for atomic writes
            val batch = db.batch()
            
            // Step 4: Add all report deletions to batch
            reportsSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            // Step 5: Add post update to batch
            batch.update(
                postDocRef,
                hashMapOf<String, Any>(
                    "reportCount" to 0,
                    "reportedUsers" to emptyList<String>()
                )
            )
            
            // Step 6: Commit batch atomically
            // If this fails, NO changes are applied to Firestore
            batch.commit().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            // Any failure (network, permissions, etc.) is caught here
            Result.failure(e)
        }
    }
}


