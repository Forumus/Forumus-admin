package com.anhkhoa.forumus_admin.ui.totalusers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.anhkhoa.forumus_admin.R
import com.anhkhoa.forumus_admin.data.model.User
import com.anhkhoa.forumus_admin.data.model.UserStatus
import com.anhkhoa.forumus_admin.databinding.FragmentTotalUsersBinding

class TotalUsersFragment : Fragment() {

    private var _binding: FragmentTotalUsersBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TotalUsersAdapter
    private var allUsers: List<User> = emptyList()
    private var filteredUsers: List<User> = emptyList()
    private var currentPage = 0
    private val itemsPerPage = 10
    private var totalPages = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTotalUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearchBar()
        setupPagination()
        loadMockData()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.moreButton.setOnClickListener {
            // TODO: Implement more options menu
            Toast.makeText(requireContext(), "More options coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = TotalUsersAdapter { user ->
            showUserDetailsDialog(user)
        }

        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@TotalUsersFragment.adapter
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
            // TODO: Implement filter options
            Toast.makeText(requireContext(), "Filter options coming soon", Toast.LENGTH_SHORT).show()
        }
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
        allUsers = generateMockUsers()
        filteredUsers = allUsers
        calculateTotalPages()
        updatePage()
    }

    private fun generateMockUsers(): List<User> {
        val users = mutableListOf<User>()
        
        val names = listOf(
            "Cao Trong Khang", "Tô Thanh Long", "Nguyễn Viết Toàn", "Trần Anh Khoa",
            "Võ Tất Toàn", "Nguyễn Văn A", "Nguyễn Văn B", "Nguyễn Văn C",
            "Trần Thị D", "Lê Văn E", "Phạm Minh F", "Hoàng Thị G",
            "Đặng Văn H", "Bùi Thị I", "Vũ Văn K", "Lý Thị L",
            "Phan Văn M", "Đinh Thị N", "Mai Văn O", "Trương Thị P"
        )
        
        val statuses = listOf(
            UserStatus.WARNING, UserStatus.BAN, UserStatus.REMIND, UserStatus.WARNING,
            UserStatus.BAN, UserStatus.REMIND, UserStatus.NORMAL, UserStatus.WARNING,
            UserStatus.REMIND, UserStatus.NORMAL, UserStatus.WARNING, UserStatus.BAN,
            UserStatus.NORMAL, UserStatus.REMIND, UserStatus.WARNING, UserStatus.NORMAL,
            UserStatus.BAN, UserStatus.REMIND, UserStatus.NORMAL, UserStatus.WARNING
        )
        
        for (i in names.indices) {
            val id = String.format("231201%02d", 32 + i)
            users.add(
                User(
                    id = id,
                    name = names[i],
                    avatarUrl = null,
                    status = statuses[i % statuses.size]
                )
            )
        }
        
        return users
    }

    private fun applySearchFilter(query: String) {
        filteredUsers = if (query.isEmpty()) {
            allUsers
        } else {
            allUsers.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.id.contains(query, ignoreCase = true)
            }
        }

        currentPage = 0
        calculateTotalPages()
        adapter.updateUsers(getCurrentPageUsers())
        updatePaginationUI()

        if (filteredUsers.isEmpty() && query.isNotEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_results_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateTotalPages() {
        totalPages = if (filteredUsers.isEmpty()) {
            1
        } else {
            (filteredUsers.size + itemsPerPage - 1) / itemsPerPage
        }
    }

    private fun getCurrentPageUsers(): List<User> {
        val startIndex = currentPage * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, filteredUsers.size)
        return if (startIndex < filteredUsers.size) {
            filteredUsers.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    private fun updatePage() {
        adapter.updateUsers(getCurrentPageUsers())
        updatePaginationUI()
        binding.usersRecyclerView.scrollToPosition(0)
    }

    private fun showUserDetailsDialog(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_details, null)
        
        // Find views
        val userAvatar = dialogView.findViewById<ImageView>(R.id.dialogUserAvatar)
        val userName = dialogView.findViewById<TextView>(R.id.dialogUserName)
        val userId = dialogView.findViewById<TextView>(R.id.dialogUserId)
        val userStatus = dialogView.findViewById<TextView>(R.id.dialogUserStatus)
        val userRole = dialogView.findViewById<TextView>(R.id.dialogUserRole)
        val userCreatedAt = dialogView.findViewById<TextView>(R.id.dialogUserCreatedAt)
        val closeButton = dialogView.findViewById<ImageView>(R.id.closeButton)
        
        // Set user data
        userName.text = user.name
        userId.text = user.id
        userRole.text = user.role
        userCreatedAt.text = user.createdAt
        
        // Set avatar
        if (user.avatarUrl.isNullOrEmpty()) {
            userAvatar.setImageResource(R.drawable.ic_default_avatar)
        }
        
        // Set status badge
        when (user.status) {
            UserStatus.BAN -> {
                userStatus.text = getString(R.string.ban)
                userStatus.setBackgroundResource(R.drawable.bg_badge_ban)
                userStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger_red))
            }
            UserStatus.WARNING -> {
                userStatus.text = getString(R.string.warning)
                userStatus.setBackgroundResource(R.drawable.bg_badge_warning)
                userStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_orange))
            }
            UserStatus.REMIND -> {
                userStatus.text = getString(R.string.remind)
                userStatus.setBackgroundResource(R.drawable.bg_badge_remind)
                userStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
            }
            UserStatus.NORMAL -> {
                userStatus.text = getString(R.string.normal)
                userStatus.setBackgroundResource(R.drawable.bg_badge_normal)
                userStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
            }
        }
        
        // Create and show dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Set transparent background to show rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Set close button click listener
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
