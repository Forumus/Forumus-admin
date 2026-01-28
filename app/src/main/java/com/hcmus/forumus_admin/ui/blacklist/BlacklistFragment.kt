package com.hcmus.forumus_admin.ui.blacklist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hcmus.forumus_admin.R
import com.hcmus.forumus_admin.databinding.FragmentBlacklistBinding
import com.hcmus.forumus_admin.data.repository.UserRepository
import com.hcmus.forumus_admin.data.model.UserStatus
import com.hcmus.forumus_admin.data.service.EmailNotificationService
import com.hcmus.forumus_admin.data.service.PushNotificationService
import com.hcmus.forumus_admin.ui.common.UserAutoCompleteAdapter
import com.hcmus.forumus_admin.ui.common.UserSuggestion
import kotlinx.coroutines.launch

data class BlacklistedUser(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val status: UserStatus,
    val uid: String = ""  // Firebase document ID
)

enum class ActionType {
    REMOVED,
    BANNED,
    WARNED,
    REMINDED
}

class BlacklistFragment : Fragment() {

    private var _binding: FragmentBlacklistBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: BlacklistAdapter
    private lateinit var autoCompleteAdapter: UserAutoCompleteAdapter
    private val userRepository = UserRepository()
    private val emailNotificationService = EmailNotificationService.getInstance()
    private val pushNotificationService = PushNotificationService.getInstance()
    private var allUsers: List<BlacklistedUser> = emptyList()
    private var filteredUsers: List<BlacklistedUser> = emptyList()
    private var currentPage = 0
    private val itemsPerPage = 15
    private var totalPages = 0
    private var isLoading = false
    
    // Filter state
    private val selectedStatuses = mutableSetOf<UserStatus>()

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
        // Set up RecyclerView
        adapter = BlacklistAdapter(
            users = emptyList(),
            onRemoveClick = { user -> showConfirmationDialog(user, ActionType.REMOVED) },
            onStatusClick = { user -> showStatusActionMenu(user) }
        )
        
