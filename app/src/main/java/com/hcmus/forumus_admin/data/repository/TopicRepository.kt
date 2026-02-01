package com.hcmus.forumus_admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class FirestoreTopic(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val postCount: Int = 0,
    val fillAlpha: Double = 0.125,
    val fillColor: String = "#808080"
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
                        postCount = (doc.getLong("postCount") ?: 0L).toInt(),
                        fillAlpha = doc.getDouble("fillAlpha") ?: 0.125,
                        fillColor = doc.getString("fillColor") ?: "#808080"
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
                    postCount = (doc.getLong("postCount") ?: 0L).toInt(),
                    fillAlpha = doc.getDouble("fillAlpha") ?: 0.125,
                    fillColor = doc.getString("fillColor") ?: "#808080"
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

            val existingColors = mutableSetOf<String>()
            try {
                val existingTopics = topicsCollection.get().await()
                existingTopics.documents.forEach { doc ->
                    doc.getString("fillColor")?.let { existingColors.add(it) }
                }
            } catch (e: Exception) {

            }

            val fillColor = generateUniqueColor(existingColors)
            val fillAlpha = 0.125
            
            val topicData = hashMapOf(
                "name" to name,
                "description" to description,
                "postCount" to 0,
                "fillAlpha" to fillAlpha,
                "fillColor" to fillColor
            )
            topicsCollection.document(topicId).set(topicData).await()
            Result.success(FirestoreTopic(
                id = topicId,
                name = name,
                description = description,
                postCount = 0,
                fillAlpha = fillAlpha,
                fillColor = fillColor
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateUniqueColor(existingColors: Set<String>): String {
        val distinctColors = listOf(
            "#E74C3C",
            "#3498DB",
            "#2ECC71",
            "#F39C12",
            "#9B59B6",
            "#1ABC9C",
            "#E67E22",
            "#16A085",
            "#D35400",
            "#C0392B",
            "#8E44AD",
            "#27AE60",
            "#2980B9",
            "#F1C40F",
            "#E91E63",
            "#00BCD4",
            "#4CAF50",
            "#FF5722",
            "#673AB7",
            "#009688"
        )
        
        // First try to find an unused predefined color
        val unusedColor = distinctColors.firstOrNull { it !in existingColors }
        if (unusedColor != null) {
            return unusedColor
        }
        
        // If all predefined colors are used, generate a color with maximum distance from existing ones
        var bestColor = distinctColors.random()
        var maxMinDistance = 0.0
        
        var attempts = 0
        while (attempts < 100) {
            val r = (80..255).random()
            val g = (80..255).random()
            val b = (80..255).random()

            val maxDiff = maxOf(Math.abs(r - g), Math.abs(g - b), Math.abs(r - b))
            if (maxDiff < 80) {  // Skip gray-like colors
                attempts++
                continue
            }
            
            val candidateColor = String.format("#%02X%02X%02X", r, g, b)

            val minDistance = existingColors.minOfOrNull { existingColor ->
                calculateColorDistance(candidateColor, existingColor)
            } ?: Double.MAX_VALUE

            if (minDistance > maxMinDistance) {
                maxMinDistance = minDistance
                bestColor = candidateColor
            }

            if (minDistance > 150.0) {
                return candidateColor
            }
            
            attempts++
        }
        
        return bestColor
    }

    private fun calculateColorDistance(color1: String, color2: String): Double {
        val rgb1 = hexToRgb(color1)
        val rgb2 = hexToRgb(color2)

        val rDiff = (rgb1[0] - rgb2[0]) * 0.3
        val gDiff = (rgb1[1] - rgb2[1]) * 0.59
        val bDiff = (rgb1[2] - rgb2[2]) * 0.11
        
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff)
    }

    private fun hexToRgb(hex: String): IntArray {
        val cleanHex = hex.removePrefix("#")
        return intArrayOf(
            cleanHex.substring(0, 2).toInt(16),
            cleanHex.substring(2, 4).toInt(16),
            cleanHex.substring(4, 6).toInt(16)
        )
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
