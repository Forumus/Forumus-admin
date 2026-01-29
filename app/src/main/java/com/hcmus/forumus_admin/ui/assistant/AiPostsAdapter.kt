package com.hcmus.forumus_admin.ui.assistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hcmus.forumus_admin.databinding.ItemAiPostCardBinding
import com.google.android.material.chip.Chip
import com.hcmus.forumus_admin.data.model.AiModerationResult
import com.hcmus.forumus_admin.data.model.FirestorePost
import com.hcmus.forumus_admin.data.model.Post
import com.hcmus.forumus_admin.data.model.PostStatus
import com.hcmus.forumus_admin.data.model.Tag
import com.hcmus.forumus_admin.data.repository.PostRepository
import kotlin.text.ifEmpty

class AiPostsAdapter(
    private val onApprove: (String) -> Unit,
    private val onReject: (String) -> Unit
) : ListAdapter<AiModerationResult, AiPostsAdapter.PostViewHolder>(PostDiffCallback()) {

    // Track which posts are currently loading
    private var loadingPostIds: Set<String> = emptySet()

    fun setLoadingPostIds(ids: Set<String>) {
        val oldIds = loadingPostIds
        loadingPostIds = ids
        // Notify items that changed loading state
        currentList.forEachIndexed { index, result ->
            val postId = result.postData.id
            if ((postId in oldIds) != (postId in ids)) {
                notifyItemChanged(index, PAYLOAD_LOADING_STATE)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemAiPostCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding, onApprove, onReject)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val isLoading = getItem(position).postData.id in loadingPostIds
        holder.bind(getItem(position), isLoading)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_LOADING_STATE)) {
            // Only update loading state, don't rebind everything
            val isLoading = getItem(position).postData.id in loadingPostIds
            holder.updateLoadingState(isLoading)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class PostViewHolder(
        private val binding: ItemAiPostCardBinding,
        private val onApprove: (String) -> Unit,
        private val onReject: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentPostId: String? = null

        fun bind(post: AiModerationResult, isLoading: Boolean) {
            currentPostId = post.postData.id
            
            binding.apply {
                postTitle.text = post.postData.title
                postAuthor.text = "by ${post.postData.authorName}"
                postDate.text = PostRepository.formatFirebaseTimestamp(post.postData.createdAt)
                postDescription.text = post.postData.content.take(100) + if (post.postData.content.length > 100) "..." else ""

                // Clear previous tags
                tagsContainer.removeAllViews()

                // Update approve button style based on post status
                // If post is AI rejected (not approved), show blue active style
                if (post.postData.status == PostStatus.REJECTED) {
                    approveButton.setBackgroundResource(com.hcmus.forumus_admin.R.drawable.bg_approve_button_active)
                } else {
                    approveButton.setBackgroundResource(com.hcmus.forumus_admin.R.drawable.bg_approve_button)
                }

                // Set click listeners
                approveButton.setOnClickListener {
                    if (!isButtonLoading()) {
                        onApprove(post.postData.id)
                    }
                }

                rejectButton.setOnClickListener {
                    if (!isButtonLoading()) {
                        onReject(post.postData.id)
                    }
                }
            }

            // Apply loading state
            updateLoadingState(isLoading)
        }

        fun updateLoadingState(isLoading: Boolean) {
            binding.apply {
                // Update approve button loading state
                approveLoadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                approveIcon.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
                approveText.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
                approveButton.isEnabled = !isLoading
                approveButton.alpha = if (isLoading) 0.6f else 1.0f

                // Update reject button loading state
                rejectLoadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                rejectIcon.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
                rejectText.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
                rejectButton.isEnabled = !isLoading
                rejectButton.alpha = if (isLoading) 0.6f else 1.0f
            }
        }

        private fun isButtonLoading(): Boolean {
            return binding.approveLoadingIndicator.visibility == View.VISIBLE
        }
    }

    private class PostDiffCallback : DiffUtil.ItemCallback<AiModerationResult>() {
        override fun areItemsTheSame(oldItem: AiModerationResult, newItem: AiModerationResult): Boolean {
            return oldItem.postData.id == newItem.postData.id
        }

        override fun areContentsTheSame(oldItem: AiModerationResult, newItem: AiModerationResult): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val PAYLOAD_LOADING_STATE = "loading_state"
    }
}
