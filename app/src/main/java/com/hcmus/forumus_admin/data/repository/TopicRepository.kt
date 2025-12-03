package com.hcmus.forumus_admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class FirestoreTopic(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val postCount: Int = 0
)

class TopicRepository {
    private val db = FirebaseFirestore.getInstance()
    private val topicsCollection = db.collection("topics")

    suspend fun getAllTopics(): Result<List<FirestoreTopic>> {
        return try {
            val snapshot = topicsCollection.get().await()
            val topics = snapshot.documents.mapNotNull { doc ->
                try {
                    FirestoreTopic(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id.replace("_", " "),
                        description = doc.getString("description") ?: "",
                        postCount = (doc.getLong("postCount") ?: 0L).toInt()
                    )
                } catch (e: Exception) {
                    // Skip documents that fail to parse
                    null
                }
            }
            Result.success(topics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopicById(topicId: String): Result<FirestoreTopic?> {
        return try {
            val doc = topicsCollection.document(topicId).get().await()
            val topic = if (doc.exists()) {
                FirestoreTopic(
                    id = doc.id,
                    name = doc.getString("name") ?: doc.id.replace("_", " "),
                    description = doc.getString("description") ?: "",
                    postCount = (doc.getLong("postCount") ?: 0L).toInt()
                )
            } else {
                null
            }
            Result.success(topic)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
