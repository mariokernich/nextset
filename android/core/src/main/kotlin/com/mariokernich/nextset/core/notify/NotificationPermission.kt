package com.mariokernich.nextset.core.notify

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/** Permission to post notifications, which Android 13+ asks for at runtime (phone and watch). */
class NotificationPermission(private val context: Context, private val launch: () -> Unit) {
    /** Also `false` when notifications are turned off for the app in the system settings. */
    val isGranted: Boolean get() = FinishedNotification.canPost(context)

    private val preferences get() = context.getSharedPreferences("nextset", Context.MODE_PRIVATE)

    /** Asks once per installation, like iOS does. */
    fun askOnce(): Boolean {
        if (isGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || preferences.getBoolean(ASKED, false)) return false
        preferences.edit { putBoolean(ASKED, true) }
        launch()
        return true
    }

    /** For a switch the user just turned on: ask, or show the system settings if already declined. */
    fun askOrOpenSettings() {
        if (isGranted || askOnce()) return
        val notificationSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        // Watches don't necessarily have the notification screen of the settings.
        for (intent in listOf(notificationSettings, appDetails)) {
            if (runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }

    private companion object {
        const val ASKED = "permission.notifications.asked"
    }
}

@Composable
fun rememberNotificationPermission(onGranted: () -> Unit): NotificationPermission {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted()
    }
    return remember(context, launcher) {
        NotificationPermission(context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
