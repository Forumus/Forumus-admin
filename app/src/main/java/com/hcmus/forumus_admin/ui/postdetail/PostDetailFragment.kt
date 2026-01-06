package com.hcmus.forumus_admin.ui.postdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.repository.PostRepository
import com.hcmus.forumus_admin.data.repository.FirestorePost
import com.hcmus.forumus_admin.databinding.FragmentPostDetailBinding
import kotlinx.coroutines.launch

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    
    private val postRepository = PostRepository()
    private var postId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        
        // Get post ID from arguments
        postId = arguments?.getString("postId")
        
        if (postId != null) {
            loadPostDetails(postId!!)
        } else {
            showError("Post ID not provided")
        }
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadPostDetails(postId: String) {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.contentScrollView.visibility = View.GONE
        
        lifecycleScope.launch {
            val result = postRepository.getAllPosts()
            
            result.fold(
                onSuccess = { posts ->
                    val post = posts.find { it.post_id == postId || it.uid == postId }
                    if (post != null) {
                        displayPostDetails(post)
                    } else {
                        showError("Post not found")
                    }
                },
                onFailure = { exception ->
                    showError("Failed to load post: ${exception.message}")
                }
            )
            
            binding.loadingIndicator.visibility = View.GONE
            binding.contentScrollView.visibility = View.VISIBLE
        }
    }

    private fun displayPostDetails(post: FirestorePost) {
        // Post title
        binding.postTitle.text = post.title
        
        // Author information
        binding.authorName.text = post.authorName
        binding.authorId.text = post.authorId
        
        // Load author avatar if available
        if (!post.author_avatar_url.isNullOrEmpty()) {
            Glide.with(this)
                .load(post.author_avatar_url)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.authorAvatar)
        }
        
        // Date
        binding.postDate.text = PostRepository.formatFirebaseTimestamp(post.createdAt)
        
        // Categories/Topics
        if (post.topic.isNotEmpty()) {
            binding.postCategories.text = post.topic.joinToString(" › ")
        } else {
            binding.postCategories.text = "Uncategorized"
        }
        
        // Post content
        binding.postContent.text = post.content
        
        // Post statistics
        binding.upvoteCount.text = post.upvote_count.toString()
        binding.downvoteCount.text = post.downvote_count.toString()
        binding.commentCount.text = post.comment_count.toString()
        
        // Post ID
        binding.postId.text = post.post_id
        
        // Status information
        binding.postStatus.text = post.status.uppercase()
        
        // Set status color based on status
        val statusColor = when (post.status.uppercase()) {
            "APPROVED" -> R.color.success_green
            "REJECTED", "DELETED" -> R.color.danger_red
            "PENDING" -> R.color.warning_orange
            else -> R.color.text_primary
        }
        binding.postStatus.setTextColor(resources.getColor(statusColor, null))
        
        // Report count
        if (post.reportCount > 0) {
            binding.reportCountLayout.visibility = View.VISIBLE
            val reportText = "${post.reportCount} report${if (post.reportCount != 1L) "s" else ""}"
            binding.reportCount.text = reportText
        } else {
            binding.reportCountLayout.visibility = View.GONE
        }
        
        // Violation types
        if (post.violation_type.isNotEmpty()) {
            binding.violationTypesLayout.visibility = View.VISIBLE
            binding.violationTypes.text = post.violation_type.joinToString(", ")
        } else {
            binding.violationTypesLayout.visibility = View.GONE
        }
        
        // Display images if available
        if (post.image_link.isNotEmpty()) {
            binding.imagesContainer.visibility = View.VISIBLE
            binding.imagesContainer.removeAllViews()
            
            post.image_link.forEach { imageUrl ->
                val imageView = com.google.android.material.imageview.ShapeableImageView(requireContext()).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.chart_height)
                    ).apply {
                        bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
                    }
                    shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                        .setAllCornerSizes(resources.getDimension(R.dimen.corner_radius_medium))
                        .build()
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                }
                
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.color.background_gray)
                    .error(R.color.background_gray)
                    .into(imageView)
                
                binding.imagesContainer.addView(imageView)
            }
        } else {
            binding.imagesContainer.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.loadingIndicator.visibility = View.GONE
        binding.contentScrollView.visibility = View.VISIBLE
        
        // You can show a toast or set an error message in the UI
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
        
        // Optionally, navigate back
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

