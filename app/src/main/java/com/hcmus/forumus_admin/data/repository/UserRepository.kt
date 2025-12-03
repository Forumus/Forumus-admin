package com.hcmus.forumus_admin.data.repository

import com.hcmus.forumus_admin.data.model.UserStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class FirestoreUser(
    val email: String = "",
    val fullName: String = "",
    val profilePictureUrl: String? = null,
    val role: String = "",
    val uid: String = "",
    val status: String = "normal"
)

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun getAllUsers(): Result<List<FirestoreUser>> {
        return try {
            val snapshot = usersCollection.get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    val email = doc.getString("email") ?: ""
                    if (email.isNotEmpty()) {
                        FirestoreUser(
                            email = email,
                            fullName = doc.getString("fullName") ?: email.substringBefore("@"),
                            profilePictureUrl = doc.getString("profilePictureUrl"),
                            role = doc.getString("role") ?: "STUDENT",
                            uid = doc.id,  // Use document ID as uid
                            status = doc.getString("status") ?: "normal"
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    // Skip documents that fail to parse
                    null
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByEmail(email: String): Result<FirestoreUser?> {
        return try {
            val snapshot = usersCollection
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            val user = snapshot.documents.firstOrNull()?.let { doc ->
                FirestoreUser(
                    email = doc.getString("email") ?: "",
                    fullName = doc.getString("fullName") ?: doc.getString("email")?.substringBefore("@") ?: "",
                    profilePictureUrl = doc.getString("profilePictureUrl"),
                    role = doc.getString("role") ?: "",
                    uid = doc.getString("uid") ?: doc.id,
                    status = doc.getString("status") ?: "normal"
                )
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBlacklistedUsers(): Result<List<FirestoreUser>> {
        return try {
            val snapshot = usersCollection.get().await()
            val blacklistedUsers = snapshot.documents.mapNotNull { doc ->
                try {
                    val email = doc.getString("email") ?: ""
                    val status = doc.getString("status") ?: "normal"
                    
                    // Only include users with status: ban, warning, or remind
                    if (email.isNotEmpty() && status in listOf("ban", "warning", "remind")) {
                        FirestoreUser(
                            email = email,
                            fullName = doc.getString("fullName") ?: email.substringBefore("@"),
                            profilePictureUrl = doc.getString("profilePictureUrl"),
                            role = doc.getString("role") ?: "STUDENT",
                            uid = doc.id,
                            status = status
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    // Skip documents that fail to parse
                    null
                }
            }
            Result.success(blacklistedUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun mapStatusToEnum(status: String): UserStatus {
            return when (status.lowercase()) {
                "ban" -> UserStatus.BAN
                "warning" -> UserStatus.WARNING
                "remind" -> UserStatus.REMIND
                else -> UserStatus.NORMAL
            }
        }
    }
}
