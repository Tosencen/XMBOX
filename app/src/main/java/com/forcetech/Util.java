package com.forcetech;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class Util {
    public static Intent intent(Context ctx, String scheme) {
        return new Intent();
    }
    public static String scheme(String url) {
        return "";
    }
    public static int port(String scheme) {
        return 0;
    }
    public static String trans(ComponentName name) {
        return "";
    }
}
