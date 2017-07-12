package android.extensions;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by arpitgupta on 12/07/17.
 * Utils.
 */
class Utils {

    static int dpToPx(Context context, int dp) {
        if (context == null) {
            return 0;
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
