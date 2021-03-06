package craig.mccoy.com;

import android.util.Log;

public class MyLog {

    private static final boolean LOGGING = BuildConfig.DEBUG;

    public static void i(String tag, String message) {
        if (LOGGING) {
            Log.i(tag, message);
        }
    }

    public static void w(String tag, String message) {
        if (LOGGING) {
            Log.w(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (LOGGING) {
            Log.e(tag, message);
        }
    }
}
