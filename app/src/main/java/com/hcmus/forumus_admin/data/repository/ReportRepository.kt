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
}


