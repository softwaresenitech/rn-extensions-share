package chat.rocket;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Arguments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;

public class ShareModule extends ReactContextBaseJavaModule {
  private File tempFolder;
	public static final String CACHE_DIR = "rcShare";

  public ShareModule(ReactApplicationContext reactContext) {
		super(reactContext);
  }

  @Override
  public String getName() {
		return "ReactNativeShareExtension";
  }

  @ReactMethod
  public void close() {
    getCurrentActivity().finish();
  }

  @ReactMethod
  public void data(Promise promise) {
		promise.resolve(processIntent());
  }

  @ReactMethod
  public void dataPreferContent(Promise promise) {
		promise.resolve(processIntentPreferContent());
  }

  public WritableArray processIntentPreferContent() {
	WritableMap map = Arguments.createMap();
	WritableArray items = Arguments.createArray();

	String text = "";
	String textContent = "";
	String type = "";
	String action = "";


	Activity currentActivity = getCurrentActivity();

	if (currentActivity != null) {
			tempFolder = new File(currentActivity.getCacheDir(), CACHE_DIR);

			Intent intent = currentActivity.getIntent();
			action = intent.getAction();
			type = intent.getType();

			// Received some text
			if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
				text = intent.getStringExtra(Intent.EXTRA_TEXT);

				map.putString("value", text);
				map.putString("type", "text");

				items.pushMap(map);
			
			// Received a single file
			} else if (Intent.ACTION_SEND.equals(action)) {
				Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

				if (uri != null) {
					try {
						text = "file://" + RealPathUtil.getRealPathFromURI(currentActivity, uri, false);
						textContent = RealPathUtil.getRealPathFromURI(currentActivity, uri, true);
					} catch (Exception e) {
						e.printStackTrace();
					}

					map.putString("value", text);
					map.putString("type", "media");
					map.putString("valueContent", textContent);

					items.pushMap(map);
				}

			// Received multiple files
			} else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
				ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

				for (Uri uri : uris) {
					String filePath = "";
					String filePathContent = "";
					try {
						filePath = RealPathUtil.getRealPathFromURI(currentActivity, uri, false);
						filePathContent = RealPathUtil.getRealPathFromURI(currentActivity, uri, true);
					} catch (Exception e) {
						e.printStackTrace();
					}

					map = Arguments.createMap();
					text = "file://" + filePath;
					textContent = filePathContent;

					map.putString("valueContent", textContent);
					map.putString("value", text);
					map.putString("type", "media");

					items.pushMap(map);
				}
			}
	}

	return items;
	}
}

  public WritableArray processIntent() {
		WritableMap map = Arguments.createMap();
		WritableArray items = Arguments.createArray();

		String text = "";
		String type = "";
		String action = "";

		Activity currentActivity = getCurrentActivity();

		if (currentActivity != null) {
				tempFolder = new File(currentActivity.getCacheDir(), CACHE_DIR);

				Intent intent = currentActivity.getIntent();
				action = intent.getAction();
				type = intent.getType();

				// Received some text
				if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
					text = intent.getStringExtra(Intent.EXTRA_TEXT);

					map.putString("value", text);
					map.putString("type", "text");

					items.pushMap(map);
				
				// Received a single file
				} else if (Intent.ACTION_SEND.equals(action)) {
					Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

					if (uri != null) {
						try {
							text = "file://" + RealPathUtil.getRealPathFromURI(currentActivity, uri, false);
						} catch (Exception e) {
							e.printStackTrace();
						}

						map.putString("value", text);
						map.putString("type", "media");

						items.pushMap(map);
					}

				// Received multiple files
				} else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
					ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

					for (Uri uri : uris) {
						String filePath = "";
						try {
							filePath = RealPathUtil.getRealPathFromURI(currentActivity, uri, false);
						} catch (Exception e) {
							e.printStackTrace();
						}

						map = Arguments.createMap();
						text = "file://" + filePath;

						map.putString("value", text);
						map.putString("type", "media");

						items.pushMap(map);
					}
				}
		}

		return items;
	}
}
