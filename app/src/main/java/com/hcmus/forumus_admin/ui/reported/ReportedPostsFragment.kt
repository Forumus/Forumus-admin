package com.hcmus.forumus_admin.ui.reported

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.repository.PostRepository
import com.hcmus.forumus_admin.data.repository.UserRepository
import com.hcmus.forumus_admin.databinding.FragmentReportedPostsBinding
import kotlinx.coroutines.launch

data class ReportedPost(
    val id: String,
    val title: String,
    val author: String,
    val date: String,
    val categories: List<String>,
    val description: String,
    val violationCount: Int,
    val reportedCount: Int
)

class ReportedPostsFragment : Fragment() {

    private var _binding: FragmentReportedPostsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ReportedPostsAdapter
    private val postRepository = PostRepository()
    private val userRepository = UserRepository()
    private var allPosts: List<ReportedPost> = emptyList()
    private var filteredPosts: List<ReportedPost> = emptyList()
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportedPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupPostsList()
        setupSearchBar()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupPostsList() {
        adapter = ReportedPostsAdapter(
            posts = emptyList(),
            onDismissClick = { post -> showDismissConfirmation(post) },
            onDeleteClick = { post -> showDeleteConfirmation(post) }
        )
        
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@ReportedPostsFragment.adapter
        }
        
