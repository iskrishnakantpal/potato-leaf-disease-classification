package com.example.potaleafscan

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.*

object FileUtils {
    fun getFileFromUri(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File.createTempFile("upload", ".jpg", context.cacheDir)
        copyStreamToFile(inputStream!!, file)
        return file
    }

    private fun copyStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use { output ->
            val buffer = ByteArray(4 * 1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
            }
            output.flush()
        }
    }
}
