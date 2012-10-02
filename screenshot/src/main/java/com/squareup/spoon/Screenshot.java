package com.squareup.spoon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import static android.content.Context.MODE_WORLD_READABLE;
import static android.graphics.Bitmap.Config.ARGB_8888;

/** Utility class for capturing screenshots for Spoon. */
public final class Screenshot {
  static final String SPOON_SCREENSHOTS = "spoon-screenshots";
  private static final String TAG = "SpoonScreenshot";
  private static final String TAGLESS_PREFIX = "image";
  private static final int QUALITY = 100;
  private static final String EXTENSION = ".png";
  private static final Object LOCK = new Object();

  /** Whether or not the screenshot output directory needs cleared. */
  private static boolean outputNeedsClear = true;

  /**
   * Take a screenshot.
   *
   * @param activity Activity with which to capture a screenshot.
   */
  public static void snap(Activity activity) {
    try {
      File screenshotDirectory = obtainScreenshotDirectory(activity);

      int number = 1;
      File screenshot;
      do {
        screenshot = new File(screenshotDirectory, TAGLESS_PREFIX + Integer.toString(number++) + EXTENSION);
      } while (screenshot.exists());

      takeScreenshot(screenshot, activity);
    } catch (Exception e) {
      throw new RuntimeException("Unable to take screenshot.", e);
    }
  }

  /**
   * Take a screenshot with the specified tag.
   *
   * @param activity Activity with which to capture a screenshot.
   * @param tag Unique tag to further identify the screenshot.
   */
  public static void snap(Activity activity, String tag) {
    try {
      File screenshotDirectory = obtainScreenshotDirectory(activity);
      takeScreenshot(new File(screenshotDirectory, tag + EXTENSION), activity);
    } catch (Exception e) {
      throw new RuntimeException("Unable to take screenshot.", e);
    }
  }

  private static void takeScreenshot(File file, final Activity activity) throws IOException {
    DisplayMetrics dm = activity.getResources().getDisplayMetrics();
    final Bitmap bitmap = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, ARGB_8888);

    if (Looper.getMainLooper() == Looper.myLooper()) {
      // On main thread already, Just Do It™.
      getScreenshot(activity, bitmap);
    } else {
      // On a background thread, post to main.
      final CountDownLatch latch = new CountDownLatch(1);
      activity.runOnUiThread(new Runnable() {
        @Override public void run() {
          try {
            getScreenshot(activity, bitmap);
          } finally {
            latch.countDown();
          }
        }
      });
      try {
        latch.await();
      } catch (InterruptedException e) {
        Log.e(TAG, "Unable to get screenshot " + file.getAbsolutePath(), e);
        return;
      }
    }

    OutputStream fos = null;
    try {
      fos = new BufferedOutputStream(new FileOutputStream(file));
      bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, fos);
      bitmap.recycle();

      file.setReadable(true, false);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  private static void getScreenshot(Activity activity, Bitmap bitmap) {
    Canvas canvas = new Canvas(bitmap);
    // TODO support display rotation / orientation.
    activity.getWindow().getDecorView().draw(canvas);
  }

  private static File obtainScreenshotDirectory(Context context) throws IllegalAccessException {
    File screenshotsDir = context.getDir(SPOON_SCREENSHOTS, MODE_WORLD_READABLE);

    synchronized (LOCK) {
      if (outputNeedsClear) {
        deletePath(screenshotsDir, false);
        outputNeedsClear = false;
      }
    }

    // The call to this method and one of the snap methods will be the first two on the stack.
    StackTraceElement element = new Throwable().getStackTrace()[2];

    File dirClass = new File(screenshotsDir, element.getClassName());
    File dirMethod = new File(dirClass, element.getMethodName());
    createDir(dirMethod);
    return dirMethod;
  }

  private static void createDir(File dir) throws IllegalAccessException {
    File parent = dir.getParentFile();
    if (!parent.exists()) {
      createDir(parent);
    }
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IllegalAccessException("Unable to create output directory: " + dir.getAbsolutePath());
    }
    dir.setReadable(true, false);
    dir.setWritable(true, false);
    dir.setExecutable(true, false);
  }

  private static void deletePath(File path, boolean inclusive) {
    if (path.isDirectory()) {
      File[] children = path.listFiles();
      if (children != null) {
        for (File child : children) {
          deletePath(child, true);
        }
      }
    }
    if (inclusive) {
      path.delete();
    }
  }

  private Screenshot() {
    // No instances.
  }
}
