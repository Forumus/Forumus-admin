package com.hcmus.forumus_admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class FirestorePost(
    val authorId: String = "",
    val comment_count: Long = 0,
    val content: String = "",
    val createdAt: Any? = null,
    val downvote_count: Long = 0,
    val image_link: List<String> = emptyList(),
    val post_id: String = "",
    val report_count: Long = 0,
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

    suspend fun getAllPosts(): Result<List<FirestorePost>> {
        return try {
            val snapshot = postsCollection.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    FirestorePost(
                        authorId = doc.getString("authorId") ?: "",
                        comment_count = doc.getLong("comment_count") ?: 0,
                        content = doc.getString("content") ?: "",
                        createdAt = doc.get("createdAt"),
                        downvote_count = doc.getLong("downvote_count") ?: 0,
                        image_link = (doc.get("image_link") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        post_id = doc.getString("post_id") ?: doc.id,
                        report_count = doc.getLong("report_count") ?: 0,
                        status = doc.getString("status") ?: "pending",
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
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
                Result.success(Unit)
            } else {
                // If no document found with post_id field, try using postId as document ID
                postsCollection.document(postId).delete().await()
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
