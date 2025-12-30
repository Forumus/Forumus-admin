package com.hcmus.forumus_admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class FirestorePost(
    val authorId: String = "",
    val authorName: String = "",
    val comment_count: Long = 0,
    val content: String = "",
    val createdAt: Any? = null,
    val downvote_count: Long = 0,
    val image_link: List<String> = emptyList(),
    val post_id: String = "",
    val reportCount: Long = 0,
    val status: String = "pending",
    val title: String = "",
    val topic: List<String> = emptyList(),
    val uid: String = "",
    val upvote_count: Long = 0,
    val video_link: List<String> = emptyList(),
    val violation_type: List<String> = emptyList()
)

class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection("posts")
    
    // Cache for posts
    private var cachedPosts: List<FirestorePost>? = null
    private var cacheTimestamp: Long = 0
    private val cacheExpirationMs = 5 * 60 * 1000L // 5 minutes

    suspend fun getAllPosts(): Result<List<FirestorePost>> {
        // Check cache first
        val currentTime = System.currentTimeMillis()
        if (cachedPosts != null && (currentTime - cacheTimestamp) < cacheExpirationMs) {
            return Result.success(cachedPosts!!)
        }
        
        return try {
            val snapshot = postsCollection.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    val status = doc.getString("status") ?: "pending"
                    // Filter out deleted posts
                    if (status.equals("DELETED", ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    
                    FirestorePost(
                        authorId = doc.getString("authorId") ?: "",
                        authorName = doc.getString("authorName") ?: "",
                        comment_count = doc.getLong("comment_count") ?: 0,
                        content = doc.getString("content") ?: "",
                        createdAt = doc.get("createdAt"),
                        downvote_count = doc.getLong("downvote_count") ?: 0,
                        image_link = (doc.get("image_link") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        post_id = doc.getString("post_id") ?: doc.id,
                        reportCount = doc.getLong("reportCount") ?: doc.getLong("reportedCount") ?: doc.getLong("report_count") ?: 0,
                        status = status,
                        title = doc.getString("title") ?: "",
                        topic = (doc.get("topic") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        uid = doc.getString("uid") ?: doc.id,
                        upvote_count = doc.getLong("upvote_count") ?: 0,
                        video_link = (doc.get("video_link") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        violation_type = (doc.get("violation_type") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            // Update cache
            cachedPosts = posts
            cacheTimestamp = currentTime
            
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get paginated posts with efficient loading
     * @param limit Number of posts to load
     * @param startAfterDoc Last document from previous page (null for first page)
     * @return Pair of posts list and last document for next page
     */
    suspend fun getPaginatedPosts(
        limit: Long = 20,
        startAfterDoc: com.google.firebase.firestore.DocumentSnapshot? = null
    ): Result<Pair<List<FirestorePost>, com.google.firebase.firestore.DocumentSnapshot?>> {
        return try {
            var query = postsCollection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
            
            // If there's a starting point, start after that document
            if (startAfterDoc != null) {
                query = query.startAfter(startAfterDoc)
            }
            
            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    val status = doc.getString("status") ?: "pending"
                    // Filter out deleted posts
                    if (status.equals("DELETED", ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    
                    FirestorePost(
                        authorId = doc.getString("authorId") ?: "",
                        authorName = doc.getString("authorName") ?: "",
                        comment_count = doc.getLong("comment_count") ?: 0,
                        content = doc.getString("content") ?: "",
                        createdAt = doc.get("createdAt"),
                        downvote_count = doc.getLong("downvote_count") ?: 0,
                        image_link = (doc.get("image_link") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        post_id = doc.getString("post_id") ?: doc.id,
                        reportCount = doc.getLong("reportCount") ?: doc.getLong("reportedCount") ?: doc.getLong("report_count") ?: 0,
                        status = status,
                        title = doc.getString("title") ?: "",
                        topic = (doc.get("topic") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        uid = doc.getString("uid") ?: doc.id,
                        upvote_count = doc.getLong("upvote_count") ?: 0,
                        video_link = (doc.get("video_link") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        violation_type = (doc.get("violation_type") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            val lastDoc = if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.last()
            } else {
                null
            }
            
            Result.success(Pair(posts, lastDoc))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get posts filtered by violation types with pagination
     * @param violationTypes List of violation type IDs to filter by (e.g., ["vio_001", "vio_007"])
     * @param limit Number of posts to load
     * @param startAfterDoc Last document from previous page (null for first page)
     * @return Pair of posts list and last document for next page
     */
    suspend fun getPostsByViolationTypes(
        violationTypes: List<String>,
        limit: Long = 20,
        startAfterDoc: com.google.firebase.firestore.DocumentSnapshot? = null
    ): Result<Pair<List<FirestorePost>, com.google.firebase.firestore.DocumentSnapshot?>> {
        if (violationTypes.isEmpty()) {
            return getPaginatedPosts(limit, startAfterDoc)
        }
        
        return try {
            // Firestore doesn't support array-contains-any with pagination directly
            // So we fetch all matching posts and handle pagination in memory
            var query = postsCollection
                .whereArrayContainsAny("violation_type", violationTypes)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            
            val snapshot = query.get().await()
            val allMatchingPosts = snapshot.documents.mapNotNull { doc ->
                try {
                    val status = doc.getString("status") ?: "pending"
                    // Filter out deleted posts
                    if (status.equals("DELETED", ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    
                    FirestorePost(
                        authorId = doc.getString("authorId") ?: "",
                        authorName = doc.getString("authorName") ?: "",
                        comment_count = doc.getLong("comment_count") ?: 0,
                        content = doc.getString("content") ?: "",
                        createdAt = doc.get("createdAt"),
                        downvote_count = doc.getLong("downvote_count") ?: 0,
                        image_link = (doc.get("image_link") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        post_id = doc.getString("post_id") ?: doc.id,
                        reportCount = doc.getLong("reportCount") ?: doc.getLong("reportedCount") ?: doc.getLong("report_count") ?: 0,
                        status = status,
                        title = doc.getString("title") ?: "",
                        topic = (doc.get("topic") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        uid = doc.getString("uid") ?: doc.id,
                        upvote_count = doc.getLong("upvote_count") ?: 0,
                        video_link = (doc.get("video_link") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        violation_type = (doc.get("violation_type") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            // Return all matching posts (pagination will be handled by the fragment)
            Result.success(Pair(allMatchingPosts, null))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear cached posts - call this when data changes
     */
    fun clearCache() {
        cachedPosts = null
        cacheTimestamp = 0
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            // Find the document with matching post_id field
            val snapshot = postsCollection
                .whereEqualTo("post_id", postId)
                .get()
                .await()
            
            if (snapshot.documents.isNotEmpty()) {
                // Delete the first matching document
                snapshot.documents.first().reference.delete().await()
                clearCache() // Clear cache after deletion
                Result.success(Unit)
            } else {
                // If no document found with post_id field, try using postId as document ID
                postsCollection.document(postId).delete().await()
                clearCache() // Clear cache after deletion
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update post to clear all report-related fields.
     * Sets reportCount to 0 and clears reportedUsers array.
     * This is internal - use ReportRepository.dismissReportsForPost for the complete atomic operation.
     */
    internal suspend fun updatePostReportStatus(postId: String, batch: com.google.firebase.firestore.WriteBatch? = null): Result<Unit> {
        return try {
            // Find the document with matching post_id field
            val snapshot = postsCollection
                .whereEqualTo("post_id", postId)
                .get()
                .await()
            
            if (snapshot.documents.isEmpty()) {
                return Result.failure(Exception("Post not found: $postId"))
            }
            
            val docRef = snapshot.documents.first().reference
            
            // Prepare the update data
            val updates = hashMapOf<String, Any>(
                "reportCount" to 0,
                "reportedUsers" to emptyList<String>()
            )
            
            if (batch != null) {
                // If batch is provided, add to batch (for atomic operations)
                batch.update(docRef, updates)
                Result.success(Unit)
            } else {
                // Otherwise, execute immediately
                docRef.update(updates).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun formatFirebaseTimestamp(timestamp: Any?): String {
            return try {
                when (timestamp) {
                    is com.google.firebase.Timestamp -> {
                        val date = timestamp.toDate()
                        SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(date)
                    }
                    is Long -> {
                        val date = Date(timestamp)
                        SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(date)
                    }
                    else -> "Unknown date"
                }
            } catch (e: Exception) {
                "Unknown date"
            }
        }

        fun getFirebaseTimestampAsDate(timestamp: Any?): Date? {
            return try {
                when (timestamp) {
                    is com.google.firebase.Timestamp -> timestamp.toDate()
                    is Long -> Date(timestamp)
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
