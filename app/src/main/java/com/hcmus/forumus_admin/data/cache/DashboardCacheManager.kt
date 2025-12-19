package com.hcmus.forumus_admin.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hcmus.forumus_admin.data.repository.FirestorePost
import com.hcmus.forumus_admin.data.repository.FirestoreTopic

/**
 * Manages caching of dashboard statistics to avoid recalculating complex statistics
 * every time the app is opened. Data is refreshed when:
 * - User manually pulls to refresh
 * - Cache expires (default: 5 minutes)
 */
class DashboardCacheManager(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "dashboard_cache"
        private const val KEY_TOTAL_USERS = "total_users"
        private const val KEY_BLACKLISTED_USERS = "blacklisted_users"
        private const val KEY_TOTAL_POSTS = "total_posts"
        private const val KEY_REPORTED_POSTS = "reported_posts"
        private const val KEY_POSTS_DATA = "posts_data"
        private const val KEY_TOPICS_DATA = "topics_data"
        private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
        
        // Cache expiration time: 5 minutes (in milliseconds)
        private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L
        
        @Volatile
        private var instance: DashboardCacheManager? = null
        
        fun getInstance(context: Context): DashboardCacheManager {
            return instance ?: synchronized(this) {
                instance ?: DashboardCacheManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * Data class to hold all dashboard statistics
     */
    data class DashboardStats(
        val totalUsers: Int,
        val blacklistedUsers: Int,
        val totalPosts: Int,
        val reportedPosts: Int
    )
    
    /**
     * Check if cache is still valid (not expired)
     */
    fun isCacheValid(): Boolean {
        val cacheTimestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - cacheTimestamp) < CACHE_EXPIRATION_MS
    }
    
    /**
     * Get the cache age in a human-readable format
     */
    fun getCacheAge(): String {
        val cacheTimestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0)
        if (cacheTimestamp == 0L) return "No cache"
        
        val ageMs = System.currentTimeMillis() - cacheTimestamp
        val ageSeconds = ageMs / 1000
        val ageMinutes = ageSeconds / 60
        
        return when {
            ageMinutes > 0 -> "$ageMinutes min ago"
            ageSeconds > 0 -> "$ageSeconds sec ago"
            else -> "Just now"
        }
    }
    
    /**
     * Save dashboard statistics to cache
     */
    fun saveDashboardStats(stats: DashboardStats) {
        prefs.edit().apply {
            putInt(KEY_TOTAL_USERS, stats.totalUsers)
            putInt(KEY_BLACKLISTED_USERS, stats.blacklistedUsers)
            putInt(KEY_TOTAL_POSTS, stats.totalPosts)
            putInt(KEY_REPORTED_POSTS, stats.reportedPosts)
            putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Get cached dashboard statistics
     * Returns null if no cache exists
     */
    fun getDashboardStats(): DashboardStats? {
        val totalUsers = prefs.getInt(KEY_TOTAL_USERS, -1)
        if (totalUsers == -1) return null
        
        return DashboardStats(
            totalUsers = totalUsers,
            blacklistedUsers = prefs.getInt(KEY_BLACKLISTED_USERS, 0),
            totalPosts = prefs.getInt(KEY_TOTAL_POSTS, 0),
            reportedPosts = prefs.getInt(KEY_REPORTED_POSTS, 0)
        )
    }
    
    /**
     * Save posts data to cache (serialized as JSON)
     */
    fun savePostsData(posts: List<FirestorePost>) {
        val postsJson = gson.toJson(posts.map { post ->
            // Convert to a cacheable format (without Firebase Timestamp)
            CachedPost(
                authorId = post.authorId,
                comment_count = post.comment_count,
                content = post.content,
                createdAtMillis = getTimestampMillis(post.createdAt),
                downvote_count = post.downvote_count,
                image_link = post.image_link,
                post_id = post.post_id,
                reportCount = post.reportCount,
                status = post.status,
                title = post.title,
                topic = post.topic,
                uid = post.uid,
                upvote_count = post.upvote_count,
                video_link = post.video_link,
                violation_type = post.violation_type
            )
        })
        prefs.edit().putString(KEY_POSTS_DATA, postsJson).apply()
    }
    
    /**
     * Get cached posts data
     */
    fun getPostsData(): List<FirestorePost>? {
        val postsJson = prefs.getString(KEY_POSTS_DATA, null) ?: return null
        return try {
            val type = object : TypeToken<List<CachedPost>>() {}.type
            val cachedPosts: List<CachedPost> = gson.fromJson(postsJson, type)
            cachedPosts.map { cached ->
                FirestorePost(
                    authorId = cached.authorId,
                    comment_count = cached.comment_count,
                    content = cached.content,
                    createdAt = cached.createdAtMillis, // Store as Long for cache
                    downvote_count = cached.downvote_count,
                    image_link = cached.image_link,
                    post_id = cached.post_id,
                    reportCount = cached.reportCount,
                    status = cached.status,
                    title = cached.title,
                    topic = cached.topic,
                    uid = cached.uid,
                    upvote_count = cached.upvote_count,
                    video_link = cached.video_link,
                    violation_type = cached.violation_type
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Save topics data to cache
     */
    fun saveTopicsData(topics: List<FirestoreTopic>) {
        val topicsJson = gson.toJson(topics.map { topic ->
            CachedTopic(
                id = topic.id,
                name = topic.name,
                description = topic.description,
                postCount = topic.postCount
            )
        })
        prefs.edit().putString(KEY_TOPICS_DATA, topicsJson).apply()
    }
    
    /**
     * Get cached topics data
     */
    fun getTopicsData(): List<FirestoreTopic>? {
        val topicsJson = prefs.getString(KEY_TOPICS_DATA, null) ?: return null
        return try {
            val type = object : TypeToken<List<CachedTopic>>() {}.type
            val cachedTopics: List<CachedTopic> = gson.fromJson(topicsJson, type)
            cachedTopics.map { cached ->
                FirestoreTopic(
                    id = cached.id,
                    name = cached.name,
                    description = cached.description,
                    postCount = cached.postCount
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Force refresh - invalidate the cache
     */
    fun invalidateCache() {
        prefs.edit().putLong(KEY_CACHE_TIMESTAMP, 0).apply()
    }
    
    /**
     * Clear all cached data
     */
    fun clearCache() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Helper to convert Firebase Timestamp to milliseconds
     */
    private fun getTimestampMillis(timestamp: Any?): Long {
        return when (timestamp) {
            is com.google.firebase.Timestamp -> timestamp.toDate().time
            is Long -> timestamp
            else -> 0L
        }
    }
    
    /**
     * Cacheable version of FirestorePost (without Firebase-specific types)
     */
    private data class CachedPost(
        val authorId: String = "",
        val comment_count: Long = 0,
        val content: String = "",
        val createdAtMillis: Long = 0,
        val downvote_count: Long = 0,
        val image_link: List<String> = emptyList(),
        val post_id: String = "",
        val reportCount: Long = 0,
        val status: String = "pending",
        val title: String = "",
        val topic: List<String> = emptyList(),
        val uid: String = "",
        val upvote_count: Long = 0,
        val video_link: List<String> = emptyList(),
        val violation_type: List<String> = emptyList()
    )
    
    /**
     * Cacheable version of FirestoreTopic
     */
    private data class CachedTopic(
        val id: String = "",
        val name: String = "",
        val description: String = "",
        val postCount: Int = 0
    )
}
