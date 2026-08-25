package com.pravor.notessharing.data.classroom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class ClassroomFileRepository(
    private val context: Context
) {
    companion object {
        private const val TAG = "ClassroomFileRepo"

        @Volatile
        private var INSTANCE: ClassroomFileRepository? = null

        fun getInstance(context: Context): ClassroomFileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ClassroomFileRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun openExternalFile(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Cannot launch external URL: $url", e)
            false
        }
    }
}
