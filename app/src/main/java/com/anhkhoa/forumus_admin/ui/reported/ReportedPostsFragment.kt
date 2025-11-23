package com.anhkhoa.forumus_admin.ui.reported

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.anhkhoa.forumus_admin.R
import com.anhkhoa.forumus_admin.databinding.FragmentReportedPostsBinding

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
    private var allPosts: List<ReportedPost> = emptyList()
    private var filteredPosts: List<ReportedPost> = emptyList()

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
        allPosts = getSampleReportedPosts()
        filteredPosts = allPosts
        
        adapter = ReportedPostsAdapter(
            posts = filteredPosts,
            onDismissClick = { post -> showDismissConfirmation(post) },
            onDeleteClick = { post -> showDeleteConfirmation(post) }
        )
        
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@ReportedPostsFragment.adapter
        }
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
        
        binding.filterButton.setOnClickListener {
            Toast.makeText(requireContext(), "Filter options coming soon", Toast.LENGTH_SHORT).show()
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
        allPosts = allPosts.filter { it.id != post.id }
        applySearchFilter(binding.searchInput.query.toString())
        Toast.makeText(requireContext(), getString(R.string.post_deleted), Toast.LENGTH_SHORT).show()
    }

    private fun getSampleReportedPosts(): List<ReportedPost> {
        return listOf(
            ReportedPost(
                id = "1",
                title = "Spam Advertisement for Math Courses",
                author = "Sarah Wilson",
                date = "November 15, 2025",
                categories = listOf("Mathematics", "Pure Mathematics", "Algebra"),
                description = "This post is pure spam advertising paid courses with no educational value and multiple affiliate links.",
                violationCount = 2,
                reportCount = 20
            ),
            ReportedPost(
                id = "2",
                title = "Plagiarized Machine Learning Research",
                author = "Mike Johnson",
                date = "November 16, 2025",
                categories = listOf("Information Technology", "Artificial Intelligence", "Machine Learning"),
                description = "This post is directly copied from published research papers without proper attribution or permission from the original authors.",
                violationCount = 2,
                reportCount = 15
            ),
            ReportedPost(
                id = "3",
                title = "Misleading Information about React",
                author = "John Doe",
                date = "November 18, 2025",
                categories = listOf("Information Technology", "Software Engineering", "Web Development"),
                description = "This article contains misleading information about React best practices and promotes outdated patterns that could harm application performance.",
                violationCount = 3,
                reportCount = 12
            ),
            ReportedPost(
                id = "4",
                title = "Offensive Language in TypeScript Tutorial",
                author = "Jane Smith",
                date = "November 17, 2025",
                categories = listOf("Information Technology", "Software Engineering", "Programming Languages"),
                description = "Tutorial contains inappropriate language and offensive comments that violate community guidelines and create a hostile environment.",
                violationCount = 2,
                reportCount = 8
            ),
            ReportedPost(
                id = "5",
                title = "Inappropriate Calculus Examples",
                author = "Tom Brown",
                date = "November 14, 2025",
                categories = listOf("Mathematics", "Applied Mathematics", "Calculus"),
                description = "Contains offensive imagery and inappropriate examples that violate community standards.",
                violationCount = 2,
                reportCount = 6
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
