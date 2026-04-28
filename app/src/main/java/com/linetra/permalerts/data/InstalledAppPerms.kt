package com.linetra.permalerts.data

data class InstalledAppPerms(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val grantedSensitive: Set<String>,
)
