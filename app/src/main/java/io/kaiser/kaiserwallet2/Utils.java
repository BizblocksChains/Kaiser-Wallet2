package io.kaiser.kaiserwallet2;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by abc on 2014. 8. 27..
 */
public class Utils {



    public static String ByteToStr(byte[] nValue)
    {
        int len = nValue.length;

        StringBuffer strBuffer = new StringBuffer();

        for(int i=0; i<len; i++)
            strBuffer.append(String.format("%02X", nValue[i]));

        return strBuffer.toString();
    }

    public static byte[] StrToByte(String strData)
    {
        String strData2 = strData.replace(" ", "");
        int len = strData2.length();
        int half = len / 2;


        byte[] nData = new byte[half];
        for(int i=0; i<half; i++)
            nData[i] = (byte)Integer.parseInt(strData2.substring(i * 2, i * 2 + 2), 16);

        return nData;
    }

    public static byte[] DataToBleData( byte[] nData )
    {
        byte[] data = new byte[nData.length + 4];
        data[0] = 0x31;
        data[1] = (byte)(nData.length >> 8);
        data[2] = (byte)nData.length;
        /*for(int i = 0; i < nData.length; i++)
            data[3+i] = nData[i];*/
        System.arraycopy(nData, 0, data, 3, nData.length);

        int nBcc = 0;
        for(int i = 0; i < data.length-1; i++)
            nBcc = nBcc ^ data[i];
        data[data.length-1] = (byte)nBcc;

        Log.i("Ble", "DataToBleData Input : " + Utils.ByteToStr(data));

        return data;
    }

    public static byte[] BleDataToData( byte[] nBleData )
    {
        int nLen = (nBleData[1] << 8) + (nBleData[2] & 0xff);
        Log.i("Ble", "BleDataToData Length : " + nLen);
        byte[] data = new byte[nLen];
        /*for(int i = 0; i < nLen; i++)
            data[i] = nBleData[i + 3];*/
        System.arraycopy(nBleData, 3, data, 0, nLen);

        Log.i("Ble", "BleDataToData Input : " + Utils.ByteToStr(data));

        return data;
    }

    public static byte[] toByteArray(long value)
    {
        ByteBuffer bb = ByteBuffer.allocate(8);
        return bb.putLong(value).array();
    }



    public static String getBondSCTBleAddrByTSN(BluetoothAdapter bt, String strTSN) {

        Set<BluetoothDevice> myBondedDevices = bt.getBondedDevices();
        for (BluetoothDevice mydevice : myBondedDevices) {
            if(mydevice.getName().contains(strTSN)) {
                return mydevice.getAddress();
            }
        }
        return "";
    }

    public static String convBTCtoMCU(long nBtc)
    {
        long nVal1 = nBtc / 100000;
        long nVal2 = nBtc % 100000;

        Log.i("Ble", String.format("val1: %d, val2: %d",
                nVal1, nVal2));

        String strRes = String.format("%08d%-8s", nVal1, nVal2).replace(' ', '0');

        return strRes;
    }

    public static boolean isForegroundActivity(Context context, Class<?> cls) {
        if(cls == null)
            return false;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> info = activityManager.getRunningTasks(1);
        ActivityManager.RunningTaskInfo running = info.get(0);
        ComponentName componentName = running.topActivity;

        return cls.getName().equals(componentName.getClassName());
    }

    public static String getAppVersion(Context context) {

        // application version
        String versionName = "";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return versionName;
    }

    public static String getSetttingInfo(Context ctx, String strKey)
    {
        SharedPreferences setting = ctx.getSharedPreferences("setting", MODE_PRIVATE);

        return setting.getString(strKey, "");


    }

    public static void setSetttingInfo(Context ctx, String strKey, String strValue)
    {
        SharedPreferences setting = ctx.getSharedPreferences("setting", MODE_PRIVATE);
        SharedPreferences.Editor editor = setting.edit();

        editor.putString(strKey, strValue);
        editor.commit();

    }


    public static String getIMEI(Context ctx)
    {
        TelephonyManager telephonyManager = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = "";
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            imei = telephonyManager.getImei();
        }
        else
        {
            imei = telephonyManager.getDeviceId();
        }

        if (imei == null || imei.length() == 0) {
            imei = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        return imei;
    }

    public static String getPhoneNumber(Context ctx)
    {
        TelephonyManager tMgr = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
        return tMgr.getLine1Number();
    }


    public static int getDrawableResId(Context context, String variableName)
    {
        return Utils.getResourceId(context, variableName, "drawable", context.getPackageName());
    }

    public static int getResourceId(Context context, String variableName, String resourceName, String packageName)
    {
        try {
            return context.getResources().getIdentifier(variableName, resourceName, packageName);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
