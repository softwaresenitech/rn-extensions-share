package chat.rocket.rnshareextension

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.core.content.ContentResolverCompat
import com.facebook.react.bridge.*
import java.io.File

class ShareModule(reactContext: ReactApplicationContext?) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "ReactNativeShareExtension"

    @ReactMethod
    fun close() = currentActivity?.finish()

    @ReactMethod
    fun data(promise: Promise) = promise.resolve(processIntent(currentActivity))

    private fun processIntent(activity: Activity?): WritableArray {

        val items = Arguments.createArray()
        val currentActivity = activity ?: return items

        val intent = activity.intent

        Log.e("GRAHAM", intent.type)

        val result = when {
            intent.action == ACTION_SEND && intent.isTypeOf("text/plain") -> actionSendText(intent).also { Log.e("GRAHAM", "plain") }
            intent.action == ACTION_SEND && intent.isTypeOf("image/") -> actionSendImage(intent, currentActivity).also { Log.e("GRAHAM", "single image") }
            intent.action == ACTION_SEND_MULTIPLE && intent.isTypeOf("image/") -> actionSendMultiple(intent, currentActivity).also { Log.e("GRAHAM", "multiple image") }
            intent.action == ACTION_SEND -> actionSendOther(intent).also { Log.e("GRAHAM", "single other") }
            intent.action == ACTION_SEND_MULTIPLE -> actionSendMultipleOther(intent, currentActivity).also { Log.e("GRAHAM", "multiple other") }
            else -> emptyList()
        }

        result.map {
            items.pushMap(it)
        }

        return items
    }

    private fun actionSendMultipleOther(intent: Intent, activity: Activity): List<WritableMap> {

        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) as? List<Uri>
                ?: emptyList()

        return uris.flatMap { uri -> blah(activity, uri, intent).also { Log.e("GRAHAM", "list has ${it.size} items") } }
    }

    private fun blah(activity: Activity, it: Uri, intent: Intent): List<WritableMap> {

        val type = activity.contentResolver.getType(it)

        Log.e("GRAHAM", "The type of file is $type")

        return when {
            type == null -> emptyList()
            type.isTypeOf("image/") -> actionSendImage(intent, activity).also { Log.e("GRAHAM", "The type of file is an image with multiple other files") }
            else -> actionSendOther(intent).also { Log.e("GRAHAM", "The type of file is OTHER with multiple other files") }
        }
    }

    private fun actionSendOther(intent: Intent): List<WritableMap> {

        val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                ?: return emptyList()

        return listOf(uri.toString().createMap("other"))
    }

    private fun actionSendMultiple(intent: Intent, activity: Activity): List<WritableMap> {

        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) as? List<Uri>
                ?: emptyList()

        return uris.mapNotNull {
            createImageFilePathArgumentsMap(it, activity)
        }
    }

    private fun actionSendImage(intent: Intent, activity: Activity): List<WritableMap> {

        val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                ?: return emptyList()

        return createImageFilePathArgumentsMap(uri, activity)?.let { listOf(it) } ?: emptyList()
    }

    private fun actionSendText(intent: Intent): List<WritableMap> {

        return intent.getStringExtra(Intent.EXTRA_TEXT)?.let { listOf(it.createMap("media")) }
                ?: return emptyList()
    }

    private fun createImageFilePathArgumentsMap(uri: Uri, activity: Activity): WritableMap? {

        return runCatching { createPrivateCopy(activity, uri) }
                .getOrDefault(null)
                ?.createMap("media")
    }

    private fun storeImage(cacheDir: File, bitmap: Bitmap): String {

        return File(cacheDir, "share-${System.currentTimeMillis()}.jpg").apply {
            writeBitmap(bitmap, Bitmap.CompressFormat.JPEG, 85)
        }.absolutePath
    }

    private fun createPrivateCopy(context: Context, uri: Uri): String? {

        return context.contentResolver.openInputStream(uri).use {
            val bitmap = BitmapFactory.decodeStream(it)
            storeImage(context.cacheDir, bitmap)
        }
    }
}

private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
    outputStream().use { out ->
        bitmap.compress(format, quality, out)
        out.flush()
    }
}

private fun String.createMap(type: String): WritableMap {

    return Arguments.createMap().apply {
        putString("value", this@createMap)
        putString("type", type)
    }
}

private fun Intent.isTypeOf(typePrefix: String): Boolean {
    return type?.startsWith(typePrefix) == true
}

private fun String.isTypeOf(typePrefix: String): Boolean {
    return this.startsWith(typePrefix)
}