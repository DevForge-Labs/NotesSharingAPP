package com.pravor.notessharing.data.classroom.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.pravor.notessharing.MainActivity
import com.pravor.notessharing.R
import com.pravor.notessharing.data.classroom.ClassroomAuthManager
import com.pravor.notessharing.data.classroom.ClassroomAuthState
import com.pravor.notessharing.data.local.db.AppDatabase
import com.pravor.notessharing.data.local.preferences.NotificationCategory
import com.pravor.notessharing.data.local.preferences.NotificationPreferences
import com.pravor.notessharing.domain.model.classroom.ClassroomDateUtils
import com.pravor.notessharing.domain.model.classroom.SubmissionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClassroomReminderReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "ClassroomReminder"
        const val ACTION_CLASSROOM_REMINDER = "com.pravor.notessharing.ACTION_CLASSROOM_REMINDER"

        const val CHANNEL_ID = "classroom_reminders"
        const val CHANNEL_NAME = "Classroom Reminders"

        const val EXTRA_COURSE_ID = "course_id"
        const val EXTRA_COURSEWORK_ID = "coursework_id"
        const val EXTRA_REMINDER_TYPE = "reminder_type"
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DUE_FORMATTED = "due_formatted"
        const val EXTRA_IS_QUIZ = "is_quiz"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for pending Google Classroom assignments, tests, and quizzes"
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CLASSROOM_REMINDER) return

        val courseId = intent.getStringExtra(EXTRA_COURSE_ID) ?: return
        val courseWorkId = intent.getStringExtra(EXTRA_COURSEWORK_ID) ?: return
        val reminderTypeStr = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: "24H"
        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: "Classroom"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Assignment"
        val dueFormatted = intent.getStringExtra(EXTRA_DUE_FORMATTED)
        val isQuiz = intent.getBooleanExtra(EXTRA_IS_QUIZ, false)

        Log.d(TAG, "Received alarm for courseWork=$courseWorkId, type=$reminderTypeStr, title='$title'")

        // 1. Check user notification preferences
        val prefs = NotificationPreferences(context)
        if (!prefs.shouldShowSystemNotification(NotificationCategory.CLASSROOM)) {
            Log.d(TAG, "Classroom notifications disabled in preferences. Discarding reminder.")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 2. Validate current auth and state in Room
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                val authManager = ClassroomAuthManager.getInstance(context)
                val currentSession = authManager.authState.value
                if (currentSession !is ClassroomAuthState.Connected) {
                    Log.d(TAG, "Classroom is no longer connected. Skipping notification.")
                    return@launch
                }

                val db = AppDatabase.getDatabase(context)
                val dao = db.classroomDao()

                // Check manual completions
                val manualCompletions = dao.getAllManualCompletions(currentUid).toSet()
                if (courseWorkId in manualCompletions) {
                    Log.d(TAG, "Assignment $courseWorkId is marked done locally. Skipping notification.")
                    return@launch
                }

                // Check remote submission state
                val sub = dao.getSubmission(courseId, courseWorkId, currentUid)
                if (sub != null) {
                    val state = try { SubmissionState.valueOf(sub.state) } catch (e: Exception) { SubmissionState.UNKNOWN }
                    if (state == SubmissionState.TURNED_IN || state == SubmissionState.RETURNED) {
                        Log.d(TAG, "Assignment $courseWorkId is already submitted ($state). Skipping notification.")
                        return@launch
                    }
                }

                // Check if hidden course
                val hiddenCourses = dao.getHiddenCourseIds(currentUid, currentSession.account.email)
                if (courseId in hiddenCourses) {
                    Log.d(TAG, "Course $courseId is hidden. Skipping notification.")
                    return@launch
                }

                // 3. Display the notification
                createNotificationChannel(context)

                val cleanDue = ClassroomDateUtils.formatDueDateTime(dueFormatted) ?: dueFormatted ?: "soon"

                val notifTitle: String
                val notifBody: String
                if (reminderTypeStr == ClassroomReminderType.DUE_3_HOURS.typeKey) {
                    if (isQuiz) {
                        notifTitle = "⏰ Quiz Starting in 3 Hours"
                        notifBody = "$title for $courseName is due in 3 hours ($cleanDue)."
                    } else {
                        notifTitle = "⏰ Assignment Due in 3 Hours"
                        notifBody = "$title ($courseName) is due in 3 hours ($cleanDue)."
                    }
                } else {
                    if (isQuiz) {
                        notifTitle = "📝 Upcoming Quiz Tomorrow"
                        notifBody = "$title for $courseName is scheduled for tomorrow ($cleanDue)."
                    } else {
                        notifTitle = "📚 Assignment Due Tomorrow"
                        notifBody = "$title ($courseName) is due tomorrow ($cleanDue)."
                    }
                }

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("course_id", courseId)
                    putExtra("coursework_id", courseWorkId)
                    putExtra("notification_id", "classroom_${courseId}_${courseWorkId}")
                }

                val requestCode = "${courseId}_${courseWorkId}_${reminderTypeStr}".hashCode()
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    requestCode,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(notifTitle)
                    .setContentText(notifBody)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .build()

                val notificationManager = NotificationManagerCompat.from(context)
                if (notificationManager.areNotificationsEnabled()) {
                    notificationManager.notify(requestCode, notification)
                    Log.d(TAG, "Classroom reminder notification successfully displayed (ID=$requestCode).")
                } else {
                    Log.w(TAG, "Notifications are disabled at the Android OS level.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Classroom reminder broadcast", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
