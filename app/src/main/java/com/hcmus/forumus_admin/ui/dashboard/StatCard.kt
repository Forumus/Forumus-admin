package com.hcmus.forumus_admin.ui.dashboard

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class StatCard(
    @DrawableRes val iconRes: Int,
    @ColorRes val colorRes: Int,
    val label: String,
    val value: String
)
