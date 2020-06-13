package com.google.appinventor.components.runtime.util;

import android.os.Handler;

public class AsynchUtil {
    public static void runAsynchronously(Runnable call) {
        new Thread(call).start();
    }

    public static void runAsynchronously(final Handler androidUIHandler, final Runnable call, final Runnable callback) {
        new Thread(new Runnable() {
            public void run() {
                call.run();
                if (callback != null) {
                    androidUIHandler.post(new Runnable() {
                        public void run() {
                            callback.run();
                        }
                    });
                }
            }
        }).start();
    }
}