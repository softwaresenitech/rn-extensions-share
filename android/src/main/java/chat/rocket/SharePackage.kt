package chat.rocket

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.JavaScriptModule
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import java.util.*

class SharePackage : ReactPackage {

    override fun createNativeModules(reactContext: ReactApplicationContext) = listOf<NativeModule>(ShareModule(reactContext))

    override fun createViewManagers(reactContext: ReactApplicationContext) = emptyList<ViewManager<*, *>>()
}