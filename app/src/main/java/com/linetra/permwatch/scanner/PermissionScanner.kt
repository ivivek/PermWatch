package com.linetra.permwatch.scanner

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.linetra.permwatch.data.InstalledAppPerms
import com.linetra.permwatch.data.SensitivePermissions

class PermissionScanner(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager
    private val appOps: AppOpsManager get() = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    fun scanAll(): List<InstalledAppPerms> {
        val ownPkg = context.packageName
        val enabledAccessibility = enabledAccessibilityPackages()

        @Suppress("DEPRECATION")
        val packages: List<PackageInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

        return packages.asSequence()
            .filter { it.packageName != ownPkg }
            .map { pkg -> toInstalledAppPerms(pkg, enabledAccessibility) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun toInstalledAppPerms(pkg: PackageInfo, accessibilityPkgs: Set<String>): InstalledAppPerms {
        val requested = pkg.requestedPermissions ?: emptyArray()
        val flags = pkg.requestedPermissionsFlags ?: IntArray(0)
        val uid = pkg.applicationInfo?.uid ?: -1

        val granted = mutableSetOf<String>()
        for (i in requested.indices) {
            val perm = requested[i] ?: continue
            val sp = SensitivePermissions.byManifestName[perm] ?: continue
            when {
                sp.isAccessibility -> {
                    if (pkg.packageName in accessibilityPkgs) granted += perm
                }
                sp.isAppOp -> {
                    if (isAppOpAllowed(perm, uid, pkg.packageName)) granted += perm
                }
                else -> {
                    val f = flags.getOrNull(i) ?: 0
                    if (f and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) granted += perm
                }
            }
        }

        return InstalledAppPerms(
            packageName = pkg.packageName,
            label = (pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName),
            isSystem = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0,
            grantedSensitive = granted,
        )
    }

    private fun isAppOpAllowed(permission: String, uid: Int, packageName: String): Boolean {
        if (uid < 0) return false
        val op = AppOpsManager.permissionToOp(permission) ?: return false
        val mode = try {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(op, uid, packageName)
            } else {
                appOps.checkOpNoThrow(op, uid, packageName)
            }
        } catch (_: SecurityException) {
            return false
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun enabledAccessibilityPackages(): Set<String> {
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return emptySet()
        return raw.split(':')
            .mapNotNull { it.substringBefore('/', missingDelimiterValue = "").takeIf(String::isNotEmpty) }
            .toSet()
    }
}
