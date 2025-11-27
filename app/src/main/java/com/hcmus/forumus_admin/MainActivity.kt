package com.hcmus.forumus_admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var navController: NavController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupNavigation()
    }
    
    private fun setupNavigation() {
        drawerLayout = findViewById(R.id.drawerLayout)
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
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}