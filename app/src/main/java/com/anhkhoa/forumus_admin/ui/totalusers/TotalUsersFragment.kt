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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.anhkhoa.forumus_admin.R
import com.anhkhoa.forumus_admin.data.model.User
import com.anhkhoa.forumus_admin.data.model.UserStatus
import com.anhkhoa.forumus_admin.data.repository.UserRepository
import com.anhkhoa.forumus_admin.databinding.FragmentTotalUsersBinding
import kotlinx.coroutines.launch

class TotalUsersFragment : Fragment() {

    private var _binding: FragmentTotalUsersBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TotalUsersAdapter
    private val userRepository = UserRepository()
    private var allUsers: List<User> = emptyList()
    private var filteredUsers: List<User> = emptyList()
    private var currentPage = 0
    private val itemsPerPage = 10
    private var totalPages = 0
    private var isLoading = false
    
    // Filter state
    private val selectedStatuses = mutableSetOf<UserStatus>()
    private val selectedRoles = mutableSetOf<String>()

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
        loadUsersFromFirebase()
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
            showFilterDialog()
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

    private fun loadUsersFromFirebase() {
        if (isLoading) return
        
        isLoading = true
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val result = userRepository.getAllUsers()
                
                result.onSuccess { firestoreUsers ->
                    if (firestoreUsers.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "No users found in database",
                            Toast.LENGTH_SHORT
                        ).show()
                        allUsers = emptyList()
                    } else {
                        // Convert Firestore users to User model
                        allUsers = firestoreUsers.map { firestoreUser ->
                            User(
                                id = extractIdFromEmail(firestoreUser.email),
                                name = firestoreUser.fullName.ifEmpty { 
                                    extractIdFromEmail(firestoreUser.email) 
                                },
                                avatarUrl = firestoreUser.profilePictureUrl,
                                status = UserRepository.mapStatusToEnum(firestoreUser.status),
                                role = mapRoleToDisplayName(firestoreUser.role)
                            )
                        }
                        
                        Toast.makeText(
                            requireContext(),
                            "Loaded ${allUsers.size} users",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    filteredUsers = allUsers
                    calculateTotalPages()
                    updatePage()
                }.onFailure { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Error loading users: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    allUsers = emptyList()
                    filteredUsers = allUsers
                    calculateTotalPages()
                    updatePage()
                }
            } finally {
                isLoading = false
                showLoading(false)
            }
        }
    }
    
    private fun extractIdFromEmail(email: String): String {
        return email.substringBefore("@")
    }
    
    private fun mapRoleToDisplayName(role: String): String {
        return when (role.uppercase()) {
            "STUDENT" -> "Student"
            "TEACHER" -> "Teacher"
            else -> role.replaceFirstChar { it.uppercase() }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.usersRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun applySearchFilter(query: String) {
        var users = allUsers
        
        // Apply status filter first
        if (selectedStatuses.isNotEmpty()) {
            users = users.filter { it.status in selectedStatuses }
        }
        
        // Apply role filter
        if (selectedRoles.isNotEmpty()) {
            users = users.filter { it.role in selectedRoles }
        }
        
        // Apply search query
        filteredUsers = if (query.isEmpty()) {
            users
        } else {
            users.filter {
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

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_users, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent for rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Track current selections (copy of selected filters)
        val tempSelectedStatuses = selectedStatuses.toMutableSet()
        val tempSelectedRoles = selectedRoles.toMutableSet()
        
        // Get all views
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val clearAllButton = dialogView.findViewById<View>(R.id.clearAllButton)
        val normalButton = dialogView.findViewById<View>(R.id.normalButton)
        val remindButton = dialogView.findViewById<View>(R.id.remindButton)
        val warningButton = dialogView.findViewById<View>(R.id.warningButton)
        val banButton = dialogView.findViewById<View>(R.id.banButton)
        val teacherButton = dialogView.findViewById<View>(R.id.teacherButton)
        val studentButton = dialogView.findViewById<View>(R.id.studentButton)
        val normalCheckmark = dialogView.findViewById<View>(R.id.normalCheckmark)
        val remindCheckmark = dialogView.findViewById<View>(R.id.remindCheckmark)
        val warningCheckmark = dialogView.findViewById<View>(R.id.warningCheckmark)
        val banCheckmark = dialogView.findViewById<View>(R.id.banCheckmark)
        val teacherCheckmark = dialogView.findViewById<View>(R.id.teacherCheckmark)
        val studentCheckmark = dialogView.findViewById<View>(R.id.studentCheckmark)
        val applyButton = dialogView.findViewById<View>(R.id.applyButton)
        
        // Initialize UI based on current selections
        updateFilterButtonState(normalButton, normalCheckmark, UserStatus.NORMAL in tempSelectedStatuses)
        updateFilterButtonState(remindButton, remindCheckmark, UserStatus.REMIND in tempSelectedStatuses)
        updateFilterButtonState(warningButton, warningCheckmark, UserStatus.WARNING in tempSelectedStatuses)
        updateFilterButtonState(banButton, banCheckmark, UserStatus.BAN in tempSelectedStatuses)
        updateRoleButtonState(teacherButton, teacherCheckmark, "Teacher" in tempSelectedRoles)
        updateRoleButtonState(studentButton, studentCheckmark, "Student" in tempSelectedRoles)
        
        // Close button
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Clear all button
        clearAllButton.setOnClickListener {
            tempSelectedStatuses.clear()
            tempSelectedRoles.clear()
            updateFilterButtonState(normalButton, normalCheckmark, false)
            updateFilterButtonState(remindButton, remindCheckmark, false)
            updateFilterButtonState(warningButton, warningCheckmark, false)
            updateFilterButtonState(banButton, banCheckmark, false)
            updateRoleButtonState(teacherButton, teacherCheckmark, false)
            updateRoleButtonState(studentButton, studentCheckmark, false)
        }
        
        // Normal button
        normalButton.setOnClickListener {
            val isSelected = UserStatus.NORMAL in tempSelectedStatuses
            if (isSelected) {
                tempSelectedStatuses.remove(UserStatus.NORMAL)
            } else {
                tempSelectedStatuses.add(UserStatus.NORMAL)
            }
            updateFilterButtonState(normalButton, normalCheckmark, !isSelected)
        }
        
        // Remind button
        remindButton.setOnClickListener {
            val isSelected = UserStatus.REMIND in tempSelectedStatuses
            if (isSelected) {
                tempSelectedStatuses.remove(UserStatus.REMIND)
            } else {
                tempSelectedStatuses.add(UserStatus.REMIND)
            }
            updateFilterButtonState(remindButton, remindCheckmark, !isSelected)
        }
        
        // Warning button
        warningButton.setOnClickListener {
            val isSelected = UserStatus.WARNING in tempSelectedStatuses
            if (isSelected) {
                tempSelectedStatuses.remove(UserStatus.WARNING)
            } else {
                tempSelectedStatuses.add(UserStatus.WARNING)
            }
            updateFilterButtonState(warningButton, warningCheckmark, !isSelected)
        }
        
        // Ban button
        banButton.setOnClickListener {
            val isSelected = UserStatus.BAN in tempSelectedStatuses
            if (isSelected) {
                tempSelectedStatuses.remove(UserStatus.BAN)
            } else {
                tempSelectedStatuses.add(UserStatus.BAN)
            }
            updateFilterButtonState(banButton, banCheckmark, !isSelected)
        }
        
        // Teacher button
        teacherButton.setOnClickListener {
            val isSelected = "Teacher" in tempSelectedRoles
            if (isSelected) {
                tempSelectedRoles.remove("Teacher")
            } else {
                tempSelectedRoles.add("Teacher")
            }
            updateRoleButtonState(teacherButton, teacherCheckmark, !isSelected)
        }
        
        // Student button
        studentButton.setOnClickListener {
            val isSelected = "Student" in tempSelectedRoles
            if (isSelected) {
                tempSelectedRoles.remove("Student")
            } else {
                tempSelectedRoles.add("Student")
            }
            updateRoleButtonState(studentButton, studentCheckmark, !isSelected)
        }
        
        // Apply button
        applyButton.setOnClickListener {
            selectedStatuses.clear()
            selectedStatuses.addAll(tempSelectedStatuses)
            selectedRoles.clear()
            selectedRoles.addAll(tempSelectedRoles)
            applySearchFilter(binding.searchInput.query.toString())
            
            val filterParts = mutableListOf<String>()
            if (selectedStatuses.isNotEmpty()) {
                filterParts.add("Status: ${selectedStatuses.joinToString(", ")}")
            }
            if (selectedRoles.isNotEmpty()) {
                filterParts.add("Role: ${selectedRoles.joinToString(", ")}")
            }
            
            val filterMessage = if (filterParts.isEmpty()) {
                "Filter cleared"
            } else {
                "Filtered by ${filterParts.joinToString(" | ")}"
            }
            Toast.makeText(requireContext(), filterMessage, Toast.LENGTH_SHORT).show()
            
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun updateFilterButtonState(button: View, checkmark: View, isSelected: Boolean) {
        // Get the TextView inside the button (first child TextView)
        val textView = (button as? ViewGroup)?.let { group ->
            (0 until group.childCount).map { group.getChildAt(it) }
                .firstOrNull { it is android.widget.TextView } as? android.widget.TextView
        }
        
        if (isSelected) {
            when (button.id) {
                R.id.normalButton -> {
                    button.setBackgroundResource(R.drawable.bg_filter_option_selected_green)
                    textView?.setTextColor(android.graphics.Color.parseColor("#00A63E"))
                }
                R.id.remindButton -> {
                    button.setBackgroundResource(R.drawable.bg_filter_option_selected_blue)
                    textView?.setTextColor(android.graphics.Color.parseColor("#155DFC"))
                }
                R.id.warningButton -> {
                    button.setBackgroundResource(R.drawable.bg_filter_option_selected_orange)
                    textView?.setTextColor(android.graphics.Color.parseColor("#F54900"))
                }
                R.id.banButton -> {
                    button.setBackgroundResource(R.drawable.bg_filter_option_selected_red)
                    textView?.setTextColor(android.graphics.Color.parseColor("#E7000B"))
                }
            }
            checkmark.visibility = View.VISIBLE
        } else {
            button.setBackgroundResource(R.drawable.bg_filter_option_unselected)
            textView?.setTextColor(android.graphics.Color.parseColor("#364153"))
            checkmark.visibility = View.GONE
        }
    }
    
    private fun updateRoleButtonState(button: View, checkmark: View, isSelected: Boolean) {
        // Get the TextView inside the button (first child TextView)
        val textView = (button as? ViewGroup)?.let { group ->
            (0 until group.childCount).map { group.getChildAt(it) }
                .firstOrNull { it is android.widget.TextView } as? android.widget.TextView
        }
        
        if (isSelected) {
            button.setBackgroundResource(R.drawable.bg_filter_option_selected_purple)
            textView?.setTextColor(android.graphics.Color.parseColor("#9810FA"))
            checkmark.visibility = View.VISIBLE
        } else {
            button.setBackgroundResource(R.drawable.bg_filter_option_unselected)
            textView?.setTextColor(android.graphics.Color.parseColor("#364153"))
            checkmark.visibility = View.GONE
        }
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
