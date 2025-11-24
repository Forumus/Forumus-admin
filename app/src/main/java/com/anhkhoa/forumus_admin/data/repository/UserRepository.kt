package com.anhkhoa.forumus_admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class FirestoreUser(
    val email: String = "",
    val fullName: String = "",
    val profilePictureUrl: String? = null,
    val role: String = "",
    val uid: String = ""
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
                            uid = doc.id  // Use document ID as uid
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
                    uid = doc.getString("uid") ?: doc.id
                )
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
