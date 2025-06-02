package com.whl.quickjs.android;

public final class QuickJSLoader {
    private static boolean libraryLoaded = false;

    /**
     * 初始化QuickJS库
     * @return 是否成功加载库
     */
    public static boolean init() {
        return init(Boolean.FALSE);
    }

    /**
     * 初始化QuickJS库
     * @param bool 是否重定向标准输出和错误
     * @return 是否成功加载库
     */
    public static boolean init(Boolean bool) {
        if (!libraryLoaded) {
            try {
                System.loadLibrary("quickjs-android-wrapper");
                libraryLoaded = true;
                android.util.Log.i("QuickJSLoader", "Successfully loaded quickjs-android-wrapper library");

                if (bool.booleanValue()) {
                    try {
                        startRedirectingStdoutStderr("QuJs ==> ");
                    } catch (Throwable e) {
                        android.util.Log.e("QuickJSLoader", "Failed to redirect stdout/stderr: " + e.getMessage());
                    }
                }
                return true;
            } catch (Throwable e) {
                android.util.Log.e("QuickJSLoader", "Failed to load quickjs-android-wrapper library: " + e.getMessage());
                return false;
            }
        }
        return libraryLoaded;
    }

    /**
     * 检查库是否已加载
     * @return 是否已加载
     */
    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }

    public static native void startRedirectingStdoutStderr(String str);
}
