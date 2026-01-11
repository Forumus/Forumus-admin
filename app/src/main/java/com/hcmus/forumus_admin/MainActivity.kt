package com.hcmus.forumus_admin

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView

import android.content.Context
import android.content.res.Configuration
import com.hcmus.forumus_admin.core.LocaleHelper
import com.hcmus.forumus_admin.core.ThemeManager

class MainActivity : AppCompatActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var navController: NavController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before super.onCreate()
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup status bar based on current theme
        setupStatusBar()
        
        setupNavigation()
        setupBackPressedHandler()
    }
    
    private fun setupStatusBar() {
        val window = window
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        if (isDarkMode) {
            // Dark mode: dark status bar with light icons
            window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.background_gray)
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        } else {
            // Light mode: white status bar with dark icons
            window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, android.R.color.white)
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        }
    }
    
    private fun setupNavigation() {
        drawerLayout = findViewById(R.id.drawerLayout)
        
        // Set drawer status bar background based on theme
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val statusBarColor = if (isDarkMode) {
            androidx.core.content.ContextCompat.getColor(this, R.color.background_gray)
        } else {
            androidx.core.content.ContextCompat.getColor(this, android.R.color.white)
        }
        drawerLayout.setStatusBarBackgroundColor(statusBarColor)
        
        navigationView = findViewById(R.id.navigationView)
        
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup NavigationView with NavController
        NavigationUI.setupWithNavController(navigationView, navController)
        
        // Handle navigation item selection
        navigationView.setNavigationItemSelectedListener { menuItem ->
            val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
            if (handled) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
            handled
        }
    }
    
    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }
    
    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
}