package io.kaiser.kaiserwallet2.Util;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

public class ToastUtil {

    public static void show(Context context, int resString)
    {
        Toast.makeText(context, resString, Toast.LENGTH_SHORT).show();
    }

    public static void show(Context context, String string)
    {
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }
}
