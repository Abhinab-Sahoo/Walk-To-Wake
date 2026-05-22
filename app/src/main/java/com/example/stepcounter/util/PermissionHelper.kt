package com.example.stepcounter.util

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionHelper(
    private val fragment: Fragment,
    private val onAllPermissionReady: () -> Unit,
    private val onStepPermissionNeeded: () -> Unit,
    private val onNotificationPermissionNeeded: () -> Unit,
    private val onExactAlarmPermissionNeeded: () -> Unit
) {

    fun startPermissionChain(requiredSteps: Boolean) {
        if (requiredSteps) {
            ensureStepCounterPermission()
        } else {
            ensureNotificationPermission()
        }
    }

    /**
     * Checks for and requests the Step Counter (Activity Recognition) permission if needed.
     */
    fun ensureStepCounterPermission() {
        /*
        Before Android 10, any app could read the step sensor freely — no permission needed.
        so below Android 10, permission already exist, we ask for the next permission.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ensureNotificationPermission()
            return // if version is below 10, exit the function here.
        }

        val perm = Manifest.permission.ACTIVITY_RECOGNITION
        when {
            ContextCompat.checkSelfPermission(
                fragment.requireContext(), perm
            ) == PackageManager.PERMISSION_GRANTED -> {
                ensureNotificationPermission()
            }

            fragment.shouldShowRequestPermissionRationale(perm) -> {
                onStepPermissionNeeded()
            }

            else -> {
                onStepPermissionNeeded()
            }
        }
    }

    /**
     * Checks for and requests the Notification permission if needed.
     */
    fun ensureNotificationPermission() {
        /*
        same as ensureStepCounterPermission, if Android version is below 13
        we don't have to ask user for permission. Already permission is there.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ensureExactAlarmPermission()
            return
        }

        val perm = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(
                fragment.requireContext(), perm
        ) == PackageManager.PERMISSION_GRANTED) {
            ensureExactAlarmPermission()
        } else {
            onNotificationPermissionNeeded()
        }
    }

    /**
     * Checks if the app can schedule exact alarms and requests permission if needed.
     */
    fun ensureExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onAllPermissionReady()
            return
        }

        val alarmManager = fragment.requireContext()
            .getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (alarmManager.canScheduleExactAlarms()) {
            onAllPermissionReady()
        } else {
            onExactAlarmPermissionNeeded()
        }
    }

    /**
     * If user denies permission, show them why we need this permission
     */
    fun showNotificationPermissionRequiredDialog() {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Notification Permission Required")
            .setMessage(
                "Walk to Wake needs notification permission to show " +
                        "the alarm screen. Without it, you won't be able to " +
                        "walk to dismiss the alarm. Please enable it in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", fragment.requireContext().packageName, null)
                }
                fragment.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * If user denies permission, show them why we need this permission
     */
    fun showStepCounterPermissionRequiredDialog() {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Step Counter Permission Required")
            .setMessage("Walk to Wake needs step counter permission to able " +
                    "to count steps. Without it, you won't be able to " +
                    "dismiss the alarm. Please enable it in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", fragment.requireContext().packageName, null)
                }
                fragment.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}