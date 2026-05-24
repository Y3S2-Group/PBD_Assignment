package com.example.financeapp.data.local

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object LocalImageStorage {
    suspend fun copyToInternalStorage(context: Context, sourceUri: Uri): String =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(sourceUri)
            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?: "jpg"
            val targetFile = File(
                context.filesDir,
                "profile_${System.currentTimeMillis()}.$extension",
            )

            contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Unable to open input stream for $sourceUri")

            targetFile.absolutePath
        }
}

