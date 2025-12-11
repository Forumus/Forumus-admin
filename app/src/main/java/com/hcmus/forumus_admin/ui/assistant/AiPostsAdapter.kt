package com.hcmus.forumus_admin.ui.assistant

import android.view.LayoutInflater
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemAiPostCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding, onApprove, onReject)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostViewHolder(
        private val binding: ItemAiPostCardBinding,
        private val onApprove: (String) -> Unit,
        private val onReject: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: AiModerationResult) {
            binding.apply {
                postTitle.text = post.postData.title
                postAuthor.text = "by ${post.postData.authorName}"
                postDate.text = PostRepository.formatFirebaseTimestamp(post.postData.createdAt)
                postDescription.text = post.postData.content.take(100) + if (post.postData.content.length > 100) "..." else ""

                // Clear previous tags
                tagsContainer.removeAllViews()

                // Add tags
//                post.postData.tags.forEach { tag ->
//                    val chip = Chip(binding.root.context).apply {
//                        text = tag.name
//                        chipBackgroundColor = ContextCompat.getColorStateList(
//                            context,
//                            tag.backgroundColor
//                        )
//                        setTextColor(ContextCompat.getColor(context, tag.textColor))
//                        isClickable = false
//                        isCheckable = false
//                    }
//                    tagsContainer.addView(chip)
//                }

                // Update approve button style based on post status
                // If post is AI rejected (not approved), show blue active style
                if (post.postData.status == PostStatus.REJECTED) {
                    approveButton.setBackgroundResource(com.hcmus.forumus_admin.R.drawable.bg_approve_button_active)
                } else {
                    approveButton.setBackgroundResource(com.hcmus.forumus_admin.R.drawable.bg_approve_button)
                }

                // Set click listeners
                approveButton.setOnClickListener {
                    onApprove(post.postData.id)
                }

                rejectButton.setOnClickListener {
                    onReject(post.postData.id)
                }
            }
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
}
