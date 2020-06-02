package chat.rocket;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.os.Environment;
import android.text.TextUtils;
import android.os.ParcelFileDescriptor;
import java.nio.channels.FileChannel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class RealPathUtil {
	public static String getRealPathFromURI(final Context context, final Uri uri) {
		final boolean isKitKatOrNewer = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

		if (isKitKatOrNewer && DocumentsContract.isDocumentUri(context, uri)) {
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/" + split[1];
				}
			} else if (isDownloadsDocument(uri)) {
				final String id = DocumentsContract.getDocumentId(uri);
				if (!TextUtils.isEmpty(id)) {
					if (id.startsWith("raw:")) return id.replaceFirst("raw:", "");
					try {
						return getPathFromSavingTempFile(context, uri);
					} catch (NumberFormatException e) {
						return null;
					}
				}
			} else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				switch (type) {
					case "image":
						contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
						break;
					case "video":
						contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
						break;
					case "audio":
						contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
						break;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] { split[1] };

				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		}

		if ("content".equalsIgnoreCase(uri.getScheme())) {
			if (isGooglePhotosUri(uri)) {
				return uri.getLastPathSegment();
			}
			return getPathFromSavingTempFile(context, uri);
		} else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	public static String getPathFromSavingTempFile(Context context, final Uri uri) {
			File tmpFile;
			String fileName = null;

			try {
				Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
				int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				returnCursor.moveToFirst();
				fileName = returnCursor.getString(nameIndex);
			} catch (Exception e) {
				// Do nothing
			}

			try {
				if (fileName == null) fileName = uri.getLastPathSegment().toString().trim();

				File cacheDir = new File(context.getCacheDir(), ShareModule.CACHE_DIR);
				if (!cacheDir.exists()) cacheDir.mkdirs();

				tmpFile = new File(cacheDir, fileName);
				tmpFile.createNewFile();

				ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");

				FileChannel src = new FileInputStream(pfd.getFileDescriptor()).getChannel();
				FileChannel dst = new FileOutputStream(tmpFile).getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			} catch (IOException ex) {
				return null;
			}

			return tmpFile.getAbsolutePath();
	}

	public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };

		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null) cursor.close();
		}

		return null;
	}

	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	public static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}
}