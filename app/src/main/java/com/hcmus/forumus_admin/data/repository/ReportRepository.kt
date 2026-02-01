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

            val violationCounts = mutableMapOf<String, Int>()
            
            postIds.chunked(10).forEach { batch ->
                val snapshot = reportsCollection
                    .whereIn("postId", batch)
                    .get()
                    .await()

                val reportsByPost = snapshot.documents
                    .mapNotNull { doc ->
                        try {
                            val postId = doc.getString("postId")
                            val violationType = doc.getString("nameViolation") ?: ""
                            if (postId != null && violationType.isNotEmpty()) {
                                Pair(postId, violationType)
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .groupBy { it.first } // Group by postId
                
                // Count unique violation types for each post
                reportsByPost.forEach { (postId, reports) ->
                    val uniqueViolations = reports.map { it.second }.toSet().size
                    violationCounts[postId] = uniqueViolations
                }
            }
            
            Result.success(violationCounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReportCountsForPosts(postIds: List<String>): Result<Map<String, Int>> {
        return try {
            if (postIds.isEmpty()) {
                return Result.success(emptyMap())
            }

            val reportCounts = mutableMapOf<String, Int>()
            
            postIds.chunked(10).forEach { batch ->
                val snapshot = reportsCollection
                    .whereIn("postId", batch)
                    .get()
                    .await()

                val batchCounts = snapshot.documents
                    .mapNotNull { doc ->
                        try {
                            doc.getString("postId")
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .groupBy { it }
                    .mapValues { entry -> entry.value.size }
                
                reportCounts.putAll(batchCounts)
            }
            
            Result.success(reportCounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

                        val postBreakdown = violationBreakdown.getOrPut(postId) { mutableMapOf() }

                        postBreakdown[nameViolation] = (postBreakdown[nameViolation] ?: 0) + 1
                    } catch (e: Exception) {

                    }
                }
            }
            
            Result.success(violationBreakdown)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dismissReportsForPost(postId: String): Result<Unit> {
        return try {
            val postsCollection = db.collection("posts")

            var postSnapshot = postsCollection
                .whereEqualTo("post_id", postId)
                .get()
                .await()
            
            val postDocRef = if (postSnapshot.documents.isNotEmpty()) {
                postSnapshot.documents.first().reference
            } else {
                val directRef = postsCollection.document(postId)
                val directDoc = directRef.get().await()
                
                if (directDoc.exists()) {
                    directRef
                } else {
                    return Result.failure(Exception("Post not found: $postId. Checked both post_id field and document ID."))
                }
            }

            val reportsSnapshot = reportsCollection
                .whereEqualTo("postId", postId)
                .get()
                .await()

            val batch = db.batch()

            reportsSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.update(
                postDocRef,
                hashMapOf<String, Any>(
                    "reportCount" to 0,
                    "reportedUsers" to emptyList<String>()
                )
            )

            batch.commit().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


