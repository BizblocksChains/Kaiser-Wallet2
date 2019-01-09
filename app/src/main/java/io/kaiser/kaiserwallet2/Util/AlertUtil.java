package io.kaiser.kaiserwallet2.Util;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

import io.kaiser.kaiserwallet2.MainActivity;
import io.kaiser.kaiserwallet2.R;

public class AlertUtil {

    public static AlertDialog lastAlertDialog = null;

    /*
    메세지를 띄우고 The End를 누르면 앱을 종료합니다.
     */
    public static void terminateSimpleEnd(Context context, String title, String msg, DialogInterface.OnClickListener onEndListener)
    {
        //AlertUtil.showSimpleCore(context, title, msg, context.getResources().getString(R.string.app_message_00032), onEndListener, null, null);
    }

    /*
    리소스 아이디의 메세지를 띄우고 Ok를 누르면 반응 없이 Alert만 닫아집니다.
     */
    public static void showSimpleOk(Context context, String msg)
    {
        AlertUtil.showSimpleOk(context, msg, null);
    }

    /*
    리소스 아이디의 메세지를 띄우고 Ok를 누르면 리스너가 동작합니다.
     */
    public static void showSimpleOk(Context context, int resId, DialogInterface.OnClickListener onClickListener)
    {
        AlertUtil.showSimpleOk(context, context.getResources().getString(resId), onClickListener);
    }

    /*
    스크링 메세지를 띄우고 Ok를 누르면 리스너가 동작합니다.
     */
    public static void showSimpleOk(Context context, String msg, DialogInterface.OnClickListener onClickListener)
    {
        AlertUtil.showSimpleCore(context, null, msg, context.getResources().getString(R.string.app_message_ok), onClickListener, null, null);
    }

    /*
    Core : 메세지를 띄우고 버튼를 누르면 리스너가 동작합니다.
     */
    public static void showSimpleCore(Context context, String title, String msg, String okMsg, DialogInterface.OnClickListener onOkListener, String cancelMsg, DialogInterface.OnClickListener onCancelListener)
    {
        AlertUtil.showSimpleCore(context, title, msg, okMsg, onOkListener, cancelMsg, onCancelListener, null);
    }
    public static void showSimpleCore(Context context, String title, String msg, String okMsg, DialogInterface.OnClickListener onOkListener, String cancelMsg, DialogInterface.OnClickListener onCancelListener, View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if(title != null)
        {
            builder.setTitle(title);
        }
        builder.setMessage(msg);
        if(okMsg != null && onOkListener != null)
        {
            builder.setCancelable(false); //버튼이 추가되면 여백을 클릭해도 다이얼로그가 닫히지 않게 개발.
            builder.setPositiveButton(okMsg, onOkListener);
        }
        if(cancelMsg != null && onCancelListener != null)
        {
            builder.setCancelable(false);
            builder.setNegativeButton(cancelMsg, onCancelListener);
        }
        if(view != null)
        {
            builder.setView(view);
        }

        lastAlertDialog = builder.create();
        lastAlertDialog.show();
    }

    /*
    가장 최근에 열린 AlertDialog를 닫는 작업
     */
    public static void closeLastAlertDialog()
    {
        if(lastAlertDialog != null)
        {
            lastAlertDialog.dismiss();
            lastAlertDialog = null;
        }
    }

}
