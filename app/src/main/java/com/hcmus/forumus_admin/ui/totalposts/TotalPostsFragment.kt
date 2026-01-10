package com.hcmus.forumus_admin.ui.totalposts

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.data.model.Post
import com.hcmus.forumus_admin.data.model.Tag
import com.hcmus.forumus_admin.data.repository.PostRepository
import com.hcmus.forumus_admin.databinding.FragmentTotalPostsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TotalPostsFragment : Fragment() {

    private var _binding: FragmentTotalPostsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TotalPostsAdapter
    private val postRepository = PostRepository()
    private var allPosts: List<Post> = emptyList()
    private var filteredPosts: List<Post> = emptyList()
    private var currentPage = 0
    private val itemsPerPage = 10
    private var totalPages = 0
    private var isLoading = false

    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
    private val maxDaysRange = 31

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
        initializeDefaultDates()
        setupDatePickers()
        setupSearchBar()
        setupPagination()
        loadPostsFromFirebase()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun initializeDefaultDates() {
        val calendar = Calendar.getInstance()
        endDate = calendar.time
        binding.endDateInput.setText(dateFormat.format(endDate!!))

        // Set start date to 31 days before end date
        calendar.add(Calendar.DAY_OF_YEAR, -maxDaysRange)
        startDate = calendar.time
        binding.startDateInput.setText(dateFormat.format(startDate!!))

        // Update date range display
        binding.dateRangeText.text = "${dateFormat.format(startDate!!)} - ${dateFormat.format(endDate!!)}"
    }

    private fun setupRecyclerView() {
        adapter = TotalPostsAdapter { post ->
            // Navigate to post detail fragment with post ID
            val bundle = Bundle().apply {
                putString("postId", post.id)
            }
            findNavController().navigate(
                R.id.action_totalPostsFragment_to_postDetailFragment,
                bundle
            )
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

    private fun loadPostsFromFirebase() {
        if (isLoading) return
        
        isLoading = true
        showLoading(true)
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val postsResult = postRepository.getAllPosts()
                
                // Check if fragment is still added to activity
                if (!isAdded || _binding == null) return@launch
                
                postsResult.onSuccess { firestorePosts ->
                    if (firestorePosts.isEmpty()) {
                        context?.let {
                            Toast.makeText(it, "No posts found in database", Toast.LENGTH_SHORT).show()
                        }
                        allPosts = emptyList()
                    } else {
                        // Convert Firestore posts to Post model - use authorName directly from Firebase
                        allPosts = firestorePosts.map { firestorePost ->
                            Post(
                                id = firestorePost.post_id,
                                title = firestorePost.title.ifEmpty { "Untitled Post" },
                                author = firestorePost.authorName.ifEmpty { "Unknown Author" },
                                date = formatPostDate(firestorePost.createdAt),
                                description = firestorePost.content.take(200),
                                tags = firestorePost.topic.map { topicName ->
                                    Tag(
                                        name = topicName,
                                        backgroundColor = 0xFFE3F2FD.toInt(),
                                        textColor = 0xFF1976D2.toInt()
                                    )
                                },
                                isAiApproved = firestorePost.status == "approved"
                            )
                        }
                        
                        context?.let {
                            Toast.makeText(it, "Loaded ${allPosts.size} posts", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    filteredPosts = allPosts
                    if (isAdded && _binding != null) {
                        applyDateFilter()
                    }
                }.onFailure { exception ->
                    context?.let {
                        Toast.makeText(it, "Error loading posts: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                    
                    allPosts = emptyList()
                    filteredPosts = allPosts
                    if (isAdded && _binding != null) {
                        calculateTotalPages()
                        updateTotalCount()
                        updatePage()
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
            context?.let {
                Toast.makeText(it, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (start.after(end)) {
            context?.let {
                Toast.makeText(it, "Start date must be before end date", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Validate 31-day maximum range
        val daysDifference = calculateDaysDifference(start, end)
        if (daysDifference > maxDaysRange) {
            context?.let {
                Toast.makeText(it, "Date range cannot exceed $maxDaysRange days. Please select a shorter period.", Toast.LENGTH_LONG).show()
            }
            return
        }

        // Filter posts by date range
        isLoading = true
        showLoading(true)
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = postRepository.getAllPosts()
                
                // Check if fragment is still added to activity
                if (!isAdded || _binding == null) return@launch
                
                result.onSuccess { firestorePosts ->
                    // Filter posts by date range
                    val filteredFirestorePosts = firestorePosts.filter { post ->
                        val postDate = PostRepository.getFirebaseTimestampAsDate(post.createdAt)
                        postDate != null && !postDate.before(start) && !postDate.after(end)
                    }
                    
                    // Use authorName directly from Firebase
                    allPosts = filteredFirestorePosts.map { firestorePost ->
                        Post(
                            id = firestorePost.post_id,
                            title = firestorePost.title.ifEmpty { "Untitled Post" },
                            author = firestorePost.authorName.ifEmpty { "Unknown Author" },
                            date = formatPostDate(firestorePost.createdAt),
                            description = firestorePost.content.take(200),
                            tags = firestorePost.topic.map { topicName ->
                                Tag(
                                    name = topicName,
                                    backgroundColor = 0xFFE3F2FD.toInt(),
                                    textColor = 0xFF1976D2.toInt()
                                )
                            },
                            isAiApproved = firestorePost.status == "approved"
                        )
                    }
                    
                    filteredPosts = allPosts
                    if (isAdded && _binding != null) {
                        currentPage = 0
                        calculateTotalPages()
                        updateTotalCount()
                        updatePage()
                    }
                    
                    context?.let {
                        Toast.makeText(it, "Found ${allPosts.size} posts between ${dateFormat.format(start)} and ${dateFormat.format(end)}", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { exception ->
                    context?.let {
                        Toast.makeText(it, "Error filtering posts: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
                
                // Update date range display
                _binding?.dateRangeText?.text = "${dateFormat.format(start)} - ${dateFormat.format(end)}"
            } finally {
                isLoading = false
                showLoading(false)
            }
        }
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

    private fun calculateDaysDifference(startDate: Date, endDate: Date): Long {
        val diffInMillis = endDate.time - startDate.time
        return diffInMillis / (1000 * 60 * 60 * 24)
    }



    private fun formatPostDate(timestamp: Any?): String {
        return PostRepository.getFirebaseTimestampAsDate(timestamp)?.let { dateObj ->
            java.text.SimpleDateFormat(getString(R.string.post_date_format), java.util.Locale.getDefault()).format(dateObj)
        } ?: "Unknown date"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
