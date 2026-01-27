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
    val status: String = "NORMAL"
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
                            status = (doc.getString("status") ?: "NORMAL").uppercase()
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
                    status = (doc.getString("status") ?: "NORMAL").uppercase()
                )
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a user by their document ID (uid).
     * 
     * @param userId The document ID of the user
     * @return Result containing the user or error
     */
    suspend fun getUserById(userId: String): Result<FirestoreUser?> {
        return try {
            val doc = usersCollection.document(userId).get().await()
            
            if (!doc.exists()) {
                return Result.success(null)
            }
            
            val user = FirestoreUser(
                email = doc.getString("email") ?: "",
                fullName = doc.getString("fullName") ?: doc.getString("email")?.substringBefore("@") ?: "",
                profilePictureUrl = doc.getString("profilePictureUrl"),
                role = doc.getString("role") ?: "",
                uid = doc.id,
                status = (doc.getString("status") ?: "NORMAL").uppercase()
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if a user is banned.
     * 
     * @param userId The document ID of the user
     * @return true if the user is banned, false otherwise
     */
    suspend fun isUserBanned(userId: String): Boolean {
        return try {
            val result = getUserById(userId)
            val user = result.getOrNull() ?: return false
            user.status.uppercase() == "BANNED"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a user can create posts (not banned).
     * 
     * @param userId The document ID of the user
     * @return Result containing true if user can post, false if banned, or error
     */
    suspend fun canUserCreatePost(userId: String): Result<Boolean> {
        return try {
            val result = getUserById(userId)
            val user = result.getOrNull()
            
            if (user == null) {
                // User not found - allow posting (they might be new)
                return Result.success(true)
            }
            
            val isBanned = user.status.uppercase() == "BANNED"
            Result.success(!isBanned)
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
                    val status = (doc.getString("status") ?: "NORMAL").uppercase()
                    
                    // Only include users with status: BANNED, WARNED, or REMINDED
                    if (email.isNotEmpty() && status in listOf("BANNED", "WARNED", "REMINDED")) {
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

    suspend fun updateUserStatus(userId: String, newStatus: UserStatus): Result<Unit> {
        return try {
            val statusString = mapEnumToStatus(newStatus)

            if (statusString == "BANNED") {
                val oneYearFromNow = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)

                usersCollection.document(userId)
                    .update(mapOf(
                        "status" to statusString,
                        "blacklistedUntil" to oneYearFromNow
                    ))
                    .await()
            } else {
                usersCollection.document(userId)
                    .update(mapOf(
                        "status" to statusString,
                        "blacklistedUntil" to null
                    ))
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromBlacklist(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("status", "NORMAL")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun mapStatusToEnum(status: String): UserStatus {
            return when (status.uppercase()) {
                "BANNED" -> UserStatus.BANNED
                "WARNED" -> UserStatus.WARNED
                "REMINDED" -> UserStatus.REMINDED
                else -> UserStatus.NORMAL
            }
        }

        fun mapEnumToStatus(status: UserStatus): String {
            return when (status) {
                UserStatus.BANNED -> "BANNED"
                UserStatus.WARNED -> "WARNED"
                UserStatus.REMINDED -> "REMINDED"
                UserStatus.NORMAL -> "NORMAL"
            }
        }
    }
}
