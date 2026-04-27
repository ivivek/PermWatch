package com.linetra.permwatch.data

import android.Manifest

enum class PermissionCategory(val displayRes: Int) {
    SMS_CONTACTS(com.linetra.permwatch.R.string.cat_sms_contacts),
    MIC_CAMERA(com.linetra.permwatch.R.string.cat_mic_camera),
    LOCATION(com.linetra.permwatch.R.string.cat_location),
    SPECIAL(com.linetra.permwatch.R.string.cat_special),
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

    private val allManifestNames: Set<String> = all.map { it.manifestName }.toSet()

    fun labelFor(manifestName: String): String =
        byManifestName[manifestName]?.shortLabel ?: manifestName.substringAfterLast('.')

    fun watchedSet(unwatched: Set<String>): Set<String> = allManifestNames - unwatched
}

data class PermissionGroup(
    val id: String,
    val label: String,
    val category: PermissionCategory,
    val members: Set<String>,
)

object PermissionGroups {

    val all: List<PermissionGroup> = listOf(
        PermissionGroup(
            id = "sms",
            label = "SMS",
            category = PermissionCategory.SMS_CONTACTS,
            members = setOf(
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.RECEIVE_MMS,
            ),
        ),
        PermissionGroup(
            id = "contacts",
            label = "Contacts",
            category = PermissionCategory.SMS_CONTACTS,
            members = setOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.GET_ACCOUNTS,
            ),
        ),
        PermissionGroup(
            id = "call_log",
            label = "Call log",
            category = PermissionCategory.SMS_CONTACTS,
            members = setOf(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG,
            ),
        ),
        PermissionGroup(
            id = "phone",
            label = "Phone",
            category = PermissionCategory.SMS_CONTACTS,
            members = setOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_PHONE_NUMBERS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
            ),
        ),
        PermissionGroup(
            id = "microphone",
            label = "Microphone",
            category = PermissionCategory.MIC_CAMERA,
            members = setOf(Manifest.permission.RECORD_AUDIO),
        ),
        PermissionGroup(
            id = "camera",
            label = "Camera",
            category = PermissionCategory.MIC_CAMERA,
            members = setOf(Manifest.permission.CAMERA),
        ),
        PermissionGroup(
            id = "location",
            label = "Location",
            category = PermissionCategory.LOCATION,
            members = setOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ),
        ),
        PermissionGroup(
            id = "draw_over",
            label = "Draw over apps",
            category = PermissionCategory.SPECIAL,
            members = setOf(Manifest.permission.SYSTEM_ALERT_WINDOW),
        ),
        PermissionGroup(
            id = "all_files",
            label = "All files access",
            category = PermissionCategory.SPECIAL,
            members = setOf("android.permission.MANAGE_EXTERNAL_STORAGE"),
        ),
        PermissionGroup(
            id = "accessibility",
            label = "Accessibility",
            category = PermissionCategory.SPECIAL,
            members = setOf("android.permission.BIND_ACCESSIBILITY_SERVICE"),
        ),
    )

    val byId: Map<String, PermissionGroup> = all.associateBy { it.id }
}
