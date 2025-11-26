package com.anhkhoa.forumus_admin.ui.totalposts

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.anhkhoa.forumus_admin.R
import com.anhkhoa.forumus_admin.data.model.Post
import com.anhkhoa.forumus_admin.data.model.Tag
import com.anhkhoa.forumus_admin.databinding.FragmentTotalPostsBinding
import java.text.SimpleDateFormat
import java.util.*

class TotalPostsFragment : Fragment() {

    private var _binding: FragmentTotalPostsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TotalPostsAdapter
    private var allPosts: List<Post> = emptyList()
    private var filteredPosts: List<Post> = emptyList()
    private var currentPage = 0
    private val itemsPerPage = 10
    private var totalPages = 0

    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTotalPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupDatePickers()
        setupSearchBar()
        setupPagination()
        loadMockData()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = TotalPostsAdapter { post ->
            // Handle post click - navigate to post details or show info
            Toast.makeText(requireContext(), "Clicked: ${post.title}", Toast.LENGTH_SHORT).show()
        }

        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@TotalPostsFragment.adapter
        }
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        binding.startDateInput.setOnClickListener {
            showDatePicker(calendar) { selectedDate ->
                startDate = selectedDate
                binding.startDateInput.setText(dateFormat.format(selectedDate))
            }
        }

        binding.endDateInput.setOnClickListener {
            showDatePicker(calendar) { selectedDate ->
                endDate = selectedDate
                binding.endDateInput.setText(dateFormat.format(selectedDate))
            }
        }

        binding.applyFilterButton.setOnClickListener {
            applyDateFilter()
        }
    }

    private fun showDatePicker(calendar: Calendar, onDateSelected: (Date) -> Unit) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(calendar.time)
        }, year, month, day).show()
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
    }

    private fun setupPagination() {
        binding.prevPageButton.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                updatePage()
            }
        }

        binding.nextPageButton.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                updatePage()
            }
        }
    }

    private fun loadMockData() {
        allPosts = generateMockPosts()
        filteredPosts = allPosts
        calculateTotalPages()
        updateTotalCount()
        updatePage()
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

        currentPage = 0
        calculateTotalPages()
        updateTotalCount()
        updatePage()
    }

    private fun applyDateFilter() {
        val start = startDate
        val end = endDate

        if (start == null || end == null) {
            Toast.makeText(requireContext(), "Please select both start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        if (start.after(end)) {
            Toast.makeText(requireContext(), "Start date must be before end date", Toast.LENGTH_SHORT).show()
            return
        }

        // In a real app, you would filter by actual post dates
        // For mock data, we'll just show a toast
        Toast.makeText(
            requireContext(),
            "Filter applied: ${dateFormat.format(start)} to ${dateFormat.format(end)}",
            Toast.LENGTH_SHORT
        ).show()

        // Update date range display
        binding.dateRangeText.text = "${dateFormat.format(start)} - ${dateFormat.format(end)}"
    }

    private fun calculateTotalPages() {
        totalPages = if (filteredPosts.isEmpty()) {
            1
        } else {
            (filteredPosts.size + itemsPerPage - 1) / itemsPerPage
        }
    }

    private fun getCurrentPagePosts(): List<Post> {
        val startIndex = currentPage * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, filteredPosts.size)
        return if (startIndex < filteredPosts.size) {
            filteredPosts.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    private fun updatePage() {
        adapter.updatePosts(getCurrentPagePosts())
        updatePaginationUI()
        binding.postsRecyclerView.scrollToPosition(0)
    }

    private fun updatePaginationUI() {
        binding.pageIndicatorText.text = "${currentPage + 1} / $totalPages"

        binding.prevPageButton.apply {
            isEnabled = currentPage > 0
            alpha = if (isEnabled) 1f else 0.5f
            background = if (isEnabled) {
                context.getDrawable(R.drawable.bg_pagination_button_active)
            } else {
                context.getDrawable(R.drawable.bg_pagination_button)
            }
        }

        binding.nextPageButton.apply {
            isEnabled = currentPage < totalPages - 1
            alpha = if (isEnabled) 1f else 0.5f
            background = if (isEnabled) {
                context.getDrawable(R.drawable.bg_pagination_button_active)
            } else {
                context.getDrawable(R.drawable.bg_pagination_button)
            }
        }
    }

    private fun updateTotalCount() {
        binding.totalPostCountText.text = filteredPosts.size.toString()
    }

    private fun generateMockPosts(): List<Post> {
        val posts = mutableListOf<Post>()

        // Sample tags
        val techTags = listOf(
            Tag("Information Technology", 0xFFE3F2FD.toInt(), 0xFF1976D2.toInt()),
            Tag("Software Engineering", 0xFFE1F5FE.toInt(), 0xFF0288D1.toInt()),
            Tag("Web Development", 0xFF155DFC.toInt(), 0xFFFFFFFF.toInt())
        )

        val mathTags = listOf(
            Tag("Mathematics", 0xFFF3E5F5.toInt(), 0xFF7B1FA2.toInt()),
            Tag("Pure Mathematics", 0xFFFCE4EC.toInt(), 0xFFC2185B.toInt()),
            Tag("Algebra", 0xFFFFF3E0.toInt(), 0xFFE65100.toInt())
        )

        val scienceTags = listOf(
            Tag("Science", 0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt()),
            Tag("Physics", 0xFFE0F2F1.toInt(), 0xFF00695C.toInt()),
            Tag("Chemistry", 0xFFFFF9C4.toInt(), 0xFFF57F17.toInt())
        )

        val businessTags = listOf(
            Tag("Business", 0xFFEFEBE9.toInt(), 0xFF4E342E.toInt()),
            Tag("Marketing", 0xFFF1F8E9.toInt(), 0xFF558B2F.toInt()),
            Tag("Finance", 0xFFE8EAF6.toInt(), 0xFF283593.toInt())
        )

        // Generate 25 mock posts
        val titles = listOf(
            "Misleading Information about React",
            "Best Practices for TypeScript Development",
            "Understanding Machine Learning Algorithms",
            "Introduction to Quantum Computing",
            "Advanced CSS Techniques for Modern Web",
            "Database Optimization Strategies",
            "Mobile App Development with Flutter",
            "Cloud Computing Architecture Patterns",
            "Cybersecurity Best Practices",
            "Data Structures and Algorithms Guide",
            "Introduction to Linear Algebra",
            "Calculus Fundamentals Explained",
            "Statistics for Data Science",
            "Number Theory and Cryptography",
            "Graph Theory Applications",
            "Introduction to Thermodynamics",
            "Quantum Mechanics Basics",
            "Organic Chemistry Overview",
            "Classical Mechanics Principles",
            "Electromagnetism Fundamentals",
            "Digital Marketing Strategies",
            "Business Analytics and Intelligence",
            "Financial Management Principles",
            "Entrepreneurship and Innovation",
            "Supply Chain Management"
        )

        val authors = listOf(
            "John Doe", "Jane Smith", "Mike Johnson", "Sarah Wilson", "David Brown",
            "Emily Davis", "Alex Martinez", "Lisa Anderson", "Tom White", "Rachel Green"
        )

        val descriptions = listOf(
            "This article contains comprehensive information about best practices and promotes modern patterns that improve application performance.",
            "An in-depth guide covering essential concepts and practical examples for beginners and intermediate learners.",
            "Detailed explanation of fundamental principles with real-world applications and case studies.",
            "A thorough exploration of key topics with step-by-step instructions and helpful illustrations.",
            "Complete tutorial covering everything from basic concepts to advanced techniques with code examples."
        )

        for (i in 0 until 25) {
            val tagSet = when (i % 4) {
                0 -> techTags
                1 -> mathTags
                2 -> scienceTags
                else -> businessTags
            }

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)

            posts.add(
                Post(
                    id = "post_$i",
                    title = titles[i],
                    author = authors[i % authors.size],
                    date = displayDateFormat.format(calendar.time),
                    description = descriptions[i % descriptions.size],
                    tags = tagSet.take(2 + (i % 2))
                )
            )
        }

        return posts
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
