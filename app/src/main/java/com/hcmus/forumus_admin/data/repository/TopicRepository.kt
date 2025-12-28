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
            
            // Get existing colors to avoid duplicates
            val existingColors = mutableSetOf<String>()
            try {
                val existingTopics = topicsCollection.get().await()
                existingTopics.documents.forEach { doc ->
                    doc.getString("fillColor")?.let { existingColors.add(it) }
                }
            } catch (e: Exception) {
                // Continue with empty set if can't fetch
            }
            
            // Generate a random color that's not gray and not already used
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
        // Predefined highly distinct colors with maximum perceptual difference
        // Colors carefully chosen to be visually distinct even for color-blind users
        val distinctColors = listOf(
            "#E74C3C", // Vivid Red
            "#3498DB", // Bright Blue
            "#2ECC71", // Emerald Green
            "#F39C12", // Vibrant Orange
            "#9B59B6", // Purple
            "#1ABC9C", // Turquoise
            "#E67E22", // Carrot Orange
            "#16A085", // Dark Cyan
            "#D35400", // Pumpkin
            "#C0392B", // Dark Red
            "#8E44AD", // Wisteria Purple
            "#27AE60", // Nephritis Green
            "#2980B9", // Belize Blue
            "#F1C40F", // Sunflower Yellow
            "#E91E63", // Pink
            "#00BCD4", // Cyan
            "#4CAF50", // Green
            "#FF5722", // Deep Orange
            "#673AB7", // Deep Purple
            "#009688"  // Teal
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
            
            // Ensure it's not a gray tone (RGB values should differ significantly)
            val maxDiff = maxOf(Math.abs(r - g), Math.abs(g - b), Math.abs(r - b))
            if (maxDiff < 80) {  // Skip gray-like colors
                attempts++
                continue
            }
            
            val candidateColor = String.format("#%02X%02X%02X", r, g, b)
            
            // Calculate minimum distance to all existing colors
            val minDistance = existingColors.minOfOrNull { existingColor ->
                calculateColorDistance(candidateColor, existingColor)
            } ?: Double.MAX_VALUE
            
            // Keep the color with maximum minimum distance (most distinct from all existing)
            if (minDistance > maxMinDistance) {
                maxMinDistance = minDistance
                bestColor = candidateColor
            }
            
            // If we found a color that's very different from all existing ones, use it
            if (minDistance > 150.0) {
                return candidateColor
            }
            
            attempts++
        }
        
        return bestColor
    }
    
    /**
     * Calculate perceptual color distance using weighted Euclidean distance in RGB space
     * This approximates human color perception where differences in certain colors are more noticeable
     */
    private fun calculateColorDistance(color1: String, color2: String): Double {
        val rgb1 = hexToRgb(color1)
        val rgb2 = hexToRgb(color2)
        
        // Weighted RGB distance (approximates perceptual difference)
        // Red and green have higher weights as humans are more sensitive to these
        val rDiff = (rgb1[0] - rgb2[0]) * 0.3
        val gDiff = (rgb1[1] - rgb2[1]) * 0.59
        val bDiff = (rgb1[2] - rgb2[2]) * 0.11
        
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff)
    }
    
    /**
     * Convert hex color string to RGB array
     */
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
