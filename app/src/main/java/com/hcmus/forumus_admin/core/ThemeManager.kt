package com.hcmus.forumus_admin.core

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "theme_settings"
    private const val KEY_THEME_MODE = "theme_mode"
    
    enum class ThemeMode {
        LIGHT, DARK, AUTO
    }
    
    /**
     * Apply the theme based on the saved preference
     */
    fun applyTheme(context: Context) {
        val mode = getSavedThemeMode(context)
        applyTheme(mode)
    }
    
    /**
     * Apply a specific theme mode
     */
    fun applyTheme(mode: ThemeMode) {
        when (mode) {
            ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeMode.AUTO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    /**
     * Save the theme mode preference
     */
    fun saveThemeMode(context: Context, mode: ThemeMode) {
        getPreferences(context).edit().apply {
            putString(KEY_THEME_MODE, mode.name)
            apply()
        }
    }
    
    /**
     * Get the saved theme mode, defaults to LIGHT if not set
     */
    fun getSavedThemeMode(context: Context): ThemeMode {
        val prefs = getPreferences(context)
        val modeName = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name)
        return try {
            ThemeMode.valueOf(modeName ?: ThemeMode.LIGHT.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.LIGHT
        }
    }
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