        loadReportedPostsFromFirebase()
    }

    private fun setupSearchBar() {
        binding.searchInput.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { applySearchFilter(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { applySearchFilter(it) }
                return true
            }
        })
        
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }
    }
    
    private fun applySearchFilter(query: String) {
        filteredPosts = if (query.isEmpty()) {
            allPosts
        } else {
            allPosts.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
        adapter.updatePosts(filteredPosts)
        
        if (filteredPosts.isEmpty() && query.isNotEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_results_found), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showSortDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sort, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent for rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Track selections
        var sortBy = "reports" // "reports" or "violations"
        var sortOrder = "asc" // "asc" or "desc"
        
        // Get all views
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val reportsButton = dialogView.findViewById<View>(R.id.reportsButton)
        val violationsButton = dialogView.findViewById<View>(R.id.violationsButton)
        val reportsIndicator = dialogView.findViewById<View>(R.id.reportsIndicator)
        val violationsIndicator = dialogView.findViewById<View>(R.id.violationsIndicator)
        val lowToHighButton = dialogView.findViewById<View>(R.id.lowToHighButton)
        val highToLowButton = dialogView.findViewById<View>(R.id.highToLowButton)
        val lowToHighIndicator = dialogView.findViewById<View>(R.id.lowToHighIndicator)
        val highToLowIndicator = dialogView.findViewById<View>(R.id.highToLowIndicator)
        val applyButton = dialogView.findViewById<View>(R.id.applyButton)
        
        // Close button
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Sort by selection
        reportsButton.setOnClickListener {
            sortBy = "reports"
            reportsButton.setBackgroundResource(R.drawable.bg_sort_option_selected)
            violationsButton.setBackgroundResource(R.drawable.bg_sort_option_unselected)
            reportsIndicator.visibility = View.VISIBLE
            violationsIndicator.visibility = View.GONE
        }
        
        violationsButton.setOnClickListener {
            sortBy = "violations"
            reportsButton.setBackgroundResource(R.drawable.bg_sort_option_unselected)
            violationsButton.setBackgroundResource(R.drawable.bg_sort_option_selected)
            reportsIndicator.visibility = View.GONE
            violationsIndicator.visibility = View.VISIBLE
        }
        
        // Sort order selection
        lowToHighButton.setOnClickListener {
            sortOrder = "asc"
            lowToHighButton.setBackgroundResource(R.drawable.bg_sort_order_selected)
            highToLowButton.setBackgroundResource(R.drawable.bg_sort_order_unselected)
            lowToHighIndicator.setBackgroundResource(R.drawable.bg_radio_order_selected)
            highToLowIndicator.setBackgroundResource(R.drawable.bg_radio_order_unselected)
        }
        
        highToLowButton.setOnClickListener {
            sortOrder = "desc"
            lowToHighButton.setBackgroundResource(R.drawable.bg_sort_order_unselected)
            highToLowButton.setBackgroundResource(R.drawable.bg_sort_order_selected)
            lowToHighIndicator.setBackgroundResource(R.drawable.bg_radio_order_unselected)
            highToLowIndicator.setBackgroundResource(R.drawable.bg_radio_order_selected)
        }
        
        // Apply button
        applyButton.setOnClickListener {
            when {
                sortBy == "reports" && sortOrder == "asc" -> sortByReportsAscending()
                sortBy == "reports" && sortOrder == "desc" -> sortByReportsDescending()
                sortBy == "violations" && sortOrder == "asc" -> sortByViolationsAscending()
                sortBy == "violations" && sortOrder == "desc" -> sortByViolationsDescending()
            }
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun sortByReportsAscending() {
        filteredPosts = filteredPosts.sortedBy { it.reportedCount }
        adapter.updatePosts(filteredPosts)
        Toast.makeText(requireContext(), getString(R.string.sorted_by_reports_asc), Toast.LENGTH_SHORT).show()
    }
    
    private fun sortByReportsDescending() {
        filteredPosts = filteredPosts.sortedByDescending { it.reportedCount }
        adapter.updatePosts(filteredPosts)
        Toast.makeText(requireContext(), getString(R.string.sorted_by_reports_desc), Toast.LENGTH_SHORT).show()
    }
    
    private fun sortByViolationsAscending() {
        filteredPosts = filteredPosts.sortedBy { it.violationCount }
        adapter.updatePosts(filteredPosts)
        Toast.makeText(requireContext(), getString(R.string.sorted_by_violations_asc), Toast.LENGTH_SHORT).show()
    }
    
    private fun sortByViolationsDescending() {
        filteredPosts = filteredPosts.sortedByDescending { it.violationCount }
        adapter.updatePosts(filteredPosts)
        Toast.makeText(requireContext(), getString(R.string.sorted_by_violations_desc), Toast.LENGTH_SHORT).show()
    }
    
    private fun showDismissConfirmation(post: ReportedPost) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dismiss_report))
            .setMessage(getString(R.string.confirm_dismiss_message, post.title))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                dismissPost(post)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showDeleteConfirmation(post: ReportedPost) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_post_title))
            .setMessage(getString(R.string.confirm_delete_post_message, post.title))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                deletePost(post)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun dismissPost(post: ReportedPost) {
        allPosts = allPosts.filter { it.id != post.id }
        applySearchFilter(binding.searchInput.query.toString())
        Toast.makeText(requireContext(), getString(R.string.report_dismissed), Toast.LENGTH_SHORT).show()
    }
    
    private fun deletePost(post: ReportedPost) {
        lifecycleScope.launch {
            try {
                val result = postRepository.deletePost(post.id)
                
                result.onSuccess {
                    // Remove from local list
                    allPosts = allPosts.filter { it.id != post.id }
                    applySearchFilter(binding.searchInput.query.toString())
                    Toast.makeText(requireContext(), getString(R.string.post_deleted), Toast.LENGTH_SHORT).show()
                }.onFailure { exception ->
                    Toast.makeText(
                        requireContext(), 
                        "Failed to delete post: ${exception.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(), 
                    "Error deleting post: ${e.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadReportedPostsFromFirebase() {
        if (isLoading) return
        
        isLoading = true
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val postsResult = postRepository.getAllPosts()
                val usersResult = userRepository.getAllUsers()
                
                postsResult.onSuccess { firestorePosts ->
                    usersResult.onSuccess { firestoreUsers ->
                        // Create a map of uid to user for quick lookup
                        val userMap = firestoreUsers.associateBy { it.uid }
                        
                        // Filter posts with report_count > 0
                        val reportedFirestorePosts = firestorePosts.filter { it.report_count > 0 }
                        
                        if (reportedFirestorePosts.isEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                "No reported posts found",
                                Toast.LENGTH_SHORT
                            ).show()
                            allPosts = emptyList()
                        } else {
                            // Convert Firestore posts to ReportedPost model
                            allPosts = reportedFirestorePosts.map { firestorePost ->
                                val author = userMap[firestorePost.authorId]?.fullName 
                                    ?: firestorePost.authorId
                                
                                ReportedPost(
                                    id = firestorePost.post_id,
                                    title = firestorePost.title.ifEmpty { "Untitled Post" },
                                    author = author,
                                    date = PostRepository.formatFirebaseTimestamp(firestorePost.createdAt),
                                    categories = firestorePost.topic,
                                    description = firestorePost.content.take(200),
                                    violationCount = firestorePost.violation_type.size,
                                    reportedCount = firestorePost.report_count.toInt()
                                )
                            }
                            
                            Toast.makeText(
                                requireContext(),
                                "Loaded ${allPosts.size} reported posts",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        
                        filteredPosts = allPosts
                        adapter.updatePosts(filteredPosts)
                    }.onFailure {
                        // Continue without user names
                        val reportedFirestorePosts = firestorePosts.filter { it.report_count > 0 }
                        
                        allPosts = reportedFirestorePosts.map { firestorePost ->
                            ReportedPost(
                                id = firestorePost.post_id,
                                title = firestorePost.title.ifEmpty { "Untitled Post" },
                                author = firestorePost.authorId,
                                date = PostRepository.formatFirebaseTimestamp(firestorePost.createdAt),
                                categories = firestorePost.topic,
                                description = firestorePost.content.take(200),
                                violationCount = firestorePost.violation_type.size,
                                reportedCount = firestorePost.report_count.toInt()
                            )
                        }
                        filteredPosts = allPosts
                        adapter.updatePosts(filteredPosts)
                    }
                }.onFailure { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Error loading posts: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    allPosts = emptyList()
                    filteredPosts = allPosts
                    adapter.updatePosts(filteredPosts)
                }
            } finally {
                isLoading = false
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.postsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
