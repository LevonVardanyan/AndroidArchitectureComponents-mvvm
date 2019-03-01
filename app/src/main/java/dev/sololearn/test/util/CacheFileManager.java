package dev.sololearn.test.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * manages the bitmap saving, here can be added all cache related utils methods
 */
public class CacheFileManager {

    public static void saveBitmapToInternalFile(Context context, Bitmap bitmap, String thumbnailFileName) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = context.openFileOutput(thumbnailFileName, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void downloadAndSaveImageInternal(Context context, String stringUrl, String fileName, Runnable callback) {
        MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.NETWORK, () -> {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(stringUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                input = connection.getInputStream();
                output = context.openFileOutput(fileName, Context.MODE_PRIVATE);

                byte data[] = new byte[4096];
                int count;
                float progressChange = 0;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                Log.e("Error", "Can't download image");
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();

                MyExecutor.getInstance().lunchOn(MyExecutor.LunchOn.UI, callback);
            }
        });

    }

    public static boolean deleteThumbnailCache(Context context, String name) {
        return new File(context.getFilesDir(), name).delete();
    }

    public static boolean isThumbnailExistInCache(Context context, String name) {
        return new File(context.getFilesDir(), name).exists();
    }

}
