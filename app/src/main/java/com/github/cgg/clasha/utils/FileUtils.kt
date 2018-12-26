package com.github.cgg.clasha.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.github.cgg.clasha.MainActivity
import org.jetbrains.annotations.NonNls
import java.io.*

/**
 * @Author: CCG
 * @Email:
 * @program: ClashA
 * @create: 2018-12-29
 * @describe
 */
const val DOCUMENTS_DIR = "documents"

fun getPath(context: Context, uri: Uri): String? {

    val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
        // ExternalStorageProvider
        if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]

            if ("primary".equals(type, ignoreCase = true)) {
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }
        } else if (isDownloadsDocument(uri)) {

            val id = DocumentsContract.getDocumentId(uri)

            //HUAWEI 8.0 MAYBE
            var split = id.split(":")
            var type = split[0]
            if ("raw".equals(
                    type,
                    true
                )
            ) { //处理某些机型（比如Goole Pixel ）ID是raw:/storage/emulated/0/Download/c20f8664da05ab6b4644913048ea8c83.mp4
                return split[1]
            }
            //HUAWEI 8.0 MAYBE

            val contentUriPrefixesToTry = arrayOf(
                "content://downloads/public_downloads",
                "content://downloads/my_downloads",
                "content://downloads/all_downloads"
            )

            for (contentUriPrefix in contentUriPrefixesToTry) {
                val contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), java.lang.Long.valueOf(id))
                try {
                    val path = getDataColumn(context, contentUri, null, null)
                    if (path != null) {
                        return path
                    }
                } catch (e: Exception) {
                }
            }
            // path could not be retrieved using ContentResolver, therefore copy file to accessible cache using streams
            var fileName = getFileName(context, uri)
            var cacheDir = getDocumentCacheDir(context)
            var file = generateFileName(fileName, cacheDir)
            var destinationPath: String = ""
            if (file != null) {
                destinationPath = file.absolutePath;
                saveFileFromUri(context, uri, destinationPath)
            }
            return destinationPath

        } else if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]

            var contentUri: Uri? = null
            if ("image" == type) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else if ("video" == type) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else if ("audio" == type) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])

            return getDataColumn(context, contentUri, selection, selectionArgs)
        }// MediaProvider
        // DownloadsProvider
    } else if ("content".equals(uri.scheme, ignoreCase = true)) {
        return getDataColumn(context, uri, null, null)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }// File
    // MediaStore (and general)
    return ""
}

/**
 * Get the value of the data column for this Uri. This is useful for
 * MediaStore Uris, and other file-based ContentProviders.
 *
 * @param context       The context.
 * @param uri           The Uri to query.
 * @param selection     (Optional) Filter used in the query.
 * @param selectionArgs (Optional) Selection arguments used in the query.
 * @return The value of the _data column, which is typically a file path.
 */
private fun getDataColumn(
    context: Context, uri: Uri?, selection: String?,
    selectionArgs: Array<String>?
): String? {

    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(column)

    try {
        cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val column_index = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(column_index)
        }
    } finally {
        if (cursor != null)
            cursor.close()
    }
    return null
}

private fun getFileName(@NonNls context: Context, uri: Uri): String? {
    val mimeType = context.getContentResolver().getType(uri)
    var fileName: String? = null

    if (mimeType == null && context != null) {
        var path = getPath(context, uri)
        if (path == null) {
            fileName = getName(uri.toString())
        } else {
            var file = File(path)
            fileName = file.getName()
        }
    } else {
        var returnCursor = context.getContentResolver().query(uri, null, null, null, null)
        if (returnCursor != null) {
            var nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            fileName = returnCursor.getString(nameIndex)
            returnCursor.close()
        }
    }
    return fileName
}

@Nullable
fun generateFileName(@Nullable name: String?, directory: File): File? {
    if (name.isNullOrEmpty()) {
        return null
    }

    var name = name

    var file = File(directory, name)

    if (file.exists()) {
        var fileName: String = name!!
        var extension = ""
        val dotIndex = name!!.lastIndexOf('.')
        if (dotIndex > 0) {
            fileName = name.substring(0, dotIndex)
            extension = name.substring(dotIndex)
        }

        var index = 0

        while (file.exists()) {
            index++
            name = fileName + '('.toString() + index + ')'.toString() + extension
            file = File(directory, name)
        }
    }

    try {
        if (!file.createNewFile()) {
            return null
        }
    } catch (e: IOException) {
        return null
    }


    return file
}

fun getDocumentCacheDir(@NonNull context: Context): File {
    val dir = File(context.cacheDir, DOCUMENTS_DIR)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}

private fun getName(fileName: String): String? {
    if (fileName == null) return null
    val index = fileName.lastIndexOf("/")
    return fileName.substring(index + 1)
}

private fun saveFileFromUri(context: Context, uri: Uri, destinationPath: String) {
    var `is`: InputStream? = null
    var bos: BufferedOutputStream? = null
    try {
        `is` = context.contentResolver.openInputStream(uri)
        bos = BufferedOutputStream(FileOutputStream(destinationPath, false))
        val buf = ByteArray(1024)
        `is`!!.read(buf)
        do {
            bos.write(buf)
        } while (`is`.read(buf) !== -1)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            `is`?.close()
            bos?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is ExternalStorageProvider.
 */
private fun isExternalStorageDocument(uri: Uri): Boolean {
    return "com.android.externalstorage.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is DownloadsProvider.
 */
private fun isDownloadsDocument(uri: Uri): Boolean {
    return "com.android.providers.downloads.documents" == uri.authority
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is MediaProvider.
 */
private fun isMediaDocument(uri: Uri): Boolean {
    return "com.android.providers.media.documents" == uri.authority
}