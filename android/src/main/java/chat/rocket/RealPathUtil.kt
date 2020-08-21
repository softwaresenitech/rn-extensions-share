// https://github.com/ivpusic/react-native-image-crop-picker/blob/master/android/src/main/java/com/reactnative/ivpusic/imagepicker/RealPathUtil.java
package chat.rocket

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal object RealPathUtil {

    @Throws(IOException::class)
    fun getRealPathFromURI(context: Context, uri: Uri): String? {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return context.getExternalFilesDir(null).toString() + "/" + split[1]
                } else {
                    val splitIndex = docId.indexOf(':', 1)
                    val tag = docId.substring(0, splitIndex)
                    val path = docId.substring(splitIndex + 1)
                    val nonPrimaryVolume = getPathToNonPrimaryVolume(context, tag)
                    if (nonPrimaryVolume != null) {
                        val result = "$nonPrimaryVolume/$path"
                        val file = File(result)
                        return if (file.exists() && file.canRead()) {
                            result
                        } else null
                    }
                }
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
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
                val selectionArgs = arrayOf(
                        split[1]
                )
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * If an image/video has been selected from a cloud storage, this method
     * should be call to download the file in the cache folder.
     *
     * @param context The context
     * @param fileName donwloaded file's name
     * @param uri file's URI
     * @return file that has been written
     */
    private fun writeToFile(context: Context, fileName: String, uri: Uri?): File {
        var fileName = fileName
        val tmpDir = context.cacheDir.toString() + "/rocket-chat"
        val created = File(tmpDir).mkdir()
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1)
        val path = File(tmpDir)
        val file = File(path, fileName)
        try {
            val oos = FileOutputStream(file)
            val buf = ByteArray(8192)
            val `is` = context.contentResolver.openInputStream(uri!!)
            var c = 0
            while (`is`!!.read(buf, 0, buf.size).also { c = it } > 0) {
                oos.write(buf, 0, c)
                oos.flush()
            }
            oos.close()
            `is`.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                              selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val projection = arrayOf(
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME)
        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs,
                    null)
            if (cursor != null && cursor.moveToFirst()) {
                // Fall back to writing to file if _data column does not exist
                val index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val path = if (index > -1) cursor.getString(index) else null
                return if (path != null) {
                    cursor.getString(index)
                } else {
                    val indexDisplayName = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val fileName = cursor.getString(indexDisplayName)
                    val fileWritten = writeToFile(context, fileName, uri)
                    fileWritten.absolutePath
                }
            }
        } finally {
            cursor?.close()
        }
        return null
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

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    private fun getPathToNonPrimaryVolume(context: Context, tag: String): String? {

        context.externalCacheDirs?.forEach {

                val path = it?.absolutePath.orEmpty()
                val index = path.indexOf(tag)
                if (index != -1) {
                    return path.substring(0, index) + tag
                }
        }

        return null
    }
}