package com.pravor.notessharing.ui.components.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object FileSharingUtils {
    fun openFile(context: Context, file: File, mimeType: String) {
        try {
            if (!file.exists() || file.length() <= 0) {
                Toast.makeText(context, "File is not ready or does not exist", Toast.LENGTH_SHORT).show()
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(context, "No compatible application found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to open: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openFileWith(context: Context, file: File, mimeType: String) {
        try {
            if (!file.exists() || file.length() <= 0) {
                Toast.makeText(context, "File is not ready or does not exist", Toast.LENGTH_SHORT).show()
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Open With")
            // Ensure flag is added if starting from a non-Activity context
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                context.startActivity(chooser)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(context, "No compatible application found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to open: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
