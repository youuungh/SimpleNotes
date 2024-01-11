package com.example.simplenotes.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getImageUriWithAuthority(context: Context, uri: Uri, activity: FragmentActivity): String? {
    var inputStream: InputStream? = null
    if (uri.authority != null) {
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            val file = getPhotoFile(activity)
            val out = FileOutputStream(file)
            inputStream.use { input ->
                out.use {
                    input?.copyTo(it)
                }
            }
            return file.absolutePath
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    return null
}

fun getPhotoFile(activity: FragmentActivity): File {
    val privateStorageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val timeStamp: String =
        SimpleDateFormat("yyyy_MM_dd_HH:mm:ss", Locale.getDefault()).format(Date())
    val file = File.createTempFile("IMG_${timeStamp}_", ".jpg", privateStorageDir)

    if (Integer.parseInt(file.length().toString()) == 0) {
        file.delete()
    }
    return file
}