package com.anhkhoa.forumus_admin.ui.blacklist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class BlacklistFragment : Fragment() {

    private var _binding: FragmentBlacklistBinding? = null
    private val binding get() = _binding!!

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
        val sampleUsers = getSampleBlacklistedUsers()
        
        // Set up RecyclerView
        val adapter = BlacklistAdapter(sampleUsers) { user ->
            // Handle remove button click
            handleRemoveUser(user)
        }
        
        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
    }
    
    private fun handleRemoveUser(user: BlacklistedUser) {
        // TODO: Implement remove user from blacklist functionality
        // This should call your backend API or database to remove the user
    }

    private fun setupSearchBar() {
        // TODO: Implement search functionality
        binding.searchInput.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search submission
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle search text changes
                return true
            }
        })
    }

    private fun getSampleBlacklistedUsers(): List<BlacklistedUser> {
        return listOf(
            BlacklistedUser("23120132", "Cao Trọng Khang", null, UserStatus.WARNING),
            BlacklistedUser("23120143", "Tô Thành Long", null, UserStatus.BAN),
            BlacklistedUser("23120096", "Nguyễn Viết Toàn", null, UserStatus.REMIND),
            BlacklistedUser("23120135", "Trần Anh Khoa", null, UserStatus.WARNING),
            BlacklistedUser("23120097", "Võ Tất Toàn", null, UserStatus.BAN),
            BlacklistedUser("23120021", "Nguyễn Văn A", null, UserStatus.REMIND)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
