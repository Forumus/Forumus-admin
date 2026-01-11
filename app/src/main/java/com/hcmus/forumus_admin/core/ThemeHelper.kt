package com.hcmus.forumus_admin.core

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    
    private const val PREFS_NAME = "settings"
    private const val THEME_KEY = "theme"
    
    enum class Theme {
        LIGHT, DARK, AUTO
    }
    
    /**
     * Apply the theme based on saved preferences
     */
    fun applyTheme(context: Context) {
        val theme = getSavedTheme(context)
        applyTheme(theme)
    }
    
    /**
     * Apply the specified theme
     */
    fun applyTheme(theme: Theme) {
        val mode = when (theme) {
            Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            Theme.AUTO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
    
    /**
     * Get the saved theme from SharedPreferences
     */
    fun getSavedTheme(context: Context): Theme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(THEME_KEY, Theme.AUTO.name) ?: Theme.AUTO.name
        return try {
            Theme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            Theme.AUTO
        }
    }
    
    /**
     * Save the theme preference
     */
    fun saveTheme(context: Context, theme: Theme) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(THEME_KEY, theme.name).apply()
    }
    
    /**
     * Set and apply the theme immediately
     */
    fun setTheme(context: Context, theme: Theme) {
        saveTheme(context, theme)
        applyTheme(theme)
    }
}
