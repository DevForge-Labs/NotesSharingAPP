package com.pravor.notessharing.data.classroom.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pravor.notessharing.data.local.preferences.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClassroomBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ClassroomBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val prefs = NotificationPreferences(context)
            if (!prefs.isMasterEnabled() || !prefs.isClassroomEnabled()) {
                Log.d(TAG, "Classroom notifications disabled. Skipping reboot alarm restoration.")
                return
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Restoring Classroom reminder alarms after $action...")
                    ClassroomReminderScheduler.scheduleEligibleReminders(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore Classroom alarms after reboot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
