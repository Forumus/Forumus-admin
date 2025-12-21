package com.hcmus.forumus_admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_admin.data.model.Violation
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing violation types from Firebase
 */
class ViolationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val violationsCollection = db.collection("violations")
    
    // Cache for violations
    private var cachedViolations: List<Violation>? = null
    private var cacheTimestamp: Long = 0
    private val cacheExpirationMs = 30 * 60 * 1000L // 30 minutes
    
    /**
     * Get all violation types from Firebase
     */
    suspend fun getAllViolations(): Result<List<Violation>> {
        // Check cache first
        val currentTime = System.currentTimeMillis()
        if (cachedViolations != null && (currentTime - cacheTimestamp) < cacheExpirationMs) {
            return Result.success(cachedViolations!!)
        }
        
        return try {
            val snapshot = violationsCollection.get().await()
            val violations = snapshot.documents.mapNotNull { doc ->
                try {
                    Violation(
                        violation = doc.getString("violation") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            // Update cache
            cachedViolations = violations
            cacheTimestamp = currentTime
            
            Result.success(violations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear cached violations - call this when data changes
     */
    fun clearCache() {
        cachedViolations = null
        cacheTimestamp = 0
    }
}
