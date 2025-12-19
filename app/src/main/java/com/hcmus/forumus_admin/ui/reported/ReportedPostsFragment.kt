package com.hcmus.forumus_admin.ui.reported

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.repository.PostRepository
import com.hcmus.forumus_admin.data.repository.ReportRepository
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
    val reportCount: Int
)

class ReportedPostsFragment : Fragment() {

    private var _binding: FragmentReportedPostsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ReportedPostsAdapter
    private val postRepository = PostRepository()
    private val reportRepository = ReportRepository()
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
            onItemClick = { post -> showReportDetailsDialog(post) },
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
        filteredPosts = filteredPosts.sortedBy { it.reportCount }
        adapter.updatePosts(filteredPosts)
        Toast.makeText(requireContext(), getString(R.string.sorted_by_reports_asc), Toast.LENGTH_SHORT).show()
    }
    
    private fun sortByReportsDescending() {
        filteredPosts = filteredPosts.sortedByDescending { it.reportCount }
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
        viewLifecycleOwner.lifecycleScope.launch {
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
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val postsResult = postRepository.getAllPosts()
                val usersResult = userRepository.getAllUsers()
                
                // Check if fragment is still added to activity
                if (!isAdded || _binding == null) return@launch
                
                postsResult.onSuccess { firestorePosts ->
                    usersResult.onSuccess { firestoreUsers ->
                        // Create a map of uid to user for quick lookup
                        val userMap = firestoreUsers.associateBy { it.uid }
                        
                        // Filter posts with reportCount > 0
                        val reportedFirestorePosts = firestorePosts.filter { it.reportCount > 0 }
                        
                        if (reportedFirestorePosts.isEmpty()) {
                            context?.let {
                                Toast.makeText(it, "No reported posts found", Toast.LENGTH_SHORT).show()
                            }
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
                                    reportCount = firestorePost.reportCount.toInt()
                                )
                            }
                            
                            context?.let {
                                Toast.makeText(it, "Loaded ${allPosts.size} reported posts", Toast.LENGTH_SHORT).show()
                            }
                        }
                        
                        filteredPosts = allPosts
                        if (isAdded && _binding != null) {
                            adapter.updatePosts(filteredPosts)
                        }
                    }.onFailure {
                        // Continue without user names
                        val reportedFirestorePosts = firestorePosts.filter { it.reportCount > 0 }
                        
                        allPosts = reportedFirestorePosts.map { firestorePost ->
                            ReportedPost(
                                id = firestorePost.post_id,
                                title = firestorePost.title.ifEmpty { "Untitled Post" },
                                author = firestorePost.authorId,
                                date = PostRepository.formatFirebaseTimestamp(firestorePost.createdAt),
                                categories = firestorePost.topic,
                                description = firestorePost.content.take(200),
                                violationCount = firestorePost.violation_type.size,
                                reportCount = firestorePost.reportCount.toInt()
                            )
                        }
                        filteredPosts = allPosts
                        if (isAdded && _binding != null) {
                            adapter.updatePosts(filteredPosts)
                        }
                    }
                }.onFailure { exception ->
                    context?.let {
                        Toast.makeText(it, "Error loading posts: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                    
                    allPosts = emptyList()
                    filteredPosts = allPosts
                    if (isAdded && _binding != null) {
                        adapter.updatePosts(filteredPosts)
                    }
                }
            } finally {
                isLoading = false
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        _binding?.let { binding ->
            binding.postsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
            binding.loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        }
    }
    
    private fun showReportDetailsDialog(post: ReportedPost) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_report_details, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent for rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Get views
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val loadingIndicator = dialogView.findViewById<ProgressBar>(R.id.loadingIndicator)
        val contentContainer = dialogView.findViewById<View>(R.id.contentContainer)
        val errorText = dialogView.findViewById<TextView>(R.id.errorText)
        val userReportText = dialogView.findViewById<TextView>(R.id.userReportText)
        val violationsContainer = dialogView.findViewById<LinearLayout>(R.id.violationsContainer)
        
        // Close button
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Show loading state
        loadingIndicator.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE
        errorText.visibility = View.GONE
        
        // Show dialog first
        dialog.show()
        
        // Load report data from Firebase
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val reportsResult = reportRepository.getReportsForPost(post.id)
                val usersResult = userRepository.getAllUsers()
                
                reportsResult.onSuccess { reports ->
                    usersResult.onSuccess { users ->
                        // Create a map of uid to user for quick lookup
                        val userMap = users.associateBy { it.uid }
                        
                        if (reports.isEmpty()) {
                            // No reports found
                            loadingIndicator.visibility = View.GONE
                            contentContainer.visibility = View.GONE
                            errorText.visibility = View.VISIBLE
                            errorText.text = "No reports found for this post"
                        } else {
                            // Build report summary
                            val reportCount = reports.size
                            val reportText = StringBuilder("This post was reported by $reportCount user${if (reportCount != 1) "s" else ""}:\n\n")
                            
                            // Clear and populate violations container with user-specific reports
                            violationsContainer.removeAllViews()
                            
                            reports.forEachIndexed { index, report ->
                                val userName = userMap[report.authorId]?.fullName ?: report.authorId
                                val violationName = report.descriptionViolation.name.ifEmpty { report.nameViolation }
                                val violationDescription = report.descriptionViolation.description
                                
                                // Create a card-like view for each report
                                val reportCard = LinearLayout(requireContext()).apply {
                                    orientation = LinearLayout.VERTICAL
                                    setPadding(16, 12, 16, 12)
                                    setBackgroundResource(R.drawable.bg_sort_option_unselected)
                                    layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        if (index > 0) topMargin = 12
                                    }
                                }
                                
                                // User name
                                val userText = TextView(requireContext()).apply {
                                    text = "• $userName"
                                    setTextColor(resources.getColor(R.color.text_primary, null))
                                    textSize = 14f
                                    setTypeface(null, android.graphics.Typeface.BOLD)
                                    setPadding(0, 0, 0, 8)
                                }
                                reportCard.addView(userText)
                                
                                // Violation name
                                if (violationName.isNotEmpty()) {
                                    val violationNameText = TextView(requireContext()).apply {
                                        text = "Violation: $violationName"
                                        setTextColor(resources.getColor(R.color.danger_red, null))
                                        textSize = 13f
                                        setTypeface(null, android.graphics.Typeface.BOLD)
                                        setPadding(0, 0, 0, 4)
                                    }
                                    reportCard.addView(violationNameText)
                                }
                                
                                // Violation description
                                if (violationDescription.isNotEmpty()) {
                                    val violationDescText = TextView(requireContext()).apply {
                                        text = violationDescription
                                        setTextColor(resources.getColor(R.color.text_secondary, null))
                                        textSize = 12f
                                    }
                                    reportCard.addView(violationDescText)
                                }
                                
                                violationsContainer.addView(reportCard)
                            }
                            
                            userReportText.text = "This post was reported by $reportCount user${if (reportCount != 1) "s" else ""}"
                            
                            // Show content
                            loadingIndicator.visibility = View.GONE
                            contentContainer.visibility = View.VISIBLE
                            errorText.visibility = View.GONE
                        }
                    }.onFailure {
                        // Continue without user names
                        val reportCount = reports.size
                        userReportText.text = "$reportCount user${if (reportCount != 1) "s" else ""} reported this post"
                        
                        // Clear and populate violations container
                        violationsContainer.removeAllViews()
                        
                        reports.forEachIndexed { index, report ->
                            val userName = report.authorId
                            val violationName = report.descriptionViolation.name.ifEmpty { report.nameViolation }
                            val violationDescription = report.descriptionViolation.description
                            
                            // Create a card-like view for each report
                            val reportCard = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.VERTICAL
                                setPadding(16, 12, 16, 12)
                                setBackgroundResource(R.drawable.bg_sort_option_unselected)
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    if (index > 0) topMargin = 12
                                }
                            }
                            
                            // User ID
                            val userText = TextView(requireContext()).apply {
                                text = "• User: $userName"
                                setTextColor(resources.getColor(R.color.text_primary, null))
                                textSize = 14f
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                setPadding(0, 0, 0, 8)
                            }
                            reportCard.addView(userText)
                            
                            // Violation name
                            if (violationName.isNotEmpty()) {
                                val violationNameText = TextView(requireContext()).apply {
                                    text = "Violation: $violationName"
                                    setTextColor(resources.getColor(R.color.danger_red, null))
                                    textSize = 13f
                                    setTypeface(null, android.graphics.Typeface.BOLD)
                                    setPadding(0, 0, 0, 4)
                                }
                                reportCard.addView(violationNameText)
                            }
                            
                            // Violation description
                            if (violationDescription.isNotEmpty()) {
                                val violationDescText = TextView(requireContext()).apply {
                                    text = violationDescription
                                    setTextColor(resources.getColor(R.color.text_secondary, null))
                                    textSize = 12f
                                }
                                reportCard.addView(violationDescText)
                            }
                            
                            violationsContainer.addView(reportCard)
                        }
                        
                        // Show content
                        loadingIndicator.visibility = View.GONE
                        contentContainer.visibility = View.VISIBLE
                        errorText.visibility = View.GONE
                    }
                }.onFailure { exception ->
                    // Show error
                    loadingIndicator.visibility = View.GONE
                    contentContainer.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = "Failed to load report details: ${exception.message}"
                }
            } catch (e: Exception) {
                // Show error
                loadingIndicator.visibility = View.GONE
                contentContainer.visibility = View.GONE
                errorText.visibility = View.VISIBLE
                errorText.text = "Error: ${e.message}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
