package com.pravor.notessharing.data.classroom.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.data.classroom.ClassroomAuthManager
import com.pravor.notessharing.data.classroom.ClassroomAuthState
import com.pravor.notessharing.data.local.db.AppDatabase
import com.pravor.notessharing.data.local.preferences.NotificationPreferences
import com.pravor.notessharing.domain.model.classroom.ClassroomDateUtils
import com.pravor.notessharing.domain.model.classroom.SubmissionState
import com.pravor.notessharing.domain.model.classroom.isGoogleFormUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ClassroomReminderScheduler {

    private const val TAG = "ClassroomScheduler"
    private const val PREFS_NAME = "classroom_reminders_registry"
    private const val KEY_SCHEDULED_REQUEST_CODES = "scheduled_request_codes"

    /**
     * Scans Room database for current pending coursework and schedules
     * exact/inexact reminders for 24 hours and 3 hours prior to deadlines.
     */
    suspend fun scheduleEligibleReminders(context: Context) = withContext(Dispatchers.IO) {
        val prefs = NotificationPreferences(context)
        if (!prefs.isMasterEnabled() || !prefs.isClassroomEnabled()) {
            Log.d(TAG, "Classroom notifications disabled in preferences. Skipping scheduling.")
            return@withContext
        }

        val authManager = ClassroomAuthManager.getInstance(context)
        val authState = authManager.authState.value
        if (authState !is ClassroomAuthState.Connected) {
            Log.d(TAG, "No active connected Google Classroom session. Skipping scheduling.")
            return@withContext
        }

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
        val account = authState.account.email
        val db = AppDatabase.getDatabase(context)
        val dao = db.classroomDao()

        try {
            val courses = dao.getCoursesForAccount(currentUid, account)
            val hiddenSet = dao.getHiddenCourseIds(currentUid, account).toSet()
            val visibleCoursesMap = courses.filter { it.courseId !in hiddenSet }.associate { it.courseId to it.name }
            if (visibleCoursesMap.isEmpty()) {
                Log.d(TAG, "No visible courses found for account $account.")
                return@withContext
            }

            val allCw = dao.getAllCourseWork(currentUid)
            if (allCw.isEmpty()) {
                Log.d(TAG, "No coursework found in local database.")
                return@withContext
            }

            val allAttachments = dao.getAllAttachments(currentUid).groupBy { it.parentId }
            val allSubmissions = dao.getAllSubmissions(currentUid).associateBy { it.courseWorkId }
            val manualCompletions = dao.getAllManualCompletions(currentUid).toSet()

            val now = System.currentTimeMillis()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@withContext

            var scheduledCount = 0

            for (cw in allCw) {
                val courseName = visibleCoursesMap[cw.courseId] ?: continue
                val dueMillis = ClassroomDateUtils.parseDueDateTimeToEpochMillis(cw.dueFormatted) ?: continue

                // 1. Must be in the future
                if (dueMillis <= now) continue

                // 2. Must not be locally marked done
                if (cw.courseWorkId in manualCompletions) continue

                // 3. Must not be turned in or returned
                val subEntity = allSubmissions[cw.courseWorkId]
                val subState = subEntity?.let {
                    try { SubmissionState.valueOf(it.state) } catch (e: Exception) { SubmissionState.UNKNOWN }
                }
                if (subState == SubmissionState.TURNED_IN || subState == SubmissionState.RETURNED) continue

                val attachments = allAttachments[cw.courseWorkId].orEmpty()
                val isQuiz = isAssessmentOrQuiz(cw.title, attachments.map { it.linkUrl to it.type })

                // Schedule for each supported reminder type
                for (reminderType in ClassroomReminderType.entries) {
                    val triggerTime = dueMillis - reminderType.offsetMillis
                    if (triggerTime > now) {
                        scheduleAlarm(
                            context = context,
                            alarmManager = alarmManager,
                            courseId = cw.courseId,
                            courseWorkId = cw.courseWorkId,
                            reminderType = reminderType,
                            courseName = courseName,
                            title = cw.title,
                            dueFormatted = cw.dueFormatted,
                            isQuiz = isQuiz,
                            triggerTimeMillis = triggerTime
                        )
                        scheduledCount++
                    }
                }
            }

            Log.d(TAG, "Finished scheduling Classroom reminders. Total active alarms queued: $scheduledCount")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule eligible Classroom reminders", e)
        }
    }

    /**
     * Cancels pending alarms for a specific coursework item (e.g. when submitted or marked done).
     */
    fun cancelRemindersForCourseWork(context: Context, courseId: String, courseWorkId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (reminderType in ClassroomReminderType.entries) {
            val requestCode = getRequestCode(courseId, courseWorkId, reminderType)
            cancelAlarm(context, alarmManager, requestCode)
        }
        Log.d(TAG, "Cancelled reminders for coursework: $courseWorkId in course: $courseId")
    }

    /**
     * Cancels all scheduled reminder alarms (e.g. when user toggles preference OFF or disconnects).
     */
    fun cancelAllReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val codes = sp.getStringSet(KEY_SCHEDULED_REQUEST_CODES, emptySet()).orEmpty()
        for (codeStr in codes) {
            val code = codeStr.toIntOrNull() ?: continue
            val intent = Intent(context, ClassroomReminderReceiver::class.java).apply {
                action = ClassroomReminderReceiver.ACTION_CLASSROOM_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        sp.edit().clear().apply()
        Log.d(TAG, "Cancelled all ${codes.size} registered Classroom reminder alarms.")
    }

    /**
     * Reconciles scheduled reminders against current local Room data.
     */
    fun reconcileReminders(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            scheduleEligibleReminders(context)
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        courseId: String,
        courseWorkId: String,
        reminderType: ClassroomReminderType,
        courseName: String,
        title: String,
        dueFormatted: String?,
        isQuiz: Boolean,
        triggerTimeMillis: Long
    ) {
        val requestCode = getRequestCode(courseId, courseWorkId, reminderType)
        val intent = Intent(context, ClassroomReminderReceiver::class.java).apply {
            action = ClassroomReminderReceiver.ACTION_CLASSROOM_REMINDER
            putExtra(ClassroomReminderReceiver.EXTRA_COURSE_ID, courseId)
            putExtra(ClassroomReminderReceiver.EXTRA_COURSEWORK_ID, courseWorkId)
            putExtra(ClassroomReminderReceiver.EXTRA_REMINDER_TYPE, reminderType.typeKey)
            putExtra(ClassroomReminderReceiver.EXTRA_COURSE_NAME, courseName)
            putExtra(ClassroomReminderReceiver.EXTRA_TITLE, title)
            putExtra(ClassroomReminderReceiver.EXTRA_DUE_FORMATTED, dueFormatted)
            putExtra(ClassroomReminderReceiver.EXTRA_IS_QUIZ, isQuiz)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }

            // Record request code in registry
            recordRequestCode(context, requestCode)
            Log.d(TAG, "Scheduled alarm [$requestCode] for '$title' ($reminderType) at $triggerTimeMillis")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for $courseWorkId", e)
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, ClassroomReminderReceiver::class.java).apply {
            action = ClassroomReminderReceiver.ACTION_CLASSROOM_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        removeRequestCode(context, requestCode)
    }

    fun getRequestCode(courseId: String, courseWorkId: String, reminderType: ClassroomReminderType): Int {
        return "${courseId}_${courseWorkId}_${reminderType.typeKey}".hashCode()
    }

    private fun isAssessmentOrQuiz(title: String, attachments: List<Pair<String, String>>): Boolean {
        val titleLower = title.lowercase()
        if (titleLower.contains("quiz") ||
            titleLower.contains("test") ||
            titleLower.contains("exam") ||
            titleLower.contains("mid-sem") ||
            titleLower.contains("end-sem") ||
            titleLower.contains("viva") ||
            titleLower.contains("assessment")
        ) {
            return true
        }
        return attachments.any { (linkUrl, type) ->
            type.equals("FORM", ignoreCase = true) || isGoogleFormUrl(linkUrl)
        }
    }

    private fun recordRequestCode(context: Context, requestCode: Int) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY_SCHEDULED_REQUEST_CODES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.add(requestCode.toString())
        sp.edit().putStringSet(KEY_SCHEDULED_REQUEST_CODES, set).apply()
    }

    private fun removeRequestCode(context: Context, requestCode: Int) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = sp.getStringSet(KEY_SCHEDULED_REQUEST_CODES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.remove(requestCode.toString())
        sp.edit().putStringSet(KEY_SCHEDULED_REQUEST_CODES, set).apply()
    }
}
