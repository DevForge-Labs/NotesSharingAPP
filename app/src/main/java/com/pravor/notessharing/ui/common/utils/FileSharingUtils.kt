package com.pravor.notessharing.ui.common.utils

import com.pravor.notessharing.ui.common.navigation.*

import com.pravor.notessharing.ui.common.loading.*

import com.pravor.notessharing.ui.common.*

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

    fun shareFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return

        try {
            val authority = "${context.packageName}.fileprovider"

            if (files.size == 1) {
                val file = files[0]
                val uri = FileProvider.getUriForFile(context, authority, file)
                val mimeType = getMimeType(file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share File")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } else {
                val uris = ArrayList(files.map { FileProvider.getUriForFile(context, authority, it) })

                // Resolve MIME types dynamically. If all have the same MIME type, use it. Otherwise, use */*
                val firstMimeType = getMimeType(files[0])
                val allSameMime = files.all { getMimeType(it) == firstMimeType }
                val mimeType = if (allSameMime) firstMimeType else "*/*"

                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = mimeType
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share Files")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        if (mimeType != null) return mimeType
        return when (extension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "txt" -> "text/plain"
            else -> "*/*"
        }
    }
}
