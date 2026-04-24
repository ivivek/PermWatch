package com.trogo.permalert.data

import android.Manifest

enum class PermissionCategory(val displayRes: Int) {
    SMS_CONTACTS(com.trogo.permalert.R.string.cat_sms_contacts),
    MIC_CAMERA(com.trogo.permalert.R.string.cat_mic_camera),
    LOCATION(com.trogo.permalert.R.string.cat_location),
    SPECIAL(com.trogo.permalert.R.string.cat_special),
}

data class SensitivePermission(
    val manifestName: String,
    val category: PermissionCategory,
    val shortLabel: String,
    val isAppOp: Boolean = false,
    val isAccessibility: Boolean = false,
)

object SensitivePermissions {

    val all: List<SensitivePermission> = listOf(
        // SMS / Contacts / Call log / Phone
        SensitivePermission(Manifest.permission.READ_SMS, PermissionCategory.SMS_CONTACTS, "Read SMS"),
        SensitivePermission(Manifest.permission.SEND_SMS, PermissionCategory.SMS_CONTACTS, "Send SMS"),
        SensitivePermission(Manifest.permission.RECEIVE_SMS, PermissionCategory.SMS_CONTACTS, "Receive SMS"),
        SensitivePermission(Manifest.permission.RECEIVE_MMS, PermissionCategory.SMS_CONTACTS, "Receive MMS"),
        SensitivePermission(Manifest.permission.READ_CONTACTS, PermissionCategory.SMS_CONTACTS, "Read contacts"),
        SensitivePermission(Manifest.permission.WRITE_CONTACTS, PermissionCategory.SMS_CONTACTS, "Write contacts"),
        SensitivePermission(Manifest.permission.GET_ACCOUNTS, PermissionCategory.SMS_CONTACTS, "Accounts"),
        SensitivePermission(Manifest.permission.READ_CALL_LOG, PermissionCategory.SMS_CONTACTS, "Read call log"),
        SensitivePermission(Manifest.permission.WRITE_CALL_LOG, PermissionCategory.SMS_CONTACTS, "Write call log"),
        SensitivePermission(Manifest.permission.READ_PHONE_STATE, PermissionCategory.SMS_CONTACTS, "Phone state"),
        SensitivePermission(Manifest.permission.READ_PHONE_NUMBERS, PermissionCategory.SMS_CONTACTS, "Phone numbers"),
        SensitivePermission(Manifest.permission.CALL_PHONE, PermissionCategory.SMS_CONTACTS, "Place calls"),
        SensitivePermission(Manifest.permission.ANSWER_PHONE_CALLS, PermissionCategory.SMS_CONTACTS, "Answer calls"),
        SensitivePermission(Manifest.permission.PROCESS_OUTGOING_CALLS, PermissionCategory.SMS_CONTACTS, "Outgoing calls"),

        // Mic / Camera
        SensitivePermission(Manifest.permission.RECORD_AUDIO, PermissionCategory.MIC_CAMERA, "Microphone"),
        SensitivePermission(Manifest.permission.CAMERA, PermissionCategory.MIC_CAMERA, "Camera"),

        // Location
        SensitivePermission(Manifest.permission.ACCESS_FINE_LOCATION, PermissionCategory.LOCATION, "Fine location"),
        SensitivePermission(Manifest.permission.ACCESS_COARSE_LOCATION, PermissionCategory.LOCATION, "Coarse location"),
        SensitivePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, PermissionCategory.LOCATION, "Background location"),

        // Special (app-op / settings backed)
        SensitivePermission(Manifest.permission.SYSTEM_ALERT_WINDOW, PermissionCategory.SPECIAL, "Draw over apps", isAppOp = true),
        SensitivePermission("android.permission.MANAGE_EXTERNAL_STORAGE", PermissionCategory.SPECIAL, "All files access", isAppOp = true),
        SensitivePermission("android.permission.BIND_ACCESSIBILITY_SERVICE", PermissionCategory.SPECIAL, "Accessibility", isAccessibility = true),
    )

    val byManifestName: Map<String, SensitivePermission> = all.associateBy { it.manifestName }

    fun labelFor(manifestName: String): String =
        byManifestName[manifestName]?.shortLabel ?: manifestName.substringAfterLast('.')
}
