package com.pravor.notessharing.widget

import android.content.Context
import android.content.Intent
import com.pravor.notessharing.MainActivity

object WidgetActions {
    fun createClickIntent(context: Context, destination: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = "com.pravor.notessharing.widget.ACTION_$destination"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(WidgetDestinations.EXTRA_DESTINATION, destination)
        }
    }
}
