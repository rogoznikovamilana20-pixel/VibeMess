package org.telegram.messenger;

import org.telegram.messenger.regular.BuildConfig;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final Thread.UncaughtExceptionHandler pastHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            try {
                String stack = android.util.Log.getStackTraceString(exception);
                android.util.Log.e("VibeCrash", "Uncaught on thread [" + thread.getName() + "]: " + exception, exception);
                try {
                    java.io.File dir = new java.io.File(ApplicationLoader.applicationContext.getExternalFilesDir(null), "crashes");
                    if (dir != null) {
                        dir.mkdirs();
                        java.io.File logFile = new java.io.File(dir, "crash_" + System.currentTimeMillis() + ".txt");
                        java.io.FileWriter fw = new java.io.FileWriter(logFile);
                        fw.write("Thread: " + thread.getName() + "\n");
                        fw.write(stack);
                        fw.close();
                    }
                } catch (Throwable ignored) {
                }
            } catch (Throwable ignored) {
            }
            if (pastHandler != null) {
                pastHandler.uncaughtException(thread, exception);
            }
        });
    }
}
