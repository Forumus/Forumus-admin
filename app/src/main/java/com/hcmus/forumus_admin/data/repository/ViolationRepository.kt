package com.hcmus.forumus_admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hcmus.forumus_admin.data.model.Violation
import kotlinx.coroutines.tasks.await

class ViolationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val violationsCollection = db.collection("violations")

    private var cachedViolations: List<Violation>? = null
    private var cacheTimestamp: Long = 0
    private val cacheExpirationMs = 30 * 60 * 1000L // 30 minutes

    suspend fun getAllViolations(): Result<List<Violation>> {
        // Check cache
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

    fun clearCache() {
        cachedViolations = null
        cacheTimestamp = 0
    }
}
