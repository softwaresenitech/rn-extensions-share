package chat.rocket

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.net.Uri
import android.os.Parcelable
import com.facebook.react.bridge.*

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

        val result = when {
            intent.action == ACTION_SEND && intent.type == "text/plain" -> actionSendText(intent)
            intent.action == ACTION_SEND -> actionSendImage(intent, currentActivity)
            intent.action == ACTION_SEND_MULTIPLE -> actionSendMultiple(intent, currentActivity)
            else -> emptyList()
        }

        result.map {
            items.pushMap(it)
        }

        return items
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

        return intent.getStringExtra(Intent.EXTRA_TEXT)?.let { listOf(it.createMap("text")) }
                ?: return emptyList()
    }

    private fun createImageFilePathArgumentsMap(uri: Uri, activity: Activity): WritableMap? {

        return runCatching { "file://${RealPathUtil.getRealPathFromURI(activity, uri)}" }
                .getOrDefault(null)
                ?.createMap("media")
    }
}

fun String.createMap(type: String): WritableMap {

    return Arguments.createMap().apply {
        putString("value", this@createMap)
        putString("type", type)
    }
}