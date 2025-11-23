package com.anhkhoa.forumus_admin.ui.blacklist

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
import com.anhkhoa.forumus_admin.databinding.FragmentBlacklistBinding

data class BlacklistedUser(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val status: UserStatus
)

enum class UserStatus {
    BAN,
    WARNING,
    REMIND
}

enum class ActionType {
    REMOVE,
    BAN,
    WARNING,
    REMIND
}

class BlacklistFragment : Fragment() {

    private var _binding: FragmentBlacklistBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: BlacklistAdapter
    private var allUsers: List<BlacklistedUser> = emptyList()
    private var filteredUsers: List<BlacklistedUser> = emptyList()
    private var currentPage = 0
    private val itemsPerPage = 15
    private var totalPages = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlacklistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupUsersList()
        setupSearchBar()
        setupPagination()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.moreButton.setOnClickListener {
            // TODO: Implement more options menu
        }
    }

    private fun setupUsersList() {
        // Sample data - Replace this with data from your database
        allUsers = getSampleBlacklistedUsers()
        filteredUsers = allUsers
        calculateTotalPages()
        
        // Set up RecyclerView
        adapter = BlacklistAdapter(
            users = getCurrentPageUsers(),
            onRemoveClick = { user -> showConfirmationDialog(user, ActionType.REMOVE) },
            onStatusClick = { user -> showStatusActionMenu(user) }
        )
        
        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@BlacklistFragment.adapter
        }
    }
    
    private fun showStatusActionMenu(user: BlacklistedUser) {
        val options = arrayOf(
            getString(R.string.ban),
            getString(R.string.warning),
            getString(R.string.remind)
        )
        
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.change_status))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showConfirmationDialog(user, ActionType.BAN)
                    1 -> showConfirmationDialog(user, ActionType.WARNING)
                    2 -> showConfirmationDialog(user, ActionType.REMIND)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showConfirmationDialog(user: BlacklistedUser, actionType: ActionType) {
        val (title, message) = when (actionType) {
            ActionType.REMOVE -> Pair(
                getString(R.string.confirm_remove_title),
                getString(R.string.confirm_remove_message, user.name)
            )
            ActionType.BAN -> Pair(
                getString(R.string.confirm_ban_title),
                getString(R.string.confirm_ban_message, user.name)
            )
            ActionType.WARNING -> Pair(
                getString(R.string.confirm_warning_title),
                getString(R.string.confirm_warning_message, user.name)
            )
            ActionType.REMIND -> Pair(
                getString(R.string.confirm_remind_title),
                getString(R.string.confirm_remind_message, user.name)
            )
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                performAction(user, actionType)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun performAction(user: BlacklistedUser, actionType: ActionType) {
        // TODO: Implement actual backend call here
        when (actionType) {
            ActionType.REMOVE -> {
                // Remove user from blacklist
                allUsers = allUsers.filter { it.id != user.id }
                applySearchFilter(binding.searchInput.query.toString())
                // Adjust current page if needed
                if (getCurrentPageUsers().isEmpty() && currentPage > 0) {
                    currentPage--
                }
                updatePaginationUI()
                Toast.makeText(requireContext(), "${user.name} removed from blacklist", Toast.LENGTH_SHORT).show()
            }
            ActionType.BAN -> {
                // Update user status to BAN
                updateUserStatus(user, UserStatus.BAN)
                Toast.makeText(requireContext(), "${user.name} has been banned", Toast.LENGTH_SHORT).show()
            }
            ActionType.WARNING -> {
                // Update user status to WARNING
                updateUserStatus(user, UserStatus.WARNING)
                Toast.makeText(requireContext(), "Warning sent to ${user.name}", Toast.LENGTH_SHORT).show()
            }
            ActionType.REMIND -> {
                // Update user status to REMIND
                updateUserStatus(user, UserStatus.REMIND)
                Toast.makeText(requireContext(), "Reminder sent to ${user.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateUserStatus(user: BlacklistedUser, newStatus: UserStatus) {
        allUsers = allUsers.map {
            if (it.id == user.id) it.copy(status = newStatus) else it
        }
        applySearchFilter(binding.searchInput.query.toString())
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
            // TODO: Implement filter options (by status, date, etc.)
            Toast.makeText(requireContext(), "Filter options coming soon", Toast.LENGTH_SHORT).show()
        }
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
        
        // Reset to first page when filtering
        currentPage = 0
        calculateTotalPages()
        adapter.updateUsers(getCurrentPageUsers())
        updatePaginationUI()
        
        // Show a message if no results found
        if (filteredUsers.isEmpty() && query.isNotEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_results_found), Toast.LENGTH_SHORT).show()
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
        
        updatePaginationUI()
    }
    
    private fun calculateTotalPages() {
        totalPages = if (filteredUsers.isEmpty()) {
            1
        } else {
            (filteredUsers.size + itemsPerPage - 1) / itemsPerPage
        }
    }
    
    private fun getCurrentPageUsers(): List<BlacklistedUser> {
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
    
    private fun updatePaginationUI() {
        // Update previous button state
        binding.prevPageButton.apply {
            isEnabled = currentPage > 0
            alpha = if (isEnabled) 1f else 0.5f
            background = if (isEnabled) {
                context.getDrawable(R.drawable.bg_pagination_button_active)
            } else {
                context.getDrawable(R.drawable.bg_pagination_button)
            }
        }
        
        // Update next button state
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
    
    private fun getSampleBlacklistedUsers(): List<BlacklistedUser> {
        return listOf(
            // Page 1
            BlacklistedUser("23120132", "Cao Trọng Khang", null, UserStatus.WARNING),
            BlacklistedUser("23120143", "Tô Thành Long", null, UserStatus.BAN),
            BlacklistedUser("23120096", "Nguyễn Viết Toàn", null, UserStatus.REMIND),
            BlacklistedUser("23120135", "Trần Anh Khoa", null, UserStatus.WARNING),
            BlacklistedUser("23120097", "Võ Tất Toàn", null, UserStatus.BAN),
            BlacklistedUser("23120021", "Nguyễn Văn A", null, UserStatus.REMIND),
            BlacklistedUser("23120045", "Lê Hoàng Nam", null, UserStatus.WARNING),
            BlacklistedUser("23120078", "Phạm Minh Tuấn", null, UserStatus.BAN),
            BlacklistedUser("23120089", "Đặng Quốc Huy", null, UserStatus.REMIND),
            BlacklistedUser("23120101", "Hoàng Văn Đức", null, UserStatus.WARNING),
            
            // Page 2
            BlacklistedUser("23120112", "Nguyễn Thị Mai", null, UserStatus.BAN),
            BlacklistedUser("23120123", "Trần Văn Bình", null, UserStatus.REMIND),
            BlacklistedUser("23120134", "Lê Thị Hương", null, UserStatus.WARNING),
            BlacklistedUser("23120145", "Phạm Văn Cường", null, UserStatus.BAN),
            BlacklistedUser("23120156", "Vũ Thị Lan", null, UserStatus.REMIND),
            BlacklistedUser("23120167", "Đỗ Văn Sơn", null, UserStatus.WARNING),
            BlacklistedUser("23120178", "Bùi Thị Nga", null, UserStatus.BAN),
            BlacklistedUser("23120189", "Ngô Văn Tài", null, UserStatus.REMIND),
            BlacklistedUser("23120190", "Dương Thị Thảo", null, UserStatus.WARNING),
            BlacklistedUser("23120201", "Lý Văn Phúc", null, UserStatus.BAN),
            
            // Page 3
            BlacklistedUser("23120212", "Trịnh Thị Hoa", null, UserStatus.REMIND),
            BlacklistedUser("23120223", "Võ Văn Hải", null, UserStatus.WARNING),
            BlacklistedUser("23120234", "Phan Thị Kim", null, UserStatus.BAN),
            BlacklistedUser("23120245", "Đinh Văn Long", null, UserStatus.REMIND),
            BlacklistedUser("23120256", "Mai Thị Linh", null, UserStatus.WARNING),
            BlacklistedUser("23120267", "Tạ Văn Minh", null, UserStatus.BAN),
            BlacklistedUser("23120278", "Chu Thị Phương", null, UserStatus.REMIND),
            BlacklistedUser("23120289", "Hồ Văn Quân", null, UserStatus.WARNING),
            BlacklistedUser("23120290", "Lương Thị Trang", null, UserStatus.BAN),
            BlacklistedUser("23120301", "Đào Văn Tuấn", null, UserStatus.REMIND),
            
            // Page 4
            BlacklistedUser("23120312", "Trương Thị Uyên", null, UserStatus.WARNING),
            BlacklistedUser("23120323", "Hà Văn Vinh", null, UserStatus.BAN),
            BlacklistedUser("23120334", "Cao Thị Xuân", null, UserStatus.REMIND),
            BlacklistedUser("23120345", "Nguyễn Văn Yên", null, UserStatus.WARNING),
            BlacklistedUser("23120356", "Lê Thị Zara", null, UserStatus.BAN)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
