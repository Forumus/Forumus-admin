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
    
    suspend fun addTopic(name: String, description: String): Result<FirestoreTopic> {
        return try {
            val topicId = name.lowercase().replace(" ", "_")
            val topicData = hashMapOf(
                "name" to name,
                "description" to description,
                "postCount" to 0
            )
            topicsCollection.document(topicId).set(topicData).await()
            Result.success(FirestoreTopic(
                id = topicId,
                name = name,
                description = description,
                postCount = 0
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTopic(topicId: String, name: String, description: String): Result<Boolean> {
        return try {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "description" to description
            )
            topicsCollection.document(topicId).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTopic(topicId: String): Result<Boolean> {
        return try {
            topicsCollection.document(topicId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