        _binding?.usersRecyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@BlacklistFragment.adapter
        }
        
        // Load users from Firebase
        loadUsersFromFirebase()
    }
    
    private fun loadUsersFromFirebase() {
        if (isLoading) return
        
        isLoading = true
        showLoading(true)
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = userRepository.getBlacklistedUsers()
                
                // Check if fragment is still added to activity
                if (!isAdded || _binding == null) return@launch
                
                result.onSuccess { firestoreUsers ->
                    if (firestoreUsers.isEmpty()) {
                        context?.let {
                            Toast.makeText(it, getString(R.string.no_blacklisted_users), Toast.LENGTH_SHORT).show()
                        }
                        // Use empty list instead of fallback
                        allUsers = emptyList()
                    } else {
                        // Convert Firestore users to BlacklistedUser with actual status from Firebase
                        allUsers = firestoreUsers.map { firestoreUser ->
                            BlacklistedUser(
                                id = extractIdFromEmail(firestoreUser.email),
                                name = firestoreUser.fullName.ifEmpty { 
                                    extractIdFromEmail(firestoreUser.email) 
                                },
                                avatarUrl = firestoreUser.profilePictureUrl,
                                status = UserRepository.mapStatusToEnum(firestoreUser.status),
                                uid = firestoreUser.uid
                            )
                        }
                        
                        context?.let {
                            Toast.makeText(it, getString(R.string.loading_users_count, allUsers.size), Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    filteredUsers = allUsers
                    if (isAdded && _binding != null) {
                        updateAutocompleteSuggestions()
                        calculateTotalPages()
                        adapter.updateUsers(getCurrentPageUsers())
                        updatePaginationUI()
                    }
                }.onFailure { exception ->
                    context?.let {
                        Toast.makeText(it, getString(R.string.loading_users_error, exception.message), Toast.LENGTH_LONG).show()
                    }
                    
                    // Initialize with empty list on error
                    allUsers = emptyList()
                    filteredUsers = allUsers
                    if (isAdded && _binding != null) {
                        calculateTotalPages()
                        adapter.updateUsers(getCurrentPageUsers())
                        updatePaginationUI()
                    }
                }
            } finally {
                isLoading = false
                showLoading(false)
            }
        }
    }
    
    private fun extractIdFromEmail(email: String): String {
        // Extract the part before @ from email
        return email.substringBefore("@")
    }
    
    private fun showLoading(show: Boolean) {
        _binding?.let { binding ->
            binding.usersRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
            binding.loadingIndicator.visibility = if (show) View.VISIBLE else View.GONE
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
                    0 -> showConfirmationDialog(user, ActionType.BANNED)
                    1 -> showConfirmationDialog(user, ActionType.WARNED)
                    2 -> showConfirmationDialog(user, ActionType.REMINDED)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showConfirmationDialog(user: BlacklistedUser, actionType: ActionType) {
        val (title, message) = when (actionType) {
            ActionType.REMOVED -> Pair(
                getString(R.string.confirm_remove_title),
                getString(R.string.confirm_remove_message, user.name)
            )
            ActionType.BANNED -> Pair(
                getString(R.string.confirm_ban_title),
                getString(R.string.confirm_ban_message, user.name)
            )
            ActionType.WARNED -> Pair(
                getString(R.string.confirm_warning_title),
                getString(R.string.confirm_warning_message, user.name)
            )
            ActionType.REMINDED -> Pair(
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
        viewLifecycleOwner.lifecycleScope.launch {
            when (actionType) {
                ActionType.REMOVED -> {
                    // Remove user from blacklist (set status to NORMAL)
                    // This will also send congratulatory email
                    updateUserStatusInFirebase(user, UserStatus.NORMAL)
                    
                    // Remove from local list after successful update
                    allUsers = allUsers.filter { it.id != user.id }
                    applySearchFilter(binding.searchInput.text.toString())
                    
                    // Adjust current page if needed
                    if (getCurrentPageUsers().isEmpty() && currentPage > 0) {
                        currentPage--
                    }
                    updatePaginationUI()
                }
                ActionType.BANNED -> {
                    // Update user status to BANNED
                    updateUserStatusInFirebase(user, UserStatus.BANNED)
                }
                ActionType.WARNED -> {
                    // Update user status to WARNED
                    updateUserStatusInFirebase(user, UserStatus.WARNED)
                }
                ActionType.REMINDED -> {
                    // Update user status to REMINDED
                    updateUserStatusInFirebase(user, UserStatus.REMINDED)
                }
            }
        }
    }
    
    private suspend fun updateUserStatusInFirebase(user: BlacklistedUser, newStatus: UserStatus) {
        val result = userRepository.updateUserStatus(user.uid, newStatus)
        result.onSuccess {
            val oldStatus = user.status
            
            // Send email notification to user
            try {
                val emailResult = if (isStatusIncreasing(oldStatus, newStatus)) {
                    // Status escalated - send warning/reminder email
                    emailNotificationService.sendEscalationEmail(
                        userEmail = getUserEmail(user),
                        userName = user.name,
                        newStatus = newStatus,
                        reportedPosts = null
                    )
                } else {
                    // Status de-escalated - send congratulatory email
                    emailNotificationService.sendDeEscalationEmail(
                        userEmail = getUserEmail(user),
                        userName = user.name,
                        oldStatus = oldStatus,
                        newStatus = newStatus
                    )
                }
                
                if (emailResult.isSuccess) {
                    android.util.Log.d("BlacklistFragment", "Email notification sent to ${user.name}")
                } else {
                    android.util.Log.w("BlacklistFragment", "Failed to send email: ${emailResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.w("BlacklistFragment", "Error sending email (non-blocking)", e)
            }
            
            // Send push notification about status change
            try {
                pushNotificationService.sendStatusChangedNotification(
                    userId = user.uid,
                    oldStatus = oldStatus.name,
                    newStatus = newStatus.name
                )
            } catch (e: Exception) {
                android.util.Log.w("BlacklistFragment", "Failed to send push notification (non-blocking)", e)
            }
            
            // Update local list
            allUsers = allUsers.map {
                if (it.id == user.id) it.copy(status = newStatus) else it
            }
            applySearchFilter(binding.searchInput.text.toString())
            
            val message = when (newStatus) {
                UserStatus.BANNED -> getString(R.string.user_banned_message, user.name)
                UserStatus.WARNED -> getString(R.string.user_warned_message, user.name)
                UserStatus.REMINDED -> getString(R.string.user_reminded_message, user.name)
                UserStatus.NORMAL -> getString(R.string.user_removed_blacklist_message, user.name)
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }.onFailure { exception ->
            Toast.makeText(requireContext(), getString(R.string.update_status_failed, exception.message), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Check if status is increasing in severity (escalation).
     * Order: NORMAL < REMINDED < WARNED < BANNED
     */
    private fun isStatusIncreasing(oldStatus: UserStatus, newStatus: UserStatus): Boolean {
        val statusOrder = listOf(UserStatus.NORMAL, UserStatus.REMINDED, UserStatus.WARNED, UserStatus.BANNED)
        val oldIndex = statusOrder.indexOf(oldStatus)
        val newIndex = statusOrder.indexOf(newStatus)
        return newIndex > oldIndex
    }
    
    /**
     * Get user email from user object.
     * This is a helper method - adjust based on how email is stored in your data model.
     */
    private suspend fun getUserEmail(user: BlacklistedUser): String {
        // Try to get full user info from repository to get email
        val userResult = userRepository.getUserById(user.uid)
        return userResult.getOrNull()?.email ?: "${user.id}@example.com"
    }

    private fun setupSearchBar() {
        // Initialize autocomplete adapter
        autoCompleteAdapter = UserAutoCompleteAdapter(requireContext())
        binding.searchInput.setAdapter(autoCompleteAdapter)
        
        // Handle text changes for filtering
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                applySearchFilter(query)
                
                // Show/hide clear button
                binding.clearSearchButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Handle item selection from autocomplete dropdown
        binding.searchInput.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val suggestion = autoCompleteAdapter.getItem(position)
            suggestion?.let {
                // Set the selected text and filter
                binding.searchInput.setText(it.name)
                binding.searchInput.setSelection(it.name.length)
                applySearchFilter(it.name)
            }
        }
        
        // Handle keyboard search action
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                applySearchFilter(binding.searchInput.text.toString())
                binding.searchInput.dismissDropDown()
                true
            } else false
        }
        
        // Clear button click
        binding.clearSearchButton.setOnClickListener {
            binding.searchInput.setText("")
            applySearchFilter("")
            binding.clearSearchButton.visibility = View.GONE
        }
        
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }
    }
    
    private fun updateAutocompleteSuggestions() {
        val suggestions = allUsers.map { user ->
            UserSuggestion(
                id = user.id,
                name = user.name,
                displayText = "${user.name} (${user.id})"
            )
        }
        autoCompleteAdapter.updateSuggestions(suggestions)
    }
    
    private fun applySearchFilter(query: String) {
        var users = allUsers
        
        // Apply status filter first
        if (selectedStatuses.isNotEmpty()) {
            users = users.filter { it.status in selectedStatuses }
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
    
    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_status, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        // Make dialog background transparent for rounded corners
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Track current selections (copy of selected statuses)
        val tempSelectedStatuses = selectedStatuses.toMutableSet()
        
        // Get all views
        val closeButton = dialogView.findViewById<View>(R.id.closeButton)
        val clearAllButton = dialogView.findViewById<View>(R.id.clearAllButton)
        val remindButton = dialogView.findViewById<View>(R.id.remindButton)
        val warningButton = dialogView.findViewById<View>(R.id.warningButton)
        val banButton = dialogView.findViewById<View>(R.id.banButton)
        val remindCheckmark = dialogView.findViewById<View>(R.id.remindCheckmark)
        val warningCheckmark = dialogView.findViewById<View>(R.id.warningCheckmark)
        val banCheckmark = dialogView.findViewById<View>(R.id.banCheckmark)
        val applyButton = dialogView.findViewById<View>(R.id.applyButton)
        
        // Initialize UI based on current selections
        updateFilterButtonState(remindButton, remindCheckmark, UserStatus.REMINDED in tempSelectedStatuses)
        updateFilterButtonState(warningButton, warningCheckmark, UserStatus.WARNED in tempSelectedStatuses)
        updateFilterButtonState(banButton, banCheckmark, UserStatus.BANNED in tempSelectedStatuses)
        
        // Close button
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Clear all button
        clearAllButton.setOnClickListener {
            tempSelectedStatuses.clear()
            updateFilterButtonState(remindButton, remindCheckmark, false)
            updateFilterButtonState(warningButton, warningCheckmark, false)
            updateFilterButtonState(banButton, banCheckmark, false)
        }
        
        // Remind button
        remindButton.setOnClickListener {
            val isSelected = UserStatus.REMINDED in tempSelectedStatuses
            if (isSelected) {
                tempSelectedStatuses.remove(UserStatus.REMINDED)
            } else {
                tempSelectedStatuses.add(UserStatus.REMINDED)
            }
            updateFilterButtonState(remindButton, remindCheckmark, !isSelected)
        }
        
        // Warning button
        warningButton.setOnClickListener {
            val isSelected = UserStatus.WARNED in tempSelectedStatuses
            if (isSelected) {
                tempSelectedStatuses.remove(UserStatus.WARNED)
            } else {
                tempSelectedStatuses.add(UserStatus.WARNED)
            }
            updateFilterButtonState(warningButton, warningCheckmark, !isSelected)
        }
        
        // Ban button
        banButton.setOnClickListener {
            val isSelected = UserStatus.BANNED in tempSelectedStatuses
            if (isSelected) {
                tempSelectedStatuses.remove(UserStatus.BANNED)
            } else {
                tempSelectedStatuses.add(UserStatus.BANNED)
            }
            updateFilterButtonState(banButton, banCheckmark, !isSelected)
        }
        
        // Apply button
        applyButton.setOnClickListener {
            selectedStatuses.clear()
            selectedStatuses.addAll(tempSelectedStatuses)
            applySearchFilter(binding.searchInput.text.toString())
            
            val filterMessage = if (selectedStatuses.isEmpty()) {
                getString(R.string.filter_cleared)
            } else {
                getString(R.string.filtered_by, selectedStatuses.joinToString(", "))
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
                R.id.remindButton -> {
                    button.setBackgroundResource(R.drawable.bg_badge_remind)
                    textView?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.badge_remind_text))
                }
                R.id.warningButton -> {
                    button.setBackgroundResource(R.drawable.bg_report_badge)
                    textView?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.badge_warning_text))
                }
                R.id.banButton -> {
                    button.setBackgroundResource(R.drawable.bg_badge_ban)
                    textView?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.badge_ban_text))
                }
            }
            checkmark.visibility = View.VISIBLE
        } else {
            button.setBackgroundResource(R.drawable.bg_filter_option_unselected)
            textView?.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_body))
            checkmark.visibility = View.GONE
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
        _binding?.usersRecyclerView?.scrollToPosition(0)
    }
    
    private fun updatePaginationUI() {
        // Update page indicator text
        binding.pageIndicatorText.text = "${currentPage + 1} / $totalPages"
        
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
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
