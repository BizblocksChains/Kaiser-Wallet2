package io.kaiser.kaiserwallet2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.hardware.fingerprint.FingerprintManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

import io.fabric.sdk.android.Fabric;
import io.kaiser.kaiserwallet2.Device.Bluetooth.BLESingleton;
import io.kaiser.kaiserwallet2.Permission.PermissionManager;
import io.kaiser.kaiserwallet2.Threads.ExchangeThread;
import io.kaiser.kaiserwallet2.Util.AlertUtil;
import io.kaiser.kaiserwallet2.Util.AppVersionUtil;
import io.kaiser.kaiserwallet2.Util.ToastUtil;
import io.kaiser.kaiserwallet2.databases.FireRemoteConfigParams;
import io.kaiser.kaiserwallet2.databases.FireRemoteConfigSingletone;
import io.kaiser.util.TestClass;
import type.ByteArray;
import us.monoid.web.Resty;

import static io.kaiser.kaiserwallet2.BuildConfig.DEBUG;
import static io.kaiser.kaiserwallet2.BuildConfig.IS_BASIC_NFC;
import static io.kaiser.kaiserwallet2.Define.URL_API_UPDATENOTIFYINFO;

public class MainActivity extends AppCompatActivity implements Handler.Callback {
    private static final int MSG_AUTH_UI_CUSTOM_TRANSPARENCY = 1010;

    private static final int PROCESS_NO_ACTION   = 0;
    private static final int PROCESS_CARD_REGIST = 1001;

    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    private static final String KEY_NAME_NOT_INVALIDATED = "key_not_invalidated";
    static final String DEFAULT_KEY_NAME = "default_key";

    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private SharedPreferences mSharedPreferences;

    private static final String TAG = MainActivity.class.getName();

    private boolean isLoginFrag = false;
    private boolean m_bHasEthereumAddress = false;
    private String m_strEthereumAddres = "";

    // 0일 경우 기본 생성
    // 1일 경우 메뉴에 의한 생성이다.

    private int m_iCreateKeyMode = 0;

    private TextView tvVersion;

    private WebView wb;
    private String m_currUrl = "";

    private String m_strwlno = "";


    //Input Tag 이미지 선택
    private static final String TYPE_IMAGE = "image/*";
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;




    private class ChangeWalletState {
        public String m_ChanginAddress="";
        public boolean m_bTx = false;
        public boolean m_bGetbalance = false;

        public void init()
        {
            m_bTx = false;
            m_bGetbalance = false;
            m_ChanginAddress = "";
        }
    }

    private ChangeWalletState m_cwState = new ChangeWalletState();

    public static final String getAppUrl() {
        if(BuildConfig.BUILD_DEBUG)
        {
            return APP_URL_DEBUG;
        }
        else
        {
            return APP_URL;
        }
    }

    private boolean m_bOnlyApp = false;

    public static MainActivity m_Activity;


    private Spass mSpass;
    private SpassFingerprint mSpassFingerprint;

    private Handler mFingerHandler;
    private Context mContext;

    //pairing 여부
    private boolean m_pairing = false;
    //serial number 저장
    private String m_serialNumber ="";
    private String m_PIN ="";
    //Wallet Info
    private JSONObject m_JWalletInfo = null;
    //Signature input
    private JSONObject m_JSigInput = null;
    //Signature list
    private JSONObject m_JSigList = null;

    private JSONObject m_JConfig = null;
    private JSONObject m_JappWalletInfo = null;
    private JSONArray m_CoinList = null;
    private HashMap<String,String> m_Mapimage = null;
    private String m_strParsedOrdering = "";
    private int m_iniMnemonicLen = 0;
    private String m_strCSN = "";
    private String m_strTSN = "";
    private String m_toAddress ="";
    private String m_strAmount = "";
    private String m_strFee = "";
    private String m_strGasLimit = "";

    private Thread getBalanceThread = null;
    private Thread checkBackground = null;

    private Thread setExchangeThread = null;
    private KaiserDB kdb = new KaiserDB();
    private KaiserERC20DBProvider erc20db = new KaiserERC20DBProvider();

    ProgressDialog pd = null; //프로그레스 이얼로그

    // ble prograssbar
    private RelativeLayout rlPrograss = null;
    private CircularProgressBar circularProgressBar = null;


    public static String  strMoneyType= "KRW";

    //권한 매니저
    PermissionManager mPermissionManager;


    // exchange thread
    private ExchangeThread mExchangeThread;

    /**
     * 결과를 받아 처리 한다.
     */
    class RestResult {
        String m_strResult;
        boolean m_bResult;
        String m_Message;
    };

    /**
     *
     * Transaction을 얻기 위한 루프
     */


    /**
     * 앱에 로드된 wallet info
     */

    private boolean isNeedGenerateSEED = false;

    public boolean getisNeedGenerateSEED(){
        return isNeedGenerateSEED;
    }

    public void set_isNeedGenerateSEED(boolean value){
        isNeedGenerateSEED = value;
    }


    private int m_iProcess = PROCESS_NO_ACTION;

    private boolean m_bFinishFinger = false;
    private boolean m_isSupportedFingerPrint = false; // 지문 인식 지원 여부

    protected boolean isSupportedFingerPrint() {
        return m_isSupportedFingerPrint;
    }

    protected void setSupportFingerPrint(){
        m_isSupportedFingerPrint = true;
    }
    protected void NotSupportFingerPrint() {m_isSupportedFingerPrint = false;}

    protected boolean isInitKey()    {
        if(Utils.getSetttingInfo(getApplicationContext(), "init_key").equals("1")) return true;

        return false;
    }

    protected void setInitKey()    {
        Utils.setSetttingInfo(getApplicationContext(), "init_key", "1");
    }

    protected void enableFingerPrint()   {
        Utils.setSetttingInfo(getApplicationContext(), "disableFingerPrint", Define.CONFIG_TRUE_CODE);
    }

    protected void disableFingerPrint(){
        Utils.setSetttingInfo(getApplicationContext(), "disableFingerPrint", Define.CONFIG_FALSE_CODE);
    }

    protected boolean isDisableFingerPrint(){
        if(Utils.getSetttingInfo(getApplicationContext(), "disableFingerPrint").equals(Define.CONFIG_TRUE_CODE)) return true;
        return false;
    }

    protected void enablePinNumber() {
        Utils.setSetttingInfo(getApplicationContext(), "disablePinNumber", Define.CONFIG_TRUE_CODE);
    }

    protected void disablePinNumber() {
        Utils.setSetttingInfo(getApplicationContext(), "disablePinNumber", Define.CONFIG_FALSE_CODE);
    }

    protected boolean isDisablePinNumber() {
        if(Utils.getSetttingInfo(getApplicationContext(), "disablePinNumber").equals(Define.CONFIG_TRUE_CODE)) return true;
        return false;
    }


    //app이 구동될 때 앱과 카드 사이의 페어링 여부를 체크한다.
    protected boolean pairingCheck(){
        if( !m_pairing )
            return false;

        if( m_serialNumber == null || m_serialNumber.length() != 12)
            return false;

        return true;
    }

    public boolean verifyPinNumber(String pinnumber)
    {
        return true;
    }


    // https://github.com/beders/Resty

    public JSONObject requestREST(String url,String params)    {
        try{
            JSONObject json = new JSONObject(params);
            return requestREST(url,json);
        }
        catch(JSONException e)
        {

        }

        return null;
    }

    public JSONObject requestREST(String url,JSONObject params)    {
        //Todo Here
        try {
            Resty r = new Resty();
            // post
            //JSONResource json = r.json(url,Resty.form(Resty.data()));


            // get
            us.monoid.json.JSONObject json = r.json(url).object();
            final String aaa = json.toString();

            wb.post(new Runnable() {
                @Override
                public void run() {
                    wb.evaluateJavascript(String.format("showResult11('%s')",aaa), null);
                }
            });

            // todo here
        }catch(Exception e)
        {

        }

        return null;
    }

    public void checkEthereumAddress()
    {
        m_bHasEthereumAddress = false;
        try {
            if(m_JWalletInfo == null) return ;

            if(!m_JWalletInfo.has(Define.JSON_LIST_WALLET_INFO)) return ;

            String strWalletInfo = m_JWalletInfo.getString(Define.JSON_LIST_WALLET_INFO);
            if (strWalletInfo != null) {

                JSONArray arr = new JSONArray(strWalletInfo);

                if (arr.length() > 0) {

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject j = arr.getJSONObject(i);
                        try {
                            String address = j.getString(Define.CONFIG_DEFAULT_WALLET);
                            String cointype = j.getString(Define.JSON_COIN_TYPE);

                            if(cointype != null && cointype.toUpperCase().equals("8000003C"))
                            {
                                m_strEthereumAddres = address;
                                m_bHasEthereumAddress = true;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }


                }

            }
        }
        catch(Exception e)
        {

        }
    }

    public JSONObject requestREST(String url)
    {
        return requestREST(url,"{}");
    }

    /**
     * 앱의 설정을 로드한다.
     * @return boolean
     */

    public boolean initConfig(){

        SharedPreferences setting = getApplicationContext().getSharedPreferences("setting", 0);
        String json_walletinfo = setting.getString(Define.CONFIG_TAG, "");

        if(json_walletinfo.isEmpty()){
            json_walletinfo = "{}";
        }

        try{
            m_JConfig = new JSONObject(json_walletinfo);

            String value = getConfigParam(Define.CONFIG_DISABLE_PIN); // 자동 입력 FALSE 일 경우 무조건 입력 받는다
            if(value == null || value.isEmpty()) value = Define.CONFIG_FALSE_CODE;
            setConfigParam(Define.CONFIG_DISABLE_PIN,value);

            value = getConfigParam(Define.CONFIG_DIABLE_FINGERPRINT);
            if(value == null || value.isEmpty()) value = Define.CONFIG_FALSE_CODE;
            setConfigParam(Define.CONFIG_DIABLE_FINGERPRINT,value);
/*
            value = getConfigParam(Define.CONFIG_DEFAULT_COINTYPE);
            if(value == null || value.isEmpty()) value = "";
            setConfigParam(Define.CONFIG_DEFAULT_COINTYPE,value);
*/
            //setConfigParam(Define.CONFIG_PIN_NUMBER,"123456");
            value = null; //앱을 초기화 하면 항상 000000으로 세팅함.
            if(value == null || value.isEmpty()) value = Define.CONFIG_DEFAULT_PIN_NUMBER;
            setConfigParam(Define.CONFIG_PIN_NUMBER,value);

            value = getConfigParam(Define.CONFIG_SET_PIN);
            if(value == null || value.isEmpty()) value = Define.CONFIG_FALSE_CODE;
            setConfigParam(Define.CONFIG_SET_PIN,value);


            value = getConfigParam(Define.CONFIG_DEFAULT_WALLET);
            if(value == null || value.isEmpty()) value = "";
            setConfigParam(Define.CONFIG_DEFAULT_WALLET,value);

            value = getConfigParam(Define.CONFIG_DEFAULT_CONTRACT);
            if(value == null || value.isEmpty()) value = "";
            setConfigParam(Define.CONFIG_DEFAULT_CONTRACT,value);

            value = getConfigParam(Define.CONFIG_DEFAULT_LANG);
            if(value == null || value.isEmpty()) value = "";
            setConfigParam(Define.CONFIG_DEFAULT_LANG,value);

            return true;
        }
        catch(JSONException e)
        {

        }

        return false;
    }
    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public String getQrCode(String file){

        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if(is != null) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            bitmap = toGrayscale(bitmap);
            String decoded = QrCodeReadActivity.scanQRImage(bitmap);
            Log.i("QrTest", "Decoded string=" + decoded);
            return decoded;
        }

        return "";

    }

    public boolean loadConfig(){
        SharedPreferences setting = getApplicationContext().getSharedPreferences("setting", 0);
        String json_walletinfo = setting.getString(Define.CONFIG_TAG, "");

        if(json_walletinfo.isEmpty()) return false;

        try{
            m_JConfig = new JSONObject(json_walletinfo);

            String value = getConfigParam(Define.CONFIG_DISABLE_PIN);
            if(value == null && value.isEmpty()) value = Define.CONFIG_FALSE_CODE;

            value = getConfigParam(Define.CONFIG_DIABLE_FINGERPRINT);
            if(value == null && value.isEmpty()) value = Define.CONFIG_FALSE_CODE;

            value = getConfigParam(Define.CONFIG_PIN_NUMBER);
            if(value == null && value.isEmpty()) value = Define.CONFIG_DEFAULT_PIN_NUMBER;

            m_PIN = value;

            return true;
        }
        catch(JSONException e)
        {

        }

        return false;
    }

    public String getConfigParam(String name)
    {
        if(m_JConfig == null) return null;

        try {
            String value = m_JConfig.getString(name);
            return value;
        }
        catch(JSONException e)
        {

        }

        return null;
    }

    public boolean setConfigParam(String name,String value)
    {
        if(m_JConfig == null) return false;
        try{
            m_JConfig.put(name,value);
            saveConfig();
            return true;
        }
        catch(JSONException e)
        {

        }
        return false;
    }

    /**
     * 앱의 설정을 저장한다.
     * @return boolean
     */

    public String getEthereumWallet()
    {
        if (m_CoinList != null) {
            try {
                for(int i=0;i<m_CoinList.length();i++)
                {
                    JSONObject item = m_CoinList.getJSONObject(i);
                    if(item.getString(Define.CONFIG_DEFAULT_COINTYPE).toUpperCase().equals("8000003C"))
                    {
                        String addr =  item.getString(Define.CONFIG_DEFAULT_WALLET);
                        addr = addAddressPrefix(addr);
                        return addr;

                    }
                }
            }
            catch(JSONException e)
            {

            }
        }
        return "";
    }

    public boolean hasEthereumWallet()
    {
        if (m_CoinList != null) {
            try {
                for(int i=0;i<m_CoinList.length();i++)
                {
                    JSONObject item = m_CoinList.getJSONObject(i);
                    if(item.getString(Define.CONFIG_DEFAULT_COINTYPE).toUpperCase().equals("8000003C"))
                    {
                        m_strEthereumAddres = item.getString(Define.CONFIG_DEFAULT_WALLET);
                        m_bHasEthereumAddress = true;
                        return true;
                    }
                }
            }
            catch(JSONException e)
            {

            }
        }


        return false;
    }

    /**
     * WalletInfo가 null인지 아닌지의 여부를 판단합니다.
     * @return boolean
     */
    public boolean isNullappWalletInfo()  {
        return (m_JWalletInfo == null);
    }

    /**
     * 페이링 여부 확인
     * @return boolean
     */
    public boolean isPairing() {
        if(isNullappWalletInfo()) return false;

        try{
            String pairing = m_JWalletInfo.getString("pairing");
            if(pairing.equals("01"))
            {
                return true;
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isSetSeed(){
        if(isNullappWalletInfo()) return false;

        try{
            String pairing = m_JWalletInfo.getString(Define.JSON_SEED);
            if(pairing.equals("01"))
                return true;

        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * TSN이 유효 확인
     * @param tsn
     * @return boolean
     */
    public boolean isCorrectTSN(String tsn)  {
        return (tsn.length() * 2 == 24);
    }

    /**
     * tsn을 확인 한다.
     * @return
     */
    public boolean hasTSN(){
        if(isNullappWalletInfo()) return false;

        try{
            String tsn = m_JWalletInfo.getString(Define.JSON_TSN);
            tsn = tsn.replaceAll(" ","");
            if(isCorrectTSN(tsn) )
            {
                return true;
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * wallet등 설정이 필요한지 확인 한다. 페어링 되어 있지 않으면 기존의 정보를 지우고 다시 생성한다.
     * @result NFC의 수행 여부를 반환한다.
     */
    public boolean checkAppEnvironment() {
        boolean result = true;
        m_iProcess = PROCESS_CARD_REGIST;
        if(!isPairing() || !hasTSN())
        {


            result = false;
        }

        return result;
    }

    /**
     * checkAppEnvironment를 수행 후 NFCActivate를 수행 한다.
     * @see
     */
    private void checkAppEnvironmentafterRunNFC() {

        if(!checkAppEnvironment())
        {
            boolean canBle = !IS_BASIC_NFC; //기본 NFC가 아니면 BLE기기, canBle(true):bleReader, canBle(false):androidNfc

            boolean needBleName = !bleSingleton.isBleConnect();

            if(canBle && needBleName)
            {
                //BLE기기 이면서 BLE 이름이 필요하면 입력받기
                inputBLEName();
            }
            else
            {
                m_PIN = getConfigParam(Define.CONFIG_PIN_NUMBER);
                Log.i("forDoc","pin number : "+m_PIN);

                runNFCActivity(NFCActivity.MODE_WALLETINFO,"device_nfc", m_PIN,"");
            }
        }
        else {
            wb.loadUrl(getAppUrl()+"main?imei="+Utils.getIMEI(getApplicationContext()));
        }
    }

    private String bleName = "";
    void inputBLEName()
    {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        String beforeSN = pref.getString("deviceSN", "");

        final EditText et = new EditText(MainActivity.this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(beforeSN);

        AlertUtil.showSimpleCore(this, getString(R.string.app_message_ble_reader_00001), getString(R.string.app_message_ble_reader_00002)+"\n"+getString(R.string.app_message_ble_reader_00003),

                getString(R.string.app_message_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.v(TAG, "Yes Btn Click");

                String value = et.getText().toString();
                Log.v(TAG, "inputBLEName : "+value);

                bleName = value;

                SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("deviceSN", value);
                editor.commit();


                ToastUtil.show(MainActivity.this, "Start Scan BLE..");
                rlPrograss.setVisibility(View.VISIBLE);
                circularProgressBar.enableIndeterminateMode(true);

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        final String targetName = bleName;

                        boolean retScanToConnect = bleSingleton.scanToConnectBle(targetName);
                        if(retScanToConnect)
                        {
                            Utils.setSetttingInfo(getApplicationContext(), "deviceSN", targetName);
                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    ToastUtil.show(getApplicationContext(), String.format("Success Connect ble : %s"    , targetName));
                                    rlPrograss.setVisibility(View.GONE);
                                    circularProgressBar.enableIndeterminateMode(false);
                                }
                            });
                        }
                        else
                        {

                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    ToastUtil.show(getApplicationContext(), String.format("Fail Connect ble : %s", targetName));
                                    rlPrograss.setVisibility(View.GONE);
                                    circularProgressBar.enableIndeterminateMode(false);
                                }
                            });
                        }
                    }
                }).start();


                dialog.dismiss();     //닫기
            }
        }
        , getString(R.string.app_message_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.v(TAG,"No Btn Click");
                dialog.dismiss();     //닫기
                // Event
            }
        }, et);
    }







    public void EmptyWalletInfo()
    {
        setConfigParam(Define.CONFIG_DEFAULT_WALLET,"");
        setConfigParam(Define.CONFIG_DEFAULT_COINTYPE,"");
        setConfigParam(Define.CONFIG_DEFAULT_PUBLICKEY,"");
        setConfigParam(Define.CONFIG_DEFAULT_CONTRACT,"");
        setConfigParam(Define.CONFIG_DEFAULT_ADDITIONAL,"");

        synchronized (balanaceObj) {

            balanaceObj.obj = new String("");
            balanaceObj.ct = "";
            balanaceObj.pk = "";
            balanaceObj.bl = "0";
            balanaceObj.addtional ="";
        }

        synchronized (m_txseObj) {

            m_txseObj.obj = new String("");
            m_txseObj.ct = "";
            m_txseObj.pk = "";
            m_txseObj.bl = "0";
            m_txseObj.addtional ="";
        }
    }

    public void ChangeWalletInfo()
    {

        String wallet = getConfigParam(Define.CONFIG_DEFAULT_WALLET);
        String cointype = getConfigParam(Define.CONFIG_DEFAULT_COINTYPE);
        String publikey = getConfigParam(Define.CONFIG_DEFAULT_PUBLICKEY);
        String contract = getConfigParam(Define.CONFIG_DEFAULT_CONTRACT);
        String additinal = getConfigParam(Define.CONFIG_DEFAULT_ADDITIONAL);

        synchronized (balanaceObj) {

            balanaceObj.obj = new String(wallet );
            balanaceObj.ct = cointype;
            balanaceObj.pk = publikey;
            balanaceObj.bl = "0";
            balanaceObj.addtional = additinal;
            balanaceObj.contract = contract;
        }

        synchronized (m_txseObj) {

            m_txseObj.obj = new String(wallet);
            m_txseObj.ct = cointype;
            m_txseObj.pk = publikey;
            m_txseObj.bl = "0";
            m_txseObj.addtional = additinal;
            m_txseObj.contract = contract;
        }

        String address = getConfigParam(Define.CONFIG_DEFAULT_WALLET);

        final String cfg_default_cointype = getConfigParam(Define.CONFIG_DEFAULT_COINTYPE);

        address = addAddressPrefix(address);



        final String sendAddress = address;
        wb.post(new Runnable() {
            @Override
            public void run() {
                wb.evaluateJavascript(String.format("update_WalletAddress('%s','%s')",sendAddress,cfg_default_cointype),null);
                wb.evaluateJavascript("update_history('[]','??')",null);
            }
        });
    }

    public void runNFCActivityJSON(int nMode, JSONObject json)
    {
        Intent intent = new Intent(MainActivity.this, NFCActivity.class);
        intent.putExtra("mode", nMode);
        // 안드로이드는 nfc로 고정
        intent.putExtra("type", "device_nfc");
        intent.putExtra("params", json.toString());
        Log.i("forDoc","NFCActivity1 mode : "+nMode);
        Log.i("forDoc","NFCActivity1 params : "+json);
        startActivityForResult(intent, nMode);
    }

    public void runCreatePinNFCActivity(int nMode, String newPin)
    {
        Intent intent = new Intent(MainActivity.this, NFCActivity.class);
        intent.putExtra("mode", nMode);
        intent.putExtra("type", "device_newpin");
        intent.putExtra("newPin",newPin);
        Log.i("forDoc","NFCActivity2 mode : "+nMode);
        Log.i("forDoc","NFCActivity2 newPin : "+newPin);
        startActivityForResult(intent, nMode);
    }

    public void runNFCActivity(int nMode, String deviceType, String strPin, String strBuffer)
    {
        Intent intent = new Intent(MainActivity.this, NFCActivity.class);
        intent.putExtra("mode", nMode);

        // 안드로이드는 nfc로 고정
        intent.putExtra("type", deviceType);
        intent.putExtra("pin", strPin);
        intent.putExtra("strBuffer", strBuffer);

        Log.i("forDoc","NFCActivity3 mode : "+nMode);
        Log.i("forDoc","NFCActivity3 type : "+deviceType);
        Log.i("forDoc","NFCActivity3 Pin : "+strPin);
        Log.i("forDoc","NFCActivity3 strBuffer : "+strBuffer);

        startActivityForResult(intent, nMode);
    }

    public RestResult call_REST(final String query,final JSONObject json)  {
        return call_REST2(query,json,true);
    }

    /**
     * REST API를 호출한다. 결과를 받기전까지 Lock을 건다.
     *
     * @param json
     * @return
     */
    public RestResult call_REST2(final String query,final JSONObject json,final boolean showProgress)  {
        if(showProgress) {
            Message message = showProgressHandler_1.obtainMessage();
            showProgressHandler_1.sendMessage(message);
        }
        final RestResult rr = new RestResult();
        final CountDownLatch latch = new CountDownLatch(1);

        @SuppressLint("StaticFieldLeak") AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {

                HttpURLConnection connection = null;
                try{

                    Log.d("call_REST","start");
                    URL url = new URL(query);
                    connection = (HttpURLConnection) url.openConnection();
                    Log.d("call_REST","created");
                    connection.setRequestMethod("PUT");
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestProperty("Connection","close");
                    connection.setRequestProperty("Content-Type", "application/json ;charset=UTF-8");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setReadTimeout(Define.DEFAULT_READ_TIMEOUT);
                    connection.setConnectTimeout(Define.DEFAULT_CONNECTION_TIMEOUT);
                    OutputStream osw = connection.getOutputStream();
                    osw.write(json.toString().getBytes("UTF-8"));
                    osw.flush();
                    Log.d("call_REST","flush");
                    osw.close();
                    Log.d("call_REST","close");
                    String inputLine = "";
                    connection.connect();
                    int responsecode = connection.getResponseCode();
                    Log.d("call_REST","response");
                    if(responsecode == 200) {
//                        DataInputStream dis = new DataInputStream(connection.getInputStream());

                        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                        StringBuffer content = new StringBuffer();
                        while ((inputLine = br.readLine()) != null) {
                            content.append(inputLine);
                        }

                        br.close();
                        inputLine  = content.toString();
                    }


                    Log.d("call_REST","input line : "+inputLine);
                    synchronized (rr)
                    {
                        rr.m_strResult = inputLine;
                    }
                    //
                    Log.d("call_REST","countDown");

                    Log.d("call_REST","disconnected");

                    //latch.countDown();
                    if(showProgress) {
                        Message message = showProgressHandler_2.obtainMessage();
                        showProgressHandler_2.sendMessage(message);
                    }
                    Log.d("call_REST","countDown stop");
                }
                catch(ProtocolException e)
                {
                    Log.d("call_REST","ProtocolException");

                    e.printStackTrace();

                    synchronized (rr)
                    {
                        rr.m_strResult = "failed";
                    }
                    if(showProgress) {
                        Message message = showProgressHandler_2.obtainMessage();
                        showProgressHandler_2.sendMessage(message);
                    }
                    latch.countDown();
                }
                catch(IOException e)
                {
                    Log.d("call_REST","IOException");
                    e.printStackTrace();
                    synchronized (rr)
                    {
                        rr.m_strResult = "failed";
                    }
                    if(showProgress) {
                        Message message = showProgressHandler_2.obtainMessage();
                        showProgressHandler_2.sendMessage(message);
                    }
                    latch.countDown();
                }
                finally {
                    Log.d("call_REST","finally");
                    if(showProgress) {
                        Message message = showProgressHandler_2.obtainMessage();
                        showProgressHandler_2.sendMessage(message);
                    }
                    latch.countDown();

                    if(connection != null)
                    connection.disconnect();
                }
                return rr;
            }


        };
        task.execute(query);
        try {
            Log.d("call_REST","await");
            latch.await(3, TimeUnit.SECONDS);
            task.cancel(true);
            Log.d("call_REST","stop");
        }catch(InterruptedException e)
        {

        }
        Log.i("PYH","call_REST  return rr.m_strResult : "+rr.m_strResult);
        return rr;

    }

    protected double m_dexponent = 0;

    protected double getPowexponenet(String cointype)
    {
        double exp = 0.0;
        switch (cointype){
            case "8000003D":
            case "8000003C":
                exp = 18.0;
                break;
            case "80000000":
            case "80000001":
            case "80000002":
            case "80000005":
            case "80000085":
            case "80000091":
            case "80000079":
            case "8000009C":
            case "8000001C":
            case "800000FD":
                exp = 8.0;
                break;
        }
        return exp;
//        return m_dexponent ;
    }

    public void appendLog(String text)
    {
//        sdcard/kaiserwallet.log"
        //String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()+"/kaiserwallet.log";
        //String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/kaiserwallet.log";
        //File logFile = new File(path);
        File logFile = new File(getExternalFilesDir(null), "kaiserwallet.log");

        Log.i("appendLog","path : "+getExternalFilesDir(null));
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
                Log.i("appendLog","check 1 : ");
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                Log.i("appendLog","check 2 : "+e);
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            Log.i("appendLog","check 3 : ");
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            Log.i("appendLog","check 4 : "+e);
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void setPowexponenet(double arg_exponent){

//        if(Define.PRINT_LOG && arg_exponent != m_dexponent) {
//            Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
//            Set<Thread> threads = map.keySet();
//
//            for (Thread thread : threads) {
//                System.out.println("Name: " + thread.getName() + ((thread.isDaemon()) ? "(Daemon)" : "(Main)"));
//                System.out.println("\t" + "소속그룹: " + thread.getThreadGroup().getName());
//                System.out.println();
//
//                //String stack = thread.getStackTrace().toString();
//                String nullValue = null;
//                try {
//                        nullValue.equals("Invoke null pointer exception");
//                }
//                catch(Exception e)
//                {
//                    String a = Log.getStackTraceString(e);
//                    synchronized (balanaceObj) {
//                        String bb = String.format("coin name %s , %s,arg_decimal %f,exponenet :  %f", balanaceObj.ct,balanaceObj.bl,arg_exponent,getPowexponenet(balanaceObj.ct));
//                        appendLog(bb+"\n"+a);
//                    }
//                }
//            }
//        }
        Log.i("","");

        m_dexponent = arg_exponent;
    }
/*
    protected double getPowexponenet(String cointype)
    {
        double exponenet = 18;
        if(cointype.equals("80000000")
                || cointype.equals("80000001") // BTC
                || cointype.equals("80000091")
                || cointype.equals("80000005") // DASH (not DSH)
                || cointype.equals("80000002") // LTC
                || cointype.equals("8000009C") // GOLD
                || cointype.equals("80000085") // LTC
                )
        {
            exponenet = 8;
        }

        return exponenet;
    }*/



// 나중에 16진수 판단하여 수정해야할 것
    protected String getAddressBycointype(String cointype,String origin,String strpubKey) {
        String result = origin;
        if(cointype.equals("80000000")
                || cointype.equals("80000001")
                || cointype.equals("80000002") // LTC
                || cointype.equals("80000005")
                || cointype.equals("80000085")
                || cointype.equals("80000079")
                || cointype.equals("8000009C") // GOLD
                || cointype.equals("800008FD") // GOLD
                || cointype.equals("80000091"))
        {
            result = SimpleCrypto.toRIPEMD160(strpubKey);
            if(result.length()/2 > 32)
            {
                result = result.substring(0,32*2);
            }
            result = paddingZeroSize(result);
        }

        return result ;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        this.getApplication();



    }




    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) || (keyCode == KeyEvent.KEYCODE_HOME))
        {
            AlertUtil.showSimpleCore(this, getResources().getString(R.string.app_message_00032), getResources().getString(R.string.app_message_00006)
            , getResources().getString(R.string.app_message_00032), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    terminateApp();
                }
            }, getResources().getString(R.string.app_message_cancel), null);
        }
        return super.onKeyDown(keyCode, event);
    }

    public void prepareFingerPrint() {
        if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return ;
        }

        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            //throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
        try {
            mKeyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            //throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }

        Cipher defaultCipher;
        Cipher cipherNotInvalidated;
        try {
            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipherNotInvalidated = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);


        if (!keyguardManager.isKeyguardSecure()) {
            // Show a message that the user hasn't set up a fingerprint or lock screen.
            Toast.makeText(this,
                    R.string.app_message_00005,
                    Toast.LENGTH_LONG).show();
            return;
        }

        createKey(DEFAULT_KEY_NAME, true);
        createKey(KEY_NAME_NOT_INVALIDATED, false);

        if (initCipher(defaultCipher, DEFAULT_KEY_NAME)) {

            // Show the fingerprint dialog. The user has the option to use the fingerprint with
            // crypto, or you can fall back to using a server-side verified password.
            fragment = new FingerprintAuthenticationDialogFragment();

            fragment.setCryptoObject(new FingerprintManager.CryptoObject(defaultCipher));
            boolean useFingerprintPreference = mSharedPreferences
                    .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                            true);

            if (useFingerprintPreference) {
                fragment.setStage(
                        FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
            } else {
                fragment.setStage(
                        FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
            }

            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);


        } else {

            // This happens if the lock screen has been disabled or or a fingerprint got
            // enrolled. Thus show the dialog to authenticate with their password first
            // and ask the user if they want to authenticate with fingerprints in the
            // future
            FingerprintAuthenticationDialogFragment fragment
                    = new FingerprintAuthenticationDialogFragment();
            fragment.setCryptoObject(new FingerprintManager.CryptoObject(defaultCipher));
            fragment.setStage(
                    FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        }

        return;

    }

    private FingerprintAuthenticationDialogFragment fragment;



    public BLESingleton bleSingleton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("TTTT","MainActivity onCreate");


        m_Activity = this;

        boolean voteEnabled = FireRemoteConfigSingletone.getInstance().getBooleanForKey(FireRemoteConfigParams.COIN_LISTING_VOTE_ENABLED);
        ToastUtil.show(this, "vote:"+voteEnabled);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        /*getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);*/


        // ADB - 연동 뷰 디버깅 활성화
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // <<<<<<<<<<<<<<ble test BLEPYH
        ActivityCompat.requestPermissions(m_Activity, new String[] {Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.ACCESS_COARSE_LOCATION},1);

        bleSingleton = BLESingleton.getInstance(this);
        bleSingleton.initBLE();

        // >>>>>>>>>>>>> ble test

        initConfig();

        Fabric.with(this, new Crashlytics());


        setContentView(R.layout.activity_main);

        tvVersion = (TextView)findViewById(R.id.tvVersion);

        wb = (WebView)findViewById(R.id.xwalkWebView);
        wb.setBackgroundColor(0x00000000);




        String value = getConfigParam(Define.CONFIG_INIT_DB);
        if(value == null || !value.equals(Define.CONFIG_TRUE_CODE))
        {
            kdb.dropTable();
            setConfigParam(Define.CONFIG_INIT_DB,Define.CONFIG_TRUE_CODE);
        }

        kdb.initTable();

        boolean haveTofillTOkens = false;


        value = getConfigParam(Define.CONFIG_INIT_ERC20_DB);
        if(value == null || !value.equals(Define.CONFIG_TRUE_CODE))
        {
            erc20db.dropTable();
            setConfigParam(Define.CONFIG_INIT_ERC20_DB,Define.CONFIG_TRUE_CODE);
            haveTofillTOkens = true;
        }

        erc20db.initTable();

        if(haveTofillTOkens)
        {
            erc20db.initTokens();
        }


        FirebaseMessaging.getInstance().unsubscribeFromTopic("notice");

        mPermissionManager = new PermissionManager(this);

        rlPrograss = findViewById(R.id.rlPrograss);
        circularProgressBar = findViewById(R.id.circularProgressbar);
        circularProgressBar.setProgressWithAnimation(65);

        rlPrograss.setVisibility(View.GONE);
        circularProgressBar.enableIndeterminateMode(false);





        OnCreate2();
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }


    public void updateVersionInfo()
    {
        String appVersion = Utils.getAppVersion(getApplicationContext());
        wb.evaluateJavascript(String.format("updateVersion('%s')",appVersion),null);
    }

    public boolean checkSupportLocale(String lang)
    {
        return RegexTest("en|ko|ja|vi|ru|zh",lang);
    }

    protected void OnCreate2()
    {
        wb.getSettings().setJavaScriptEnabled(true);
        Log.d("","");
        wb.loadUrl(getAppUrl() + "index");
        isLoginFrag = false;
        /*String mVersion = Utils.getMarketVersionFast("io.kaiser.kaiserwallet");
        String appVersion = Utils.getAppVersion(getApplicationContext());

        if(!mVersion.contains(appVersion))
        {
            //terminateApp();
        }*/

        getBalanceThread();
        txsHistory.start();

        mExchangeThread = new ExchangeThread();
        mExchangeThread.start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                    String strToken = FirebaseInstanceId.getInstance().getToken();
                    String strAppVersion = AppVersionUtil.getAppVersion(getApplicationContext())+" "+(IS_BASIC_NFC?"Basic":"BleReader");

                    JSONObject tokenJson = new JSONObject();
                    tokenJson.put("os","AOS");
                    tokenJson.put("appVersion",strAppVersion);
                    tokenJson.put("token",strToken);
                    call_REST2(url,tokenJson,false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();








        setExchange();

        wb.setWebChromeClient(new WebChromeClient() { //웹뷰 클라이언트(주소창 없애기 위해)

            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.i("Main", "url : " + url);
                if (!m_currUrl.equals(url)) {
                    view.loadUrl(url);

                    m_currUrl = url;
                }

                return true;
            }

            @Override
            public boolean onJsAlert (WebView view,
                                      String url,
                                      String message,
                                      final JsResult result)
            {
                AlertUtil.closeLastAlertDialog();
                
                new AlertDialog.Builder(m_Activity)
                        .setTitle("Kaiser Wallet alert")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok,
                                new AlertDialog.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int wicht)
                                    {
                                        result.confirm();
                                    }
                                }).setCancelable(false)
                        .create()
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, final String url, String message, final JsResult result) {

                AlertUtil.closeLastAlertDialog();

                new AlertDialog.Builder(m_Activity)
                        .setTitle("Kaiser Wallet confirm")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        Log.d("TTTT",String.format("url:%s",url));
                                        if(url.contains("verifypin"))
                                        {
                                            dialog.dismiss();

                                            alertVerifypinNoRsetMessage();
                                        }
                                        else
                                        {
                                            result.confirm();
                                        }
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        result.cancel();
                                    }
                                })
                        .setCancelable(false)
                        .create()
                        .show();
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) {
                    String url = view.getUrl();

                }
            }
/*
            @Override
            public void onPageFinished(WebView view, String url) {

            }*/

            // For 4.1 <= Android Version < 5.0
            public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
                Log.d(getClass().getName(), "openFileChooser : " + acceptType + "/" + capture);
                mUploadMessage = uploadFile;
                imageChooser();
            }

            // For Android Version 5.0+
            // Ref: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                System.out.println("WebViewActivity A>5, OS Version : " + Build.VERSION.SDK_INT + "\t onSFC(WV,VCUB,FCP), n=3");
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePathCallback;
                imageChooser();
                return true;
            }

            private void imageChooser() {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Log.e(getClass().getName(), "Unable to create Image File", ex);
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:"+photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType(TYPE_IMAGE);

                Intent[] intentArray;
                if(takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, Define.MODE_PICK_INPUT_FILE_IMAGE);
            }

            private File createImageFile() throws IOException {
                // Create an image file name
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";
                File storageDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                File imageFile = File.createTempFile(
                        imageFileName,  /* prefix */
                        ".jpg",         /* suffix */
                        storageDir      /* directory */
                );
                return imageFile;
            }


        });

        wb.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                // do screenshot
                // Page loading finished
                //Toast.makeText(MainActivity.this,"Page Loaded.",Toast.LENGTH_SHORT).show();

                Log.i("Main", "onPageFinished - url : " + url);

                updateVersionInfo();

                tvVersion.setText(""); //빈 값으로 초기화

                if (url.contains("index")) {

                    //인덱스 창일때만 버전을 보이도록 함
                    tvVersion.setText(AppVersionUtil.getAppVersion(getApplicationContext())+" "+(IS_BASIC_NFC?"Basic":"BleReader"));


                    if(!getConfigParam(Define.CONFIG_DEFAULT_LANG).isEmpty()) {
                        String lang = getConfigParam(Define.CONFIG_DEFAULT_LANG);

                        if(checkSupportLocale(lang))
                        {
                            changeLocale(lang);
                        }

                        updateVersionInfo();

                        //연락처 권한을 꼭 통과해야됨.
                        mPermissionManager.checkAppPermission(new PermissionManager.PermissionResultListener() {
                            @Override
                            public void granted() {
                                //처음 index 페이지가 로딩될 때 지문인증보다 먼저 아래 코드가 수행됨
                                if(mPermissionManager.isSupportFingerPrintDevice()) {
                                    setSupportFingerPrint(); // 지문 인식 지원

                                    if (mPermissionManager.hasEnrolledFingerprints()) {
                                        prepareFingerPrint();
                                        return;
                                    } else {
                                        AlertUtil.showSimpleCore(getApplicationContext(), null, getResources().getString(R.string.app_message_00051),
                                                getResources().getString(R.string.app_message_00032), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                clearWalletInfo();
                                                terminateApp();
                                            }
                                        }, null, null);
                                        return;
                                    }

                                }else {
                                    checkAppEnvironmentafterRunNFC();
                                }
                            }

                            @Override
                            public void denied() {
                                ToastUtil.show(MainActivity.this, R.string.app_message_00070);
                            }
                        });


                    }
                }
                else if(url.contains("transmit"))
                {
                    String fee = "10000000000";
                    String balance = "";
                    synchronized (balanaceObj)
                    {
                        balance = balanaceObj.bl;
                    }



                    String cointype = getConfigParam(Define.CONFIG_DEFAULT_COINTYPE);
                    String contract = getConfigParam(Define.CONFIG_DEFAULT_CONTRACT);
                    String address = getConfigParam(Define.CONFIG_DEFAULT_WALLET);
                    String lists = erc20db.toString();

                    if(cointype!=null)
                    {
                        cointype = cointype.toUpperCase();
                    }
                    if(contract!=null)
                    {
                        contract = contract.toUpperCase();
                    }

                    wb.evaluateJavascript(String.format("update_contract_list('%s')",lists),null);
                    wb.evaluateJavascript(String.format("setTransmitBalance('%s','%s','%.0f')",balance,fee,Math.pow(10,18)),null);
                    wb.evaluateJavascript(String.format("update_sendinfo('%s','%s')",cointype,contract),null);
                    wb.evaluateJavascript(String.format("update_WalletAddress('%s','%s')",address,cointype),null);
                }
                else if(url.contains("check_nodekey"))
                {

                    ArrayList<Integer> list = new ArrayList<Integer>();
                    boolean loop = true;
                    Random rand = new Random();
                    while(list.size() < 3)
                    {
                        int newaa = rand.nextInt(24);
                        if(list.indexOf(newaa) == -1)
                        {
                            list.add(newaa);
                        }
                    }

                    JSONArray arr = new JSONArray();

                    for(int i=0;i<list.size();i++)
                    {
                        arr.put(list.get(i));
                    }
                    m_strwlno = arr.toString();
                    wb.evaluateJavascript(String.format("update_wl('%s')",m_strwlno ),null);
                }
                else if(url.contains("nodekey") || url.contains("nodekey2") )
                {
                    wb.evaluateJavascript(String.format("emptyList()"),null);


                    if(m_strParsedOrdering == null || m_strParsedOrdering.isEmpty()) return ;

                    ByteArray ar = new ByteArray(m_strParsedOrdering);

                    byte[] bytes = ar.getBytes();

                    for(int i=0;i<m_iniMnemonicLen ;i++)
                    {

                        byte[] tmp = new byte[2];
                        tmp[0] = bytes[i*2];
                        tmp[1] = bytes[i*2+1];

                        String key = SimpleCrypto.toHex(tmp);
                        wb.evaluateJavascript(String.format("insertNewImage('%d','%s')",i+1,m_Mapimage.get(key)),null);

                    }
                }

                else if(url.contains("main")) {

                    if(m_CoinList != null)
                    {
                        String coins = m_CoinList.toString();
                        Log.i("viewupdate","m_CoinList.toString() "+coins);
                        Log.i("Main", "onPageFinished - coins : " + coins);
                        wb.evaluateJavascript(String.format("update_coin('%s')",coins),null);
                    }

                    String tokens = erc20db.toString();
                    Log.i("Main", "onPageFinished - tokens : " + tokens);
                    wb.evaluateJavascript(String.format("update_erc20tokens('%s')",tokens),null);

                    wb.evaluateJavascript(String.format("update_walletinfo('%s')",getConfigParam(Define.CONFIG_LIST_WALLET_INFO)),null);
                    String walletinfo = getConfigParam(Define.CONFIG_LIST_WALLET_INFO);
                    Log.i("Main", "onPageFinished - walletinfo : " + walletinfo);

                    String address = getConfigParam(Define.CONFIG_DEFAULT_WALLET);
                    //if(!getConfigParam(Define.CONFIG_DEFAULT_COINTYPE).equals("80000000") && !getConfigParam(Define.CONFIG_DEFAULT_COINTYPE).equals("80000001"))
                    String cointype = getConfigParam(Define.CONFIG_DEFAULT_COINTYPE);
                    address = addAddressPrefix(address);


                    wb.evaluateJavascript(String.format("update_WalletAddress('%s','%s')",address,cointype),null);


                    String token = FirebaseInstanceId.getInstance().getToken();
                    Log.i("FireBaseToken Test","tttt "+token);




                }
                if(url.contains("transmit"))
                {
                    if(m_CoinList != null)
                    {
                        String coins = m_CoinList.toString();
                        wb.evaluateJavascript(String.format("update_coin('%s')",coins),null);
                    }

                    String tokens = erc20db.toString();
                    wb.evaluateJavascript(String.format("update_erc20tokens('%s')",tokens),null);

                    wb.evaluateJavascript(String.format("update_walletinfo('%s')",getConfigParam(Define.CONFIG_LIST_WALLET_INFO)),null);
                    String walletinfo = getConfigParam(Define.CONFIG_LIST_WALLET_INFO);

                }

                else if (url.contains("card"))
                {
                    String kisAddress = null;

                    String strWalletInfo = getConfigParam(Define.CONFIG_LIST_WALLET_INFO);
                    try {

                        JSONArray jsonArray = new JSONArray(strWalletInfo);

                        if(jsonArray != null)
                        {
                            for(int i=0; i<jsonArray.length(); i++)
                            {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                String coinType = jsonObject.getString(Define.JSON_COIN_TYPE);
                                String strPubKey = jsonObject.getString(Define.JSON_PUBLIC_KEY);
                                if(coinType.equals("80005E00"))
                                {
                                    String result = getAddressFromAPI(strPubKey, coinType,"");
                                    JSONObject jResult = new JSONObject(result);

                                    if(jResult.has("fromAddress"))
                                    {
                                        kisAddress = jResult.getString("fromAddress");
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }


                    if(kisAddress != null)
                    {
                        KYCManager kycManager = KYCManager.getInstance(getApplicationContext());

                        String original_tsn = SimpleCrypto.fromHex(m_strTSN);
                        KYCManager.getInstance(getApplicationContext()).setCardTSN(original_tsn);
                        KYCManager.getInstance(getApplicationContext()).setKaiserWalletAddress(kisAddress);

                        KYCInfo kycInfo = kycManager.getKYCInfo();
                        wb.evaluateJavascript(String.format("setCertiData('%s','%s','%s','%s','%s')", kycInfo.strIMEI, kycInfo.strFCMToken, kycInfo.strPhoneNumber, kycInfo.strCardTSN, kycInfo.strKaiserWalletAddress),null);
                        Log.i("","");
                    }
                    else
                    {
                        if(wb.canGoBack())
                        {
                            wb.goBack();
                        }

                        AlertUtil.showSimpleOk(getApplicationContext(), getResources().getString(R.string.app_message_00066));
                    }
                }

                else {
                    //wb.loadUrl(APP_URL + "main?device=" + DEVICE_TYPE);
                }
            }

        });

        wb.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public void KeypadShow(){
                hideSoftInputWindow(wb,true);
            }


            @JavascriptInterface
            public void AppRequst(String cmd){
                Log.i("Main", "javascriptinterface AppRequst : " + cmd);
                AppRequestProcess(cmd);

            }

            @JavascriptInterface           // For API 17+
            public void performClick(String strFunc, String strDeviceType, String strMode, String strAmount) {
                Log.i("forDoc", "javascriptinterface performClick strFunc: " + strFunc);
                Log.i("forDoc", "javascriptinterface performClick strDeviceType: " + strDeviceType);
                Log.i("forDoc", "javascriptinterface performClick strMode: " + strMode);
                Log.i("forDoc", "javascriptinterface performClick strAmount: " + strAmount);
                //Toast.makeText (MainActivity.this, strData, Toast.LENGTH_SHORT).show();


                if (strFunc.equals("qr")) {
                    Intent intent = new Intent(MainActivity.this, QrCodeReadActivity.class);
                    startActivityForResult(intent, QrCodeReadActivity.REQEUST_CODE_SCAN_QR_CODE);
                } else if (strFunc.equals("verifyPin")) {
                    boolean verifyPinNumber = false;
                    String pinnumber = "";
                    try {
                        JSONObject reader = new JSONObject(strDeviceType);
                        pinnumber = reader.getString("pin");

                        JSONObject json = new JSONObject();
                        json.put(Define.JSON_PIN_NUMEBR, pinnumber);
                        runNFCActivityJSON(NFCActivity.MODE_VERIFY_PIN, json);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (verifyPinNumber) {
                        //runNFCActivity(NFCActivity.MODE_PAIRING, "device_nfc", pinnumber, "");
                    } else {

                    }


                } else if (strFunc.equals("authenticate")) {
                    String cointype = getConfigParam(Define.CONFIG_DEFAULT_COINTYPE);

              /*    synchronized (kdb)
                    {
                        if(kdb.hasUnconfirmCount(cointype))
                        {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    return ;
                                }
                            });
                            builder.setMessage("등록된 지문이 없습니다. 지문을 먼저 등록해 주세요.");
                            builder.setCancelable(false); // 뒤로가기 버튼 차단
                            builder.show(); // 다이얼로그실행
                            return ;
                        }
                    }*/

                    m_bOnlyApp = true;
                    int nMode = 0;
                    RestResult rr = new RestResult();
                    final JSONObject tojson = new JSONObject();
                    JSONObject json = null;
                    JSONObject expJson = null;
                    try {

                        json = new JSONObject(strDeviceType);

                        String fromAddress = getConfigParam(Define.CONFIG_DEFAULT_WALLET);
                        String strPubkey1 = getConfigParam(Define.CONFIG_DEFAULT_PUBLICKEY);
                        String contract = getConfigParam(Define.CONFIG_DEFAULT_CONTRACT);

                        String strtoAddress = json.getString("toAddress");

                        String result = getAddressFromAPI(strPubkey1, cointype,contract);
                        expJson = new JSONObject(result);

                        double tmpexp = expJson.getDouble("exponent");

                        double origin_amount = TestClass.parseDouble(json.getString("amount"),"2");
//                        double exponenet = getPowexponenet(cointype);
                        double amount = origin_amount * Math.pow(10, tmpexp);


                        String strFee = json.getString("feeAmount");
                        String strGasLimit = json.getString("gaslimit");

                        double fee = TestClass.parseDouble(json.getString("feeAmount"),"3");
                        double gasLimit = TestClass.parseDouble(json.getString("gaslimit"),"4");


                        String toAddress = strtoAddress;

                        m_toAddress = strtoAddress;
                        m_strFee = strFee;//String.format("%f", fee);
                        m_strGasLimit = strGasLimit;//String.format("%f", gasLimit);

                        strtoAddress = remove0xHexString(strtoAddress);
                        strtoAddress = paddingZeroSize(strtoAddress);

                        String strAmount1 = String.format("%.0f", amount);
                        m_strAmount = strAmount1;
                        strAmount1 = String.format("%016.0f", amount);

                        // 바이너리로 바꿀때 8바이트를 맞춰 준다.

                        if (strAmount1.length() % 2 > 0) {
                            strAmount1 = "0" + strAmount1;
                        }

                        if (strAmount1.length() > 16) {
                            strAmount1 = strAmount1.substring(0, 16);
                        }

                        tojson.put(Define.JSON_SIGN_COINTYPE, cointype);
                        tojson.put(Define.JSON_SIGN_DESTADDR, getAddressBycointype(cointype, strtoAddress, strPubkey1));
                        tojson.put(Define.JSON_SIGN_AMOUNT, strAmount1);

                        tojson.put(Define.JSON_SIGN_VERIFY, verify);

                        JSONObject obj = new JSONObject();

                        obj.put("fromAddress", fromAddress);
                        obj.put("toAddress", toAddress);
                        obj.put("amount", String.format("%f", amount));
                        Log.i("Main", "javascriptinterface performClick json: " + json);

//                        double dFee = TestClass.parseDouble(""+fee);
//                        double dGasLimit = TestClass.parseDouble(""+gasLimit);
//
//                        String strFee = String.valueOf(dFee);
//                        String strGasLimit = String.valueOf(dGasLimit);

                        obj.put("fee", strFee);
                        obj.put("gasprice", strFee);
                        obj.put("gaslimit", strGasLimit);


                        /*
                        if(cointype.equals("80000000") || cointype.equals("80000001") || cointype.equals("80000005") || cointype.equals("80000085") || cointype.equals("80000091")|| cointype.equals("8000009C") || cointype.equals("800008FD"))
                        {
                            m_strFee =  String.format("%f", fee);
                            obj.put("fee", String.format("%.0f", fee));
                        }
                        else//  8000003C : ETH
                        {
                            m_strGasLimit=  String.format("%.0f", gasLimit);
                            obj.put("gasprice", String.format("%.0f", fee));
                            obj.put("gaslimit", String.format("%.0f", gasLimit));
                        }
                        */


                        obj.put("cointype", cointype);
                        obj.put("strPubkey", strPubkey1);
                        obj.put("strSignedData", "");
                        obj.put("contract", contract);

                        String strIMEI = Utils.getIMEI(getApplicationContext());
                        obj.put("imei", strIMEI);
                        String strPhone = Utils.getPhoneNumber(getApplicationContext());
                        obj.put("phone", strPhone);
                        String original_tsn = SimpleCrypto.fromHex(m_strTSN);
                        obj.put("tsn", original_tsn);
                        // imei, tsn

                        Toast.makeText(MainActivity.this, R.string.app_message_00003, Toast.LENGTH_SHORT).show();
                        rr = call_REST(Define.URL_API_READY, obj);
                        Log.i("Main", "javascriptinterface performClick rr: " + rr);

                        JSONObject jjj = new JSONObject(rr.m_strResult);
                        String isError  = jjj.getString("result");

                        if (isError.equals("error")){
                            int dResult = jjj.getInt("code");
                            if (dResult == -1){
                                Toast.makeText(MainActivity.this, R.string.app_message_00062, Toast.LENGTH_LONG).show();
                            }else if (dResult == -2){
                                Toast.makeText(MainActivity.this, R.string.app_message_00063, Toast.LENGTH_LONG).show();
                            }else {
                                Toast.makeText(MainActivity.this, R.string.app_message_00064, Toast.LENGTH_LONG).show();
                            }

                            return;
                        }


                        Toast.makeText(MainActivity.this, R.string.app_message_00004, Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    if (strMode.equals("reg"))
                        nMode = NFCActivity.MODE_REGIST;
                    else
                        nMode = NFCActivity.MODE_SIGN;


                    String strResult = "";
                    JSONArray hashlist = null;

                    try {
                        JSONObject json_strResult = new JSONObject(rr.m_strResult);
                        //strResult = json_strResult.getString("gethash");
                        Log.i("forDoc", "performClick authenticate json_strResult: " + json_strResult);


                        hashlist = json_strResult.getJSONArray("gethash");
                        for (int hashlist_count = 0; hashlist_count < hashlist.length(); hashlist_count++) {
                            strResult += hashlist.getString(hashlist_count);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Intent intent = new Intent(MainActivity.this, NFCActivity.class);
                    intent.putExtra("mode", nMode);
                    // 안드로이드는 nfc로 고정
                    intent.putExtra("type", "device_nfc");
                    intent.putExtra("JSON_READY", rr.m_strResult);

                    Log.i("forDoc", "performClick authenticate mode: " + nMode);
                    Log.i("forDoc", "performClick authenticate JSON_READY: " + rr.m_strResult);

                    if (nMode == NFCActivity.MODE_SIGN) {
                        String strIn = strResult;
                        if (testSignValue(strIn))
                            Toast.makeText(MainActivity.this, R.string.app_message_00007, Toast.LENGTH_SHORT).show();

                        int iCount = (strIn.length() / 2) / 32;

                        String strNewIn = "";


                        String strInLen = String.format("%04X", strNewIn.length() / 2);

                        try {
                            tojson.put(Define.JSON_PIN_NUMEBR, m_PIN);    //App에서 전달 받아야함.
                            tojson.put(Define.JSON_SIGN_INLEN, strInLen);
                            tojson.put(Define.JSON_SIGN_INPUT, strNewIn);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        intent.putExtra("strBuffer", tojson.toString());
                    }
                    Log.i("forDoc", "performClick authenticate strBuffer: " + tojson.toString());
                    Log.i("forDoc", "performClick authenticate nMode: " + nMode);

                    startActivityForResult(intent, nMode);

                } else if (strFunc.equals("fcm")) {
                    // [START subscribe_topics]
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("notice");
                    // [END subscribe_topics]

                    // Log and toast
                    String msg = getString(R.string.msg_subscribed);
                    Log.d(TAG, msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                } else if (strFunc.equals("fcm_key")) {
                    final String strToken = Utils.getSetttingInfo(getApplicationContext(), "token");
                    //wb.evaluateJavascript(String.format("setSender(%s)", strToken), null);
                    new Handler().post(new Runnable() { // new Handler and Runnable
                        @Override
                        public void run() {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("token", strToken);
                            clipboard.setPrimaryClip(clip);
                        }
                    });
                } else if (strFunc.equals("fcm_log")) {
                    // Get token
                    String token = FirebaseInstanceId.getInstance().getToken();

                    // Log and toast
                    String msg = getString(R.string.msg_token_fmt, token);
                    Log.d(TAG, msg);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                } else if (strFunc.equals("signin")) {
                    /* 앱 시작시 실행 */
                    m_bOnlyApp = true;
                    int nMode = NFCActivity.MODE_AUTH;


                    Intent intent = new Intent(MainActivity.this, NFCActivity.class);
                    intent.putExtra("mode", nMode);
                    // 안드로이드는 nfc로 고정
                    intent.putExtra("type", "device_nfc");
                    startActivityForResult(intent, nMode);

                }


            }
        }, "ok");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        if (getIntent().getExtras() != null) {
            m_bOnlyApp = false;
            Intent intent = new Intent(MainActivity.this, NFCActivity.class);
            boolean bExistDeviceKey = false;
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
                if (key.equals("type")) {
                    bExistDeviceKey = true;
                }

                intent.putExtra(key, value.toString());
            }
            if (bExistDeviceKey) {
                intent.putExtra("mode", NFCActivity.MODE_SIGN);
                startActivityForResult(intent, NFCActivity.MODE_SIGN);
            }

        }
        // [END handle_data_extras]

    }

//    Thread exchangeThread = null;


    private void alertVerifypinNoRsetMessage()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setPositiveButton(R.string.app_message_ok, null);
        builder.setMessage(R.string.app_message_00067);
        builder.setCancelable(false); // 뒤로가기 버튼 차단
        builder.create().show(); // 다이얼로그실행
    }

    public void setWebViewElementData(String type, String key, String value)
    {
        wb.loadUrl("javascript:document.getElementBy"+type+"('"+key+"').value ='"+value+"';");
    }



    public boolean hideSoftInputWindow(View edit_view, boolean bState) {

        InputMethodManager imm = (InputMethodManager) getSystemService
                (Context.INPUT_METHOD_SERVICE);

        if ( bState )
            return imm.showSoftInput(edit_view, 0);
        else
            return imm.hideSoftInputFromWindow
                    (edit_view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

    }




    /**
     * 주소 부분의 0x를 제거한다.
     * @param origin
     * @return 0x를 제거한 스트링이 반환된다.
     */
    private String remove0xHexString(String origin)  {
        return origin.replace("0x","");
    }

    /**
     * paddingZeroSize의 alias 길이는 64로 고정한다.
     *
     * @param origin
     * @return 변경된 스트링
     */

    private String paddingZeroSize(String origin) {
        return paddingZeroSize(origin,64);
    }

    /**
     * 지정된 패딩 만큼 자르거나 0으로 채운다.
     * @param origin 수정될 문자열
     * @param padding 문자열의 길이를 조정할 길이
     * @return 변경된 스트링
     */
    private String paddingZeroSize(String origin,int padding)  {
        int iLen = 0;
        if( origin.length() < padding )
        {
            iLen = padding - origin.length();
            for(int i=0; i<iLen; i++)
                origin = origin + "0";
        }
        else
            origin = origin.substring(0, padding);

        return origin;
    }

    /**
     * 입력 스트링이 유요한지 평가 합니다.
     * @param strIn
     * @return true,false
     */
    private boolean testSignValue(String strIn) {

        return (strIn == null || strIn.length() == 0 || (((strIn.length()/2) % 32) != 0));
    }


    private String m_OrderingNumbers = "000000";
    private String m_reservPin = "";

    public static boolean RegexTest(String pattern,String origin)
    {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(origin);
        return m.find();
    }

    public static String addAddressPrefix(String address)
    {
        if(RegexTest("^[0-9A-Fa-f]+$",address))
        {
            address = "0x"+ address;
        }
        return address;
    }

    public void changeLocale(String locale)
    {
        Log.i("changeLocale","locale : "+locale);
        Locale mLocale = new Locale(locale);
        Configuration config = new Configuration();
        config.locale = mLocale;
        getResources().updateConfiguration(config, null);

    }

    /**
     * setClipboard
     * @param context
     * @param text
     */
    private void setClipboard(Context context, String text) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
        }

        Toast.makeText(MainActivity.this,R.string.app_message_00002,Toast.LENGTH_SHORT).show();

    }

    private String appRequestProcess_command_createPin(JSONObject json, JSONObject ret) throws JSONException
    {
        m_PIN = json.getString("pin");

        if(!RegexTest("^[0-9]{6}$",m_reservPin) || !RegexTest("^[0-9]{6}$",m_PIN))
        {
            wb.post(new Runnable() {
                @Override
                public void run() {
                    wb.evaluateJavascript("gofail()",null);
                }
            });

            return "fail";
        }

        if(!m_PIN.equals(m_reservPin))
        {
            wb.post(new Runnable() {
                @Override
                public void run() {
                    wb.evaluateJavascript("gofail()",null);
                }
            });

            return "fail";
        }

        setConfigParam(Define.CONFIG_SET_PIN,Define.CONFIG_TRUE_CODE);
        runCreatePinNFCActivity(NFCActivity.MODE_SET_NEWPIN, m_PIN);

        return ret.toString();
    }

    private String appRequestProcess_command_confirmPin(JSONObject json, JSONObject ret) throws JSONException
    {
        m_PIN = json.getString("pin");

        setConfigParam(Define.CONFIG_SET_PIN,Define.CONFIG_TRUE_CODE);
        runCreatePinNFCActivity(NFCActivity.MODE_SET_NEWPIN, m_PIN);


        return ret.toString();
    }
    private String appRequestProcess_command_exchange(JSONObject json, JSONObject ret) throws JSONException
    {
        if(json.has("moneyType"))
        {
            synchronized (balanaceObj)
            {
                String bal = balanaceObj.bl.trim();
                String coinType = balanaceObj.ct.trim();
                String strContract = balanaceObj.contract.trim();
                final String moneyType = json.getString("moneyType");

                if(RegexTest("^[0-9]+(\\.[0-9]+)$",bal))
                {
                    //환율 환전
                    final double bbal = TestClass.parseDouble(bal,"6");
                    strMoneyType = moneyType;

                    boolean isContrack = false;
                    if (!strContract.equals("")){
                        isContrack = true;
                    }

                    final String strValue =  getExchangeRate(coinType, strMoneyType, bbal, isContrack);
                    final String strExchangeList = getStrExchangedList(coinType);
                    final String strExchangeInfo = getExchangListInfo(coinType,strMoneyType);

                    Log.d("","");

                    wb.post(new Runnable() {
                        @Override
                        public void run() {
                            wb.evaluateJavascript(String.format("update_exchange('%s','%s')", strValue, strMoneyType), null);
                            wb.evaluateJavascript(String.format("update_exchange_list('%s','%s')", strExchangeList, strExchangeInfo), null);
                        }
                    });
                }
            }
        }


        return ret.toString();
    }

    private String appRequestProcess_command_lang(JSONObject json, JSONObject ret) throws JSONException
    {
        if (json.has("value")) {
            final String value = json.getString("value");
            if (checkSupportLocale(value)) {
                setConfigParam(Define.CONFIG_DEFAULT_LANG, value);
                changeLocale(value);
                wb.post(new Runnable() {
                    @Override
                    public void run() {
                        isLoginFrag = false;
                        wb.loadUrl(getAppUrl() + value);
                    }
                });
            } else {
                terminateApp();
            }
        }

        return ret.toString();
    }

    private String appRequestProcess_command_deleteERC20Wallet(JSONObject json, JSONObject ret) throws JSONException
    {
        if (json.has("contract")) {
            String contract = json.getString("contract");

            if (erc20db.delete(contract)) {
                EmptyWalletInfo();
                String str = getResources().getString(R.string.app_message_00052);
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();

                Message message2 = showProgressHandler_2.obtainMessage();
                showProgressHandler_2.sendMessage(message2);


                final String tokens = erc20db.toString();
                wb.post(new Runnable() {
                    @Override
                    public void run() {
                        wb.evaluateJavascript(String.format("update_erc20tokens('%s')", tokens), null);
                        wb.evaluateJavascript(String.format("update_WalletAddress('%s','%s')", "", ""), null);
                    }
                });
            } else {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.app_message_00053), Toast.LENGTH_LONG).show();
            }
        }

        return ret.toString();
    }
    private String appRequestProcess_command_addERC20(JSONObject json, JSONObject ret) throws JSONException
    {
        String contract = json.getString("contract");
        if (!RegexTest("(0x)?[0-9a-fA-F]+", contract)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(R.string.app_message_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });
            builder.setMessage(R.string.app_message_00054);
            builder.setCancelable(false); // 뒤로가기 버튼 차단
            builder.create().show(); // 다이얼로그실행


            return "fail";
        }

        if (!hasEthereumWallet()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(R.string.app_message_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });

            builder.setMessage(R.string.app_message_00055);
            builder.setCancelable(false); // 뒤로가기 버튼 차단
            builder.create().show(); // 다이얼로그실행


            return "fail";
        }

        String address = getEthereumWallet();


        if (erc20db.has(contract, address)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setPositiveButton(R.string.app_message_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });

            builder.setMessage(R.string.app_message_00056);
            builder.setCancelable(false); // 뒤로가기 버튼 차단
            builder.create().show(); // 다이얼로그실행
            return "fail";
        }


        JSONObject api_param = new JSONObject();
        api_param.put("contract", contract);
        api_param.put("cointype", "8000003C"); // EThereum Only;

        Log.i("PYH", "addERC20 api_param : " + api_param);
        RestResult rr = call_REST2(Define.URL_API_INFO, api_param, true);
        Log.i("PYH", "addERC20 call_REST reeturn rr : " + rr);
        Log.i("PYH", "addERC20 rr.m_strResult : " + rr.m_strResult);
        JSONObject rr_res = new JSONObject(rr.m_strResult);
        String result = rr_res.getString("result");
        String fullname = rr_res.getString("fullname");
        String shortname = rr_res.getString("shortname");
        Log.i("PYH", "rr_res : " + rr_res);
        if (result.toLowerCase().equals("ok")) {

            JSONObject inParam = new JSONObject();
            inParam.put(KaiserERC20DBProvider.JSON_CONTRACT, contract);
            inParam.put(KaiserERC20DBProvider.JSON_FULLNAME, fullname);
            inParam.put(KaiserERC20DBProvider.JSON_COINTYPE, "8000003C");
            inParam.put(KaiserERC20DBProvider.JSON_SHORTNAME, shortname);
            inParam.put(KaiserERC20DBProvider.JSON_ADDRESS, address);

            erc20db.insert(inParam);
            Log.i("PYH", "inParam : " + inParam);
            final String tokens = erc20db.toString();
            Log.i("PYH", "tokens : " + tokens);
            wb.post(new Runnable() {
                @Override
                public void run() {

                    wb.evaluateJavascript("removeContractAddress()", null);
                    wb.evaluateJavascript(String.format("update_erc20tokens('%s')", tokens), null);
                }
            });

        } else {
            Toast.makeText(MainActivity.this, R.string.app_message_00057, Toast.LENGTH_LONG).show();
        }


        return ret.toString();
    }



    private String appRequestProcess_command_saveClipboard(JSONObject json, JSONObject ret) throws JSONException
    {
        String clipboard_string = getConfigParam(Define.CONFIG_DEFAULT_WALLET);
        clipboard_string = addAddressPrefix(clipboard_string);
        setClipboard(getApplicationContext(), clipboard_string);

        return "ok";
    }

    private String appRequestProcess_command_openWeb(JSONObject json, JSONObject ret) throws JSONException
    {
        String url = json.getString("url");
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        return "ok";
    }

    private String appRequestProcess_command_createQRcode(JSONObject json, JSONObject ret) throws JSONException
    {
        String amount = json.getString("amount");
        JSONObject newJson = new JSONObject();

        newJson.put(Define.JSON_SIGN_AMOUNT, amount);
        String address = getConfigParam(Define.CONFIG_DEFAULT_WALLET);

        address = addAddressPrefix(address);

        newJson.put(Define.JSON_TO_ADDRESS, address);
        newJson.put(Define.JSON_COIN_TYPE, getConfigParam(Define.CONFIG_DEFAULT_COINTYPE));

        String strNewJson = newJson.toString();
        if(strNewJson != null)
        {
            final String base64Image = BitmptoBase64Png(strNewJson);
            final String prefix = "data:image/png;base64, ";
            wb.post(new Runnable() {
                @Override
                public void run() {
                    wb.evaluateJavascript(String.format("updateQRcode('%s')", prefix + base64Image), null);
                    Toast.makeText(MainActivity.this, R.string.app_message_00001, Toast.LENGTH_LONG).show();
                }
            });
        }

        return ret.toString();
    }

    private String appRequestProcess_command_chooseImage(JSONObject json, JSONObject ret) throws JSONException
    {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PermissionManager.REQUEST_GALLARY_CODE_PERMISSION);


            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant

            return "fail";
        }

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), Define.MODE_PICK_IAMGE);

        return ret.toString();
    }

    private String appRequestProcess_command_changePin2(JSONObject json, JSONObject ret) throws JSONException
    {
        final String newPin= json.getString("pin");

        if(!RegexTest("^[0-9]{6}$",m_reservPin) || !RegexTest("^[0-9]{6}$",newPin))
        {
            wb.post(new Runnable() {
                @Override
                public void run() {
                    wb.evaluateJavascript("gofail()",null);
                }
            });

            return "fail";
        }

        JSONObject params = new JSONObject();

        params.put(Define.JSON_NEW_PIN_NUMBER,newPin);
        params.put(Define.JSON_PIN_NUMEBR,m_reservPin);

        runNFCActivityJSON(NFCActivity.MODE_CHANGE_PIN,params );

        return ret.toString();
    }

    private String appRequestProcess_command_resetnewpin2_1(JSONObject json, JSONObject ret) throws JSONException
    {
        String wordlist = json.getString("wordlist");

        if(!RegexTest("^[a-zA-Z]+,[a-zA-Z]+,[a-zA-Z]+$",wordlist)) {
            wb.post(new Runnable() {
                @Override
                public void run() {
                    wb.evaluateJavascript("gofail()",null);
                }
            });

            return "fail";
        }

        String[] wordlists = wordlist.split(",");
        JSONArray words = new JSONArray();
        for(int wln = 0;wln < wordlists.length;wln++)
        {
            words.put(wordlists[wln]);
        }
        JSONObject inparam = new JSONObject();
        inparam.put("index",words);

        RestResult rr = call_REST(Define.URL_API_INDEX,inparam );

        JSONObject retJson = new JSONObject(rr.m_strResult);

        JSONArray arr = retJson.getJSONArray("index");
        byte[] index_byte = new byte[6];
        short index1 = (short)arr.getInt(0);
        short index2 = (short)arr.getInt(1);
        short index3 = (short)arr.getInt(2);

        if(index1 < 0 || index2 <0 || index3 < 0)
        {
            Toast.makeText(MainActivity.this, R.string.app_message_00008, Toast.LENGTH_LONG).show();
            return "fail";
        }

        return ret.toString();
    }
    private String appRequestProcess_command_resetnewpin(JSONObject json, JSONObject ret) throws JSONException
    {
        final String newPin = json.getString("pin");

        if(!RegexTest("^[0-9]{6}$",newPin))
        {
            wb.post(new Runnable() {
                @Override
                public void run() {
                    wb.evaluateJavascript("gofail()",null);
                }
            });

            return "fail";
        }

        JSONObject params = new JSONObject();
        params.put(Define.JSON_NEW_PIN_NUMBER,newPin);

        runNFCActivityJSON(NFCActivity.MODE_RESET_NEW_PIN,params );

        return ret.toString();
    }

    private String appRequestProcess_command_changePin(JSONObject json, JSONObject ret) throws JSONException
    {
        String pin = json.getString("pin");
        m_reservPin = pin;
        wb.post(new Runnable() {
                    @Override
                    public void run() {
                        wb.loadUrl(getAppUrl()+"changepin2");
                    }
                }
        );

        return ret.toString();
    }

    private String appRequestProcess_command_changeERCWallet(JSONObject json, JSONObject ret) throws JSONException
    {
        return appRequestProcess_command_changeWallet_and_changeERCWallet(json, ret);
    }

    private String appRequestProcess_command_changeWallet(JSONObject json, JSONObject ret) throws JSONException
    {
        return appRequestProcess_command_changeWallet_and_changeERCWallet(json, ret);
    }

    private String appRequestProcess_command_changeWallet_and_changeERCWallet(JSONObject json, JSONObject ret) throws JSONException
    {
        synchronized (m_cwState) {
            m_cwState.m_bGetbalance = false;
            m_cwState.m_bTx = false;
            m_cwState.m_ChanginAddress = "";
        }

        Message msg = showProgressHandler_1.obtainMessage();
        showProgressHandler_1.sendMessage(msg);

        String in_cointype = json.getString("cointype");
        String in_contract = "";

        if(json.has("contract"))
            in_contract = json.getString("contract");

            if(!m_bHasEthereumAddress){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                        startActivityForResult(intent, 0);
                    }
                });
                builder.setMessage(R.string.app_message_00009);
                builder.setCancelable(false); // 뒤로가기 버튼 차단
                builder.show(); // 다이얼로그실행
                return "failed";
            }
        }

        String strWalletInfo = null;
        try {
            strWalletInfo = m_JWalletInfo.getString(Define.JSON_LIST_WALLET_INFO);

            if(strWalletInfo != null)
            {

                JSONArray arr = new JSONArray(strWalletInfo);


                if(arr.length() > 0)
                {
                    int findindex =-1 ;
                    for (int i=0; i < arr.length(); i++) {
                        JSONObject j = arr.getJSONObject(i);
                        try {
                            String strPubKey = j.getString(Define.JSON_PUBLIC_KEY);
                            String cointype = j.getString(Define.JSON_COIN_TYPE);
                            String result = getAddressFromAPI(strPubKey, cointype,in_contract);
                            JSONObject jj = new JSONObject(result);
                            String wallet = jj.getString("fromAddress");

                            if (in_cointype.equals(cointype)) {
                                setConfigParam(Define.CONFIG_DEFAULT_COINTYPE, cointype);
                                setConfigParam(Define.CONFIG_DEFAULT_WALLET, wallet);
                                setConfigParam(Define.CONFIG_DEFAULT_PUBLICKEY, strPubKey);
                                setConfigParam(Define.CONFIG_DEFAULT_CONTRACT,in_contract);
                                double tmpexp= jj.getDouble("exponent");

                                setPowexponenet(tmpexp);

                                synchronized (m_cwState) {
                                    m_cwState.m_ChanginAddress = wallet;
                                }


                                break;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            checkEthereumAddress();
            ChangeWalletInfo();

        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String cointype = getConfigParam(Define.CONFIG_DEFAULT_COINTYPE);
        final String contract = getConfigParam(Define.CONFIG_DEFAULT_CONTRACT);
        final String lists = erc20db.toString();

        boolean isContract = false;

        if (!contract.equals("")){
            isContract = true;
        }

        }
        return ret.toString();

    }

    private String appRequestProcess_command_createwallet(JSONObject json, JSONObject ret) throws JSONException
    {
        String cointype = json.getString("cointype");
        cointype = cointype.replace("0x","");
        JSONObject params = new JSONObject();
        params.put(Define.JSON_COIN_TYPE,cointype);
        params.put(Define.CONFIG_PIN_NUMBER,m_PIN);
        runNFCActivityJSON(NFCActivity.MODE_PREPARE_CREATE_WALLET,params);

        //runNFCActivity(NFCActivity.MODE_REGIST, "device_nfc", m_PIN, "");

        return ret.toString();
    }
    private String appRequestProcess_command_goMain(JSONObject json, JSONObject ret) throws JSONException
    {
        wb.loadUrl(getAppUrl() + "main?imei=" + Utils.getIMEI(getApplicationContext()) + "&time=" + System.currentTimeMillis());

        return ret.toString();
    }
    private String appRequestProcess_command_request_rest(JSONObject json, JSONObject ret) throws JSONException
    {
        String url = json.getString("url");
        AppRequest_requestREST(url);

        return ret.toString();
    }
    private String appRequestProcess_command_getwalletinfo(JSONObject json, JSONObject ret) throws JSONException
    {
        //..
        return ret.toString();
    }

    private String appRequestProcess_command_reservpin(JSONObject json, JSONObject ret) throws JSONException
    {
        // 한번 저장 후 삭제 한다.
        m_reservPin = json.getString("pin");
        wb.post(new Runnable() {
            @Override
            public void run() {
                wb.loadUrl(getAppUrl()+"confirmpin");
            }
        });

        return ret.toString();
    }

    private String appRequestProcess_command_ordering(JSONObject json, JSONObject ret) throws JSONException
    {
        // 한번 저장 후 삭제 한다.
        m_OrderingNumbers = json.getString("ordering_numbers");
        wb.post(new Runnable() {
            @Override
            public void run() {
                wb.loadUrl(getAppUrl()+"passphrase");
            }
        });

        return ret.toString();
    }
    private String appRequestProcess_command_deleteWallet(JSONObject json, JSONObject ret) throws JSONException
    {
        String target = json.getString("target");
        JSONObject inparam = new JSONObject();
        inparam.put(Define.JSON_PIN_NUMEBR,m_PIN);
        inparam.put(Define.JSON_COIN_TYPE,target);
        runNFCActivityJSON(NFCActivity.MODE_DELETE_WALLET,inparam);

        return ret.toString();

    }
    private String appRequestProcess_command_createKey(JSONObject json, JSONObject ret) throws JSONException
    {
        String passPhrase = json.getString("passphrase");

        JSONObject json_params= new JSONObject();

        //String ordering = m_OrderingNumbers.tohex

        String HexOrdering = m_OrderingNumbers.replace(",","");
        HexOrdering = SimpleCrypto.toHex(HexOrdering);
        String hexpassPhrase = SimpleCrypto.toHex(passPhrase);

        json_params.put(Define.JSON_PIN_NUMEBR,getConfigParam(Define.JSON_PIN_NUMEBR));
        json_params.put(Define.JSON_ORDERING,HexOrdering);
        json_params.put(Define.JSON_PASSPHRASE,hexpassPhrase);

        Toast.makeText(MainActivity.this, R.string.app_message_00058, Toast.LENGTH_LONG).show();
        runNFCActivityJSON(NFCActivity.MODE_CREATE_KEY ,json_params);

        return ret.toString();
    }

    private String appRequestProcess_command_runcreateKey(JSONObject json, JSONObject ret) throws JSONException
    {
        m_iCreateKeyMode = 1;
        runCreateKeyProcess(null);

        return ret.toString();
    }

    private String appRequestProcess_command_setNewKey(JSONObject json, JSONObject ret) throws JSONException
    {
        JSONObject json_params = new JSONObject();

        String HexOrdering = m_OrderingNumbers.replace(",","");
        HexOrdering = SimpleCrypto.toHex(HexOrdering);
        String hexpassPhrase = SimpleCrypto.toHex("");

        json_params.put(Define.JSON_PIN_NUMEBR,getConfigParam(Define.JSON_PIN_NUMEBR));
        json_params.put(Define.JSON_ORDERING,HexOrdering);
        json_params.put(Define.JSON_PASSPHRASE,hexpassPhrase);

        runNFCActivityJSON(NFCActivity.MODE_COMPELTE_CREATE_KEY, json_params);

        return ret.toString();
    }




    final ThreadSyncObject balanaceObj = new ThreadSyncObject();
    final ThreadSyncObject m_txseObj = new ThreadSyncObject();

    private Thread txsHistory = new Thread(
            new Runnable() {

                @Override
                public void run() {
                    String balance = "";
                    boolean isrunning = true;




                    while(isrunning)
                    {
                        String strfromAddress = null;
                        String strcointype = "";
                        String Publickey = "";
                        String contract = "";
                        synchronized (m_txseObj)
                        {
                            isrunning = m_txseObj.isruning;
                            strfromAddress = (String)m_txseObj.obj;
                            strcointype = (String)m_txseObj.ct;
                            Publickey = (String)m_txseObj.pk;
                            contract = (String)m_txseObj.contract;

                        }
                        if(strfromAddress != null && !strfromAddress.isEmpty()) {
                            HttpURLConnection connection = null;
                            try {
                                URL url = new URL(Define.);
                                connection = (HttpURLConnection) url.openConnection();
//                                Log.d("call_REST","created");
                                connection.setRequestMethod("PUT");
                                connection.setDoOutput(true);
                                connection.setDoInput(true);
                                connection.setRequestProperty("Content-Type", "application/json ;charset=UTF-8");
                                connection.setRequestProperty("Accept", "application/json");
                                OutputStream osw = connection.getOutputStream();

                                /*
                                 * agcraft
                                 * put 방식으로 REST를 구성해서 보낸다.
                                 * */

                                JSONObject obj = new JSONObject();
                                obj.put("strMyAddress", strfromAddress);
                                obj.put("publickey",Publickey);
                                obj.put("cointype", strcointype);
                                obj.put("contract", contract);

                                osw.write(obj.toString().getBytes("UTF-8"));
                                osw.flush();
                                osw.close();
                                String inputLine = "";

                                Log.d("TXS",obj.toString());

                                int responsecode = connection.getResponseCode();
                                if (responsecode == 200) {

                                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                                    StringBuffer content = new StringBuffer();
                                    while ((inputLine = br.readLine()) != null) {
                                        content.append(inputLine);
                                    }

                                    br.close();
                                    inputLine = content.toString();
                                }

//                                Log.d("call_REST","disconnected");
                                JSONObject json = new JSONObject(inputLine);
                                if(json.has("data")){
                                    balance = json.getString("data");
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (ProtocolException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            finally {
                                if(connection != null)
                                    connection.disconnect();
                            }

                            final String strList = balance;
                            String strMnemonic = "???";


                            Log.d("TXS","kdb");

                            synchronized (kdb)
                            {
                                try {

                                    JSONArray json_arr = new JSONArray(balance);

                                    for (int i = 0; i < json_arr.length(); i++) {
                                        JSONObject json = json_arr.getJSONObject(i);

                                        double amount = json.getDouble("amount");
                                        String value = BigDecimal.valueOf(amount).toPlainString();
                                        json.put("amount", value);


                                        double fee = json.getDouble("fee");
                                        String value2 = BigDecimal.valueOf(fee).toPlainString();
                                        json.put("fee", value2);


                                        kdb.push(json);
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            String tmphistory = "";


                            synchronized (m_cwState)
                            {
                                if(m_cwState.m_ChanginAddress.equals(strfromAddress))
                                {
                                    m_cwState.m_bTx = true;
                                    Log.d("TXS","m_bTx");
                                }
                            }

                        else
                        {
                            m_getbalanceHandler.post(new Runnable() {
                                @Override
                                public void run() {
                            if(wb.getUrl().contains("main")) {
                                String params = String.format("update_txs('%s','%s')","[]","???");
                                wb.evaluateJavascript(params, null);
                            }
                                }
                            });
                        }


                                synchronized (m_cwState){
                                    m_cwState.m_bTx = true;
                                    if(m_cwState.m_bGetbalance && m_cwState.m_bTx)
                                    {
                                        Log.d("TXS","m_bTx true");
                                        Message message = showProgressHandler_2.obtainMessage();
                                        showProgressHandler_2.sendMessage(message);
                                        m_cwState.init();
                                    }
                                }
                        }
                    }
                }
            }
    );




    public void setExchange(){
        if (setExchangeThread == null){
            setExchangeThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (true){
                        try {
                            Thread.sleep(3*1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        String strBal ="";
                        String strCoinType = "";
                        String strContract = "";
                        synchronized (balanaceObj)
                        {
                            strBal = (String)balanaceObj.bl;
                            strCoinType = (String)balanaceObj.ct;
                            strContract = (String)balanaceObj.contract;

                        }

                        if (!strBal.equals("") && (strCoinType.length() > 0)){
                            double bbal = TestClass.parseDouble(strBal,"7");
                            boolean isContract = false;
                            if (!strContract.equals("")){
                                isContract = true;
                            }


                            final String strValue =  getExchangeRate(strCoinType, strMoneyType, Cibalance, isContract);

                            final String strExchangeList = getStrExchangedList(strCoinType);
                            final String strExchangeInfo = getExchangListInfo(strCoinType,strMoneyType);

                            wb.post(new Runnable() {
                                @Override
                                public void run() {
                                    wb.evaluateJavascript((String.format("update_exchange('%s','%s')", strValue, strMoneyType)), null);
                                    wb.evaluateJavascript(String.format("update_exchange_list('%s','%s')", strExchangeList, strExchangeInfo), null);

                                }
                            });
                        }
                    }
                }
            });
            setExchangeThread.start();

        }
    }

    public void getBalanceThread()
    {
        if(getBalanceThread == null)
        {

            getBalanceThread = new Thread(
                    new Runnable() {

                        @Override
                        public void run() {
                            double balance = 0;
                            String nm = "";
                            String ct = "";
                            String cont = "";
                            boolean isrunning = true;
                            while(isrunning)
                            {
                                String strfromAddress = null;
                                String strcointype = "";
                                String additinal = "";
                                String contract = "";
                                synchronized (balanaceObj)
                                {
                                    isrunning = balanaceObj.isruning;
                                    strfromAddress = (String)balanaceObj.obj;
                                    strcointype = (String)balanaceObj.ct;
                                    additinal = (String)balanaceObj.addtional;
                                    contract = (String)balanaceObj.contract;

                                }
                                if(strfromAddress != null && !strfromAddress.isEmpty()) {
                                    HttpURLConnection connection = null;
                                    try {
                                        //   Log.d("call_REST",String.format("Thread id %l",Thread.currentThread().getId()));
                                        JSONObject obj = new JSONObject();
                                        obj.put("coinAddress", strfromAddress);
                                        obj.put("cointype", strcointype);
                                        obj.put("contract",contract);
                                        obj.put("additinal",additinal );

                                        Log.d("update_balance","obj : "+obj);

                                        URL url = new URL(Define.URL_API_GETBALANCE);
                                        connection = (HttpURLConnection) url.openConnection();

                                        connection.setRequestMethod("PUT");
                                        connection.setDoOutput(true);
                                        connection.setDoInput(true);
                                        connection.setRequestProperty("Content-Type", "application/json ;charset=UTF-8");
                                        connection.setRequestProperty("Accept", "application/json");
//                                        connection.setRequestProperty("Content-Length",String.format("%d",obj.toString().getBytes("UTF-8").length));


                                        OutputStream osw = connection.getOutputStream();

                                        /*
                                         * agcraft
                                         * put 방식으로 REST를 구성해서 보낸다.
                                         * */


                                        osw.write(obj.toString().getBytes("UTF-8"));
                                        osw.flush();
                                        osw.close();
                                        String inputLine = "";
                                        int responsecode = connection.getResponseCode();
                                        if (responsecode == 200) {

                                            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                                            StringBuffer content = new StringBuffer();
                                            while ((inputLine = br.readLine()) != null) {
                                                content.append(inputLine);
                                            }

                                            br.close();
                                            inputLine = content.toString();
                                        }

                                        Log.d("call_REST","disconnected");

                                        JSONObject json = new JSONObject(inputLine);
                                        Log.d("update_balance","json : "+json);

                                        if(json.isNull("balance"))
                                            balance = 0.0;
                                        else
                                            balance = json.getDouble("balance");

                                        nm = json.getString("nm");
                                        ct = json.getString("cointype");
                                        cont = json.getString("contract");


                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } catch (ProtocolException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    finally {
                                        if(connection != null)
                                            connection.disconnect();
                                    }

                                    String strMnemonic = "???";
                                    /*double pow_number = -18;
                                    if(getConfigParam(Define.CONFIG_DEFAULT_COINTYPE).equals("8000003C"))
                                    {
                                        strMnemonic = "ETH";
                                    }
                                    else if(getConfigParam(Define.CONFIG_DEFAULT_COINTYPE).equals("80000000"))
                                    {
                                        strMnemonic = "BTC";
                                        pow_number = -8;
                                    }*/

                                    synchronized (m_cwState)
                                    {
                                        if(m_cwState.m_ChanginAddress.equals(strfromAddress))
                                        {
                                            m_cwState.m_bGetbalance= true;
                                            Log.d("TXS","m_balance");
                                        }
                                    }

                                    final String Mnemonic = nm;
                                    final String strCoinType = ct;
                                    final String strContract = cont;

                                    //final double updatebalance = balance*Math.pow(10,pow_number);
                                    final double updatebalance = balance;

                                    m_getbalanceHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(wb.getUrl().contains("main") || wb.getUrl().contains("transmit")) {
                                                String inbalanace = BigDecimal.valueOf(updatebalance).toPlainString();
                                                if(inbalanace.length() > 10)
                                                {
                                                    inbalanace = inbalanace.substring(0,10);
                                                }


                                                inbalanace = decimalFormatMoney(updatebalance);


                                                String params = String.format("update_balance('%s','%s');",inbalanace,Mnemonic );
                                                Log.i("update_balance","coin type : "+Mnemonic);
                                                wb.evaluateJavascript(params, null);

                                                params = String.format("update_cointype('%s');",strCoinType);
                                                wb.evaluateJavascript(params, null);

                                                synchronized (m_cwState){
                                                    if(m_cwState.m_bGetbalance && m_cwState.m_bTx)
                                                    {
                                                        Log.d("TXS","m_balance true");
                                                        Message message = showProgressHandler_2.obtainMessage();
                                                        showProgressHandler_2.sendMessage(message);
                                                        m_cwState.init();
                                                    }
                                                }
                                                // 환전


                                            }
                                        }
                                    });

                                    synchronized (balanaceObj)
                                    {
                                        balanaceObj.bl = String.valueOf(updatebalance);
                                        if(balanaceObj.bl!=null)
                                        {
                                            Cibalance = TestClass.parseDouble(balanaceObj.bl.trim(),"8");
                                        }
                                        else
                                        {
                                            Cibalance = 0;
                                        }
                                    }


                                }
                                else
                                {
                                    m_getbalanceHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(wb.getUrl().contains("main")) {


                                                String params = String.format("update_balance('%s','%s');","0.0","???");
                                                wb.evaluateJavascript(params, null);

                                                params = String.format("update_cointype('%s');","");
                                                wb.evaluateJavascript(params, null);

                                            }
                                        }
                                    });
                                }

                                try {
                                    Thread.sleep(3000);
                                }catch (InterruptedException e){
                                    e.printStackTrace();
                                    synchronized (m_cwState){
                                        if(m_cwState.m_bGetbalance && m_cwState.m_bTx)
                                        {
                                            Log.d("TXS","m_balance true");
                                            Message message = showProgressHandler_2.obtainMessage();
                                            showProgressHandler_2.sendMessage(message);
                                            m_cwState.init();
                                        }
                                    }
                                }
                            }
                        }
                    }

            );

            getBalanceThread.start();
        }

    }

    double Cibalance =0.0;


    private void AppRequest_requestREST(String url)
    {

        requestREST(url);
    }
    private String decimalFormatMoney(double money){

        DecimalFormat dFormat = new DecimalFormat("###,###,###,###,###,###,###,###.########################");//콤마
        String result_int = dFormat.format(money);

        return result_int;
    }




    public void onNewIntent(Intent passedIntent) {
        Log.d(TAG, "onNewIntent() called.");

        m_Activity = this;

        // 완벽하게 로그인 하기 전까지는 인덱스 화면으로 가야한다.
        if(isLoginFrag){
            wb.loadUrl(getAppUrl() + "main?imei=" + Utils.getSetttingInfo(getApplicationContext(), "imei"));
        }else{
//            wb.loadUrl(getAppUrl() + "index");
            isLoginFrag = false;
        }



    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_AUTH_UI_CUSTOM_TRANSPARENCY:
                startIdentifyDialog(false);
                break;
        }
        return true;
    }

    private void startIdentifyDialog(boolean backup) {
        try {
            if (mSpassFingerprint != null) {
                mSpassFingerprint.setDialogTitle(getResources().getString(R.string.app_message_00011), 0x000000);
                mSpassFingerprint.setDialogBgTransparency(0);
                mSpassFingerprint.startIdentifyWithDialog(this, mIdentifyListenerDialog, backup);
            }
        } catch (IllegalStateException e) {
            Log.d(TAG, "Exception: " + e);
        }

    }

    private static String getEventStatusName(int eventStatus) {
        switch (eventStatus) {
            case SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS:
                return "STATUS_AUTHENTIFICATION_SUCCESS";
            case SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS:
                return "STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS";
            case SpassFingerprint.STATUS_TIMEOUT_FAILED:
                return "STATUS_TIMEOUT";
            case SpassFingerprint.STATUS_SENSOR_FAILED:
                return "STATUS_SENSOR_ERROR";
            case SpassFingerprint.STATUS_USER_CANCELLED:
                return "STATUS_USER_CANCELLED";
            case SpassFingerprint.STATUS_QUALITY_FAILED:
                return "STATUS_QUALITY_FAILED";
            case SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE:
                return "STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE";
            case SpassFingerprint.STATUS_BUTTON_PRESSED:
                return "STATUS_BUTTON_PRESSED";
            case SpassFingerprint.STATUS_OPERATION_DENIED:
                return "STATUS_OPERATION_DENIED";
            case SpassFingerprint.STATUS_AUTHENTIFICATION_FAILED:
            default:
                return "STATUS_AUTHENTIFICATION_FAILED";
        }
    }


    private SpassFingerprint.IdentifyListener mIdentifyListenerDialog = new SpassFingerprint.IdentifyListener() {
        @Override
        public void onFinished(int eventStatus) {
            Log.d(TAG, "identify finished : reason =" + getEventStatusName(eventStatus));
            int FingerprintIndex = 0;
            boolean isFailedIdentify = false;

            try {
                FingerprintIndex = mSpassFingerprint.getIdentifiedFingerprintIndex();
            } catch (IllegalStateException ise) {
                Log.d(TAG, ise.getMessage());
            }
            if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS) {
                Log.d(TAG, "onFinished() : Identify authentification Success with FingerprintIndex : " + FingerprintIndex);

                m_bFinishFinger = true;
                Log.d("","");
                checkAppEnvironmentafterRunNFC();

            } else if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS) {
                Log.d(TAG, "onFinished() : Password authentification Success");
            } else if (eventStatus == SpassFingerprint.STATUS_USER_CANCELLED
                    || eventStatus == SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE) {
                Log.d(TAG, "onFinished() : User cancel this identify.");

                Toast.makeText(mContext, R.string.app_message_00013, android.widget.Toast.LENGTH_LONG).show();
                terminateApp();

            } else if (eventStatus == SpassFingerprint.STATUS_TIMEOUT_FAILED) {
                Log.d(TAG, "onFinished() : The time for identify is finished.");
                Toast.makeText(mContext, R.string.app_message_00012, android.widget.Toast.LENGTH_LONG).show();
                terminateApp();
            } else if (!mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_AVAILABLE_PASSWORD)) {
                if (eventStatus == SpassFingerprint.STATUS_BUTTON_PRESSED) {
                    Log.d(TAG, "onFinished() : User pressed the own button");
                    Toast.makeText(mContext, "Please connect own Backup Menu", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d(TAG, "onFinished() : Authentification Fail for identify");
                isFailedIdentify = true;
            }

        }

        @Override
        public void onReady() {
            Log.d(TAG, "identify state is ready");
        }

        @Override
        public void onStarted() {
            Log.d(TAG, "User touched fingerprint sensor");
        }

        @Override
        public void onCompleted() {
            Log.d(TAG, "the identify is completed");
        }
    };


    public void clearWalletInfo()
    {
        setConfigParam(Define.CONFIG_DEFAULT_COINTYPE,"");
        setConfigParam(Define.CONFIG_DEFAULT_PUBLICKEY,"");
        setConfigParam(Define.CONFIG_DEFAULT_WALLET,"");
    }

    public void terminateApp()
    {

        m_Activity.finish();
        finishAffinity();
        System.exit(0);
    }

    private void terminateApp(String msg)
    {
        Log.d("ERROR", msg);
        Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
        //getActivity().finish();
        //finishAffinity();
        //System.exit(0);
    }

    public void process_result_fail(int nMode,int iSW)
    {
        if(iSW == 0)
        {
            return;
        }
        else if(Define.SW_APP_ERROR_LIFECYCLE == iSW)
        {
            // 라이프 사이클이 사용중이 아니므로 프로그램을 종료 한다.
            String msg = String.format(getResources().getString(R.string.app_message_00014)+": %04X", iSW);
            AlertUtil.showSimpleOk(this, msg);
        }
        else if(Define.SW_APP_ERROR_VERIFY_PIN == iSW) {
            // 핀 인증이 필요 하므로 핀 인증을 진행 한다.
        }
        else if(Define.SW_APP_ERROR_GEN_MULTI_SIGN == iSW) {
            AlertUtil.showSimpleOk(this, R.string.app_message_00015, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    wb.reload();
                }
            });
        }
        else if(Define.SW_APP_ERROR_NEED_SEED == iSW) {
            AlertUtil.showSimpleOk(this, R.string.app_message_00016, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    wb.reload();
                }
            });
        }
        else if(0x63c0 < iSW && 0x63c4 >= iSW ) {
            if(nMode == NFCActivity.MODE_RESET_NEW_PIN) {
                AlertUtil.showSimpleOk(this, String.format(getResources().getString(R.string.app_message_00018), (iSW - 0x63c0)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        wb.reload();
                    }
                });
            }
            else if(nMode == NFCActivity.MODE_COMPELETE_RESTORE_KEY) {
                AlertUtil.showSimpleOk(this, String.format(getResources().getString(R.string.app_message_00018), (iSW - 0x63c0)), null);
            }
            else if(nMode == NFCActivity.MODE_CHANGE_PIN) {
                AlertUtil.showSimpleOk(this, String.format(getResources().getString(R.string.app_message_00017), (iSW - 0x63c0)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        wb.evaluateJavascript("location.href='/changepin'",null);
                    }
                });
            }
            else {
                AlertUtil.showSimpleOk(this, String.format(getResources().getString(R.string.app_message_00017), (iSW - 0x63c0)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        wb.reload();
                    }
                });
            }

        }
        else if(Define.SW_FILE_INVALID == iSW || 0x63c0  == iSW) {
            if(nMode == NFCActivity.MODE_VERIFY_PIN) {
                String msg = String.format(getResources().getString(R.string.app_message_00019)+": %04X", iSW);
                AlertUtil.showSimpleOk(this, msg);
            }
            else if(nMode == NFCActivity.MODE_RESET_NEW_PIN) {
                String msg = String.format(getResources().getString(R.string.app_message_00020)+": %04X", iSW);
                AlertUtil.showSimpleOk(this, msg);
            }
        }
        else if(Define.SW_FILE_NOT_FOUND == iSW) {
            AlertUtil.showSimpleOk(this, R.string.app_message_00021, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    wb.reload();
                }
            });
        }
        else if(Define.SW_APP_ERROR_UNKNOWN_APPLET == iSW) {
            // 애플릿이 없다.
            AlertUtil.showSimpleOk(this, R.string.app_message_00022, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    wb.reload();
                }
            }); 
        }
        else if(Define.SW_TAG_WAS_LOST == iSW) {
            AlertUtil.showSimpleOk(this, R.string.app_message_00023, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    wb.reload();
                }
            });
        }
        else{
            Toast.makeText(this, String.format(getResources().getString(R.string.app_message_00022)+" : %4d", iSW), android.widget.Toast.LENGTH_LONG).show();
        }

    }


    public String getAddressFromAPI(String strPubKey,String cointype, String contract)
    {
        JSONObject obj = new JSONObject();
        try {
            obj.put("strPubkey", strPubKey);
            obj.put("cointype", cointype);
            obj.put("contract", contract);
        }catch (JSONException e)
        {
            return "";
        }

        RestResult rr = call_REST2(Define.URL_API_GETADDRESS,obj,false);
        // 태그 후 결과를 받는다.
        return rr.m_strResult;
    }


    public byte[] getSHA256(String src)
    {

        try{
            MessageDigest sh = MessageDigest.getInstance("SHA-256");

            sh.update(src.getBytes());

            return sh.digest();


        }catch(NoSuchAlgorithmException e){

            e.printStackTrace();

        }

        return null;

    }

    public static void Swap(byte[] a, int aOffset, byte[] b, int bOffset, byte[] temp, int tOffset, int sLen)
    {
        System.arraycopy(a, aOffset, temp, tOffset, sLen);
        System.arraycopy(b, bOffset, a, aOffset, sLen);
        System.arraycopy(temp, tOffset, b, bOffset, sLen);
    }

    public void overwiteDefaultWalletInfo(String cointype,String publickey,String wallet)
    {
        setConfigParam(Define.CONFIG_DEFAULT_COINTYPE,cointype);
        setConfigParam(Define.CONFIG_DEFAULT_PUBLICKEY,publickey);
        setConfigParam(Define.CONFIG_DEFAULT_WALLET,wallet);
    }

    public void overwiteDefaultWalletInfo(String cointype,String publickey,String wallet,double arg_exponent)
    {
        overwiteDefaultWalletInfo(cointype,publickey,wallet);
        setPowexponenet(arg_exponent);
    }

    public void updateDefaultWallet(String cointype,String publickey,String wallet,double arg_exponent) {
        String defaultcointype = getConfigParam(Define.CONFIG_DEFAULT_COINTYPE);
        if (defaultcointype == null || defaultcointype.isEmpty()) {
            setConfigParam(Define.CONFIG_DEFAULT_COINTYPE, cointype);
            setConfigParam(Define.CONFIG_DEFAULT_PUBLICKEY, publickey);
            setConfigParam(Define.CONFIG_DEFAULT_WALLET, wallet);

        }

        setPowexponenet(arg_exponent);

    }

    public void runCreateKeyProcess(JSONObject json)
    {
        /**
         * 순서 변경 정보등의
         * 입력 절차를 빼고 진행 한다.
         *
         */

        JSONObject json_params= new JSONObject();


        String HexOrdering = SimpleCrypto.toHex("000000");

        String hexpassPhrase = SimpleCrypto.toHex("");

        try {
            json_params.put(Define.JSON_PIN_NUMEBR,getConfigParam(Define.JSON_PIN_NUMEBR));
            json_params.put(Define.JSON_ORDERING,HexOrdering);
            json_params.put(Define.JSON_PASSPHRASE,hexpassPhrase);

            Toast.makeText(MainActivity.this, R.string.app_message_00024, Toast.LENGTH_LONG).show();
            runNFCActivityJSON(NFCActivity.MODE_CREATE_KEY ,json_params);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public String getImageFilePath(Uri uri) {

        File file = new File(uri.getPath());
        String[] filePath = file.getPath().split(":");
        String image_id = filePath[filePath.length - 1];

        Cursor cursor = getContentResolver().query(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, MediaStore.Images.Media._ID + " = ? ", new String[]{image_id}, null);
        if (cursor!=null) {
            cursor.moveToFirst();
            String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            cursor.close();
            return imagePath;
        }

        return "";
    }

    @Override
    public void onActivityResult(final int requestCode, int resultCode, final Intent data) {

        Log.i("forDoc","onActivityResult requestCode : "+requestCode);
        Log.i("forDoc","onActivityResult resultCode : "+resultCode);
        Log.i("forDoc","onActivityResult data : "+data);

       super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {
            case Define.MODE_PICK_IAMGE:
                onActivityResult_Define_MODE_PICK_IMAGE(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_WALLETINFO:
                onActivityResult_NFCActivity_MODE_WALLETINFO(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_PREPARE_CREATE_WALLET:
                onActivityResult_NFCActivity_MODE_PREPARE_CREATE_WALLET(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_COMPELTE_CREATE_WALLET:
                onActivityResult_NFCActivity_MODE_COMPELETE_CREATE_WALLET(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_SET_NEW_KEY:
                onActivityResult_NFCActivity_MODE_SET_NEW_KEY(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_COMPELTE_CREATE_KEY:
                onActivityResult_NFCActivity_MODE_COMPELETE_CREATE_KEY(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_PREPARE_RESTORE_KEY:
                onActivityResult_NFCActivity_MODE_PREPARE_RESTORE_KEY(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_COMPELETE_RESTORE_KEY:
                onActivityResult_NFCActivity_MODE_COMPELETE_RESTORE_KEY(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_CREATE_KEY:
                onActivityResult_NFCActivity_MODE_CREATE_KEY(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_DELETE_WALLET:
                onActivityResult_NFCActivity_MODE_DELETE_WALLET(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_VERIFY_PIN:
                onActivityResult_NFCActivity_MODE_VERIFY_PIN(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_RESET_NEW_PIN:
                onActivityResult_NFCActivity_MODE_RESET_NEW_PIN(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_RESET_PIN:
                onActivityResult_NFCActivity_MODE_RESET_PIN(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_CHANGE_PIN:
                onActivityResult_NFCActivity_MODE_CHANGE_PIN(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_SET_NEWPIN:
                onActivityResult_NFCActivity_MODE_SET_NEWPIN(requestCode, resultCode, data);
                break;
            case QrCodeReadActivity.REQEUST_CODE_SCAN_QR_CODE:
                onActivityResult_QrCodeReadActivity_REQUEST_CODE_SCAN_OR_CODE(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_REGIST:
                onActivityResult_NFCActivity_MODE_REGIST(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_SIGN:
                onActivityResult_NFCActivity_MODE_SIGN(requestCode, resultCode, data);
                break;
            case NFCActivity.MODE_AUTH:
                onActivityResult_NFCActivity_MODE_AUTH(requestCode, resultCode, data);
                break;
            case Define.MODE_PICK_INPUT_FILE_IMAGE:
                onActivityResult_Define_MODE_PICK_INPUT_FILE_IMAGE(requestCode,resultCode, data);
                break;
            case NFCActivity.MODE_INIT_ZERO_PIN:
                onActivityResult_NFCActivity_MODE_INIT_ZERO_PIN(requestCode, resultCode, data);
                break;
        }
    }

    private void onActivityResult_Define_MODE_PICK_IMAGE(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        try {
            if (resultCode == RESULT_OK && null != data) {

                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount(); //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        String filepath = getImageFilePath(imageUri);
                        final String qrcode = getQrCode(filepath);
                        if(qrcode != null || !qrcode.isEmpty())
                        {

                            try{
                                JSONObject json = new JSONObject(qrcode);

                                String cointype = json.getString(Define.JSON_COIN_TYPE);
                                final String amount = json.getString(Define.JSON_SIGN_AMOUNT);
                                final String address1 = json.getString(Define.JSON_TO_ADDRESS);

                                wb.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        wb.evaluateJavascript(String.format("setSender('%s')", address1), null);
                                        wb.evaluateJavascript(String.format("setAmount('%s')", amount), null);
                                    }
                                });


                            }
                            catch(JSONException e)
                            {
                                wb.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        wb.evaluateJavascript(String.format("setSender('%s')",qrcode),null);
                                    }
                                });
                            }
                        }
                    }
                    //do something with the image (save it to some directory or whatever you need to do with it here)
                } else if (data.getData() != null) {

                    Uri imgUrl = data.getData();
                    String filepath = getImageFilePath(imgUrl);
                    final String qrcode = getQrCode(filepath);
                    if(qrcode != null || !qrcode.isEmpty())
                    {
                        try{
                            JSONObject json = new JSONObject(qrcode);

                            String cointype = json.getString(Define.JSON_COIN_TYPE);
                            final String amount = json.getString(Define.JSON_SIGN_AMOUNT);
                            final String address1 = json.getString(Define.JSON_TO_ADDRESS);

                            wb.post(new Runnable() {
                                @Override
                                public void run() {
                                    wb.evaluateJavascript(String.format("setSender('%s')", address1), null);
                                    wb.evaluateJavascript(String.format("setAmount('%s')", amount), null);
                                }
                            });


                        }catch(JSONException e)
                        {
                            wb.post(new Runnable() {
                                @Override
                                public void run() {
                                    wb.evaluateJavascript(String.format("setSender('%s')",qrcode),null);
                                }
                            });
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.app_message_00025,
                            Toast.LENGTH_LONG).show();
                }
            }

        } catch (Exception e) {
            Toast.makeText(this, R.string.app_message_00026, Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void onActivityResult_NFCActivity_MODE_WALLETINFO(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);

                    /*String strLog = String.format("지갑 정보를 가져오는데 실패했습니다. 오류코드 : %04X", iSW);
                    Log.d("ERROR", strLog);
                    Toast.makeText(this, strLog, android.widget.Toast.LENGTH_LONG).show();

                    wb.loadUrl(APP_URL+"main?imei="+Utils.getSetttingInfo(getApplicationContext(), "imei"));*/
            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            String strJson = data.getStringExtra(Define.INTENT_WALLENT_INFO);
            try {
                m_JWalletInfo = new JSONObject(strJson);
                Log.d("WALLET_INFO", m_JWalletInfo.toString());
                m_strCSN = m_JWalletInfo.getString(Define.JSON_CSN);
                m_strTSN = m_JWalletInfo.getString(Define.JSON_TSN);
                boolean isdiff = false;
                if (!m_strTSN.equals(getConfigParam(Define.JSON_TSN))) {

                    setConfigParam(Define.JSON_TSN, m_strTSN);
                    overwiteDefaultWalletInfo("", "", "");
                    setConfigParam(Define.CONFIG_LIST_WALLET_INFO, "[]");
                    ChangeWalletInfo();
                    isdiff = true;
                }
                if (m_JWalletInfo.has(Define.JSON_LIST_WALLET_INFO)) {
                    String strWalletInfo = m_JWalletInfo.getString(Define.JSON_LIST_WALLET_INFO);
                    Log.d("WALLET_INFO", "checn "+strWalletInfo);
                    if (strWalletInfo != null) {
                        boolean isLoad = false;
                        String lastCointype = "";
                        String lastPublickey = "";
                        String lastWallet = "";
                        double lastexponent = 0;
                        m_CoinList = new JSONArray();
                        setConfigParam(Define.CONFIG_LIST_WALLET_INFO, strWalletInfo);
                        JSONArray arr = new JSONArray(strWalletInfo);
                        m_JappWalletInfo = m_JWalletInfo;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject j = arr.getJSONObject(i);
                            try {
                                String strPubKey = j.getString(Define.JSON_PUBLIC_KEY);
                                String cointype = j.getString(Define.JSON_COIN_TYPE);
                                String result = getAddressFromAPI(strPubKey, cointype,"");
                                JSONObject jj = new JSONObject(result);
                                String wallet = jj.getString("fromAddress");
                                double exp = jj.getDouble("exponent");

                                JSONObject obj = new JSONObject();
                                obj.put(Define.CONFIG_DEFAULT_COINTYPE, cointype);
                                obj.put(Define.CONFIG_DEFAULT_WALLET, wallet);


                                lastCointype = cointype;
                                lastPublickey = strPubKey;
                                lastWallet = wallet;
                                lastexponent = exp;
                                m_CoinList.put(obj);
                                isLoad = true;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        if (isdiff) {
                            if (isLoad) {
                                overwiteDefaultWalletInfo(lastCointype, lastPublickey, lastWallet,lastexponent);
                            } else {
                                overwiteDefaultWalletInfo("", "", "",0);
                            }
                        } else {
                            if (isLoad) {
                                updateDefaultWallet(lastCointype, lastPublickey, lastWallet,lastexponent);
                            }
                        }
                    }

                    // 새로운 api 함수로 값 보내주기
                    // tsn imei firebase token

                    String token = FirebaseInstanceId.getInstance().getToken();
                    String imei = Utils.getIMEI(getApplicationContext());

                    JSONObject infoObj = new JSONObject();
                    infoObj.put("tsn", m_strTSN);
                    infoObj.put("token", token);
                    infoObj.put("imei", imei);

                    RestResult result = call_REST(URL_API_UPDATENOTIFYINFO,infoObj);


                } else {
                    m_JWalletInfo.put(Define.JSON_LIST_WALLET_INFO, new JSONArray());
                }
                m_iCreateKeyMode = 0;
                checkEthereumAddress();
                ChangeWalletInfo();
                /**/

                if (!m_JWalletInfo.getString(Define.JSON_PIN).equals(Define.CONFIG_TRUE_CODE)) {
                    // 핀이 없을댄, 핀 등록페이지로 이동하지 않고, 핀 확인 페이지로 이동해서 등록도 같이 진행하도록 한다.
                    Log.i("forDoc","onActivityResult MODE_WALLETINFO setpin : ");

                    showAlertInitZeroPinGuide();
                    return;
                    // 이후 confirmpin페이지로 이동하며 종국엔 createPin이 AppRequest가 호출 된다.
                } else if (m_JWalletInfo.getString(Define.JSON_PIN).equals(Define.CONFIG_TRUE_CODE) &&
                        (!getConfigParam(Define.CONFIG_SET_PIN).equals(Define.CONFIG_TRUE_CODE) ||
                                !getConfigParam(Define.CONFIG_DISABLE_PIN).equals(Define.CONFIG_TRUE_CODE))) {
                    Log.i("forDoc","onActivityResult MODE_WALLETINFO verifypin : ");
                    wb.loadUrl(getAppUrl() + "verifyPin");
                    return;
                } else if (!m_JWalletInfo.getString(Define.JSON_SEED).equals(Define.CONFIG_TRUE_CODE)) {
                    /**
                     * 순서 변경 정보등의
                     * 입력 절차를 빼고 진행 한다.
                     *
                     */
                    Log.i("forDoc","onActivityResult MODE_WALLETINFO JSON_SEED 0  ");
                    m_iCreateKeyMode = 0;
                    runCreateKeyProcess(null);

                            /*JSONObject json_params= new JSONObject();

                            //String ordering = m_OrderingNumbers.tohex

                            String HexOrdering = SimpleCrypto.toHex("000000");

                            String hexpassPhrase = SimpleCrypto.toHex("");

                            json_params.put(Define.JSON_PIN_NUMEBR,getConfigParam(Define.JSON_PIN_NUMEBR));
                            json_params.put(Define.JSON_ORDERING,HexOrdering);
                            json_params.put(Define.JSON_PASSPHRASE,hexpassPhrase);

                            Toast.makeText(MainActivity.this, "키생성 절차를 시작합니다.\n 카드를 다시 터치 하세요", Toast.LENGTH_LONG).show();
                            runNFCActivityJSON(NFCActivity.MODE_CREATE_KEY ,json_params);*/
                    return;
                } else {


                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.i("forDoc","onActivityResult MODE_WALLETINFO main?imei : ");
            wb.loadUrl(getAppUrl() + "main?imei=" + Utils.getIMEI(getApplicationContext()));
        }
    }

    private void onActivityResult_NFCActivity_MODE_PREPARE_CREATE_WALLET(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);
            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            String params = data.getStringExtra(Define.INTENT_PARAMS);

            try {
                JSONObject json = new JSONObject(params);

                String tsn = json.getString(Define.JSON_TSN);
                if (!m_strTSN.equals(tsn)) {
                    Toast.makeText(MainActivity.this, R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return;
                }
                //json.put("cointype",cointype);
                json.put("sig", sig);
                json.put("pub", pub);
                json.put("chain", chain);

                RestResult rr = call_REST(Define.URL_API_GPKP, json);

                JSONObject outparam = new JSONObject(rr.m_strResult);
                String prefix = outparam.getString("res");

                if (prefix.equals("02") || prefix.equals("03")) {
                    JSONObject inparams = new JSONObject();
                    String cointype = json.getString(Define.JSON_COIN_TYPE);
                    String pin = json.getString(Define.JSON_PIN_NUMEBR);

                    inparams.put(Define.JSON_PREFIX, prefix);
                    inparams.put(Define.JSON_COIN_TYPE, cointype);
                    inparams.put(Define.JSON_PIN_NUMEBR, pin);
                    Toast.makeText(this, R.string.app_message_00028, android.widget.Toast.LENGTH_LONG).show();
                    runNFCActivityJSON(NFCActivity.MODE_COMPELTE_CREATE_WALLET, inparams);

                } else {

                    Toast.makeText(this, R.string.app_message_00029, android.widget.Toast.LENGTH_LONG).show();
                }


            } catch (InvalidDataException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    private void onActivityResult_NFCActivity_MODE_COMPELETE_CREATE_WALLET(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);
            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            String params = data.getStringExtra(Define.INTENT_PARAMS);

            try {
                JSONObject root = new JSONObject(params);

                String tsn = root.getString(Define.JSON_TSN);
                if (!m_strTSN.equals(tsn)) {
                    Toast.makeText(MainActivity.this,R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return;
                }

                String createdCointype = root.getString(Define.JSON_COIN_TYPE);
                String real_walletinfo = root.getString(Define.JSON_LIST_WALLET_INFO);
                m_JWalletInfo = new JSONObject(real_walletinfo);

                // List Wallet info를 2중으로 넣음
                if (m_JWalletInfo.has(Define.JSON_LIST_WALLET_INFO)) {
                    String strWalletInfo = m_JWalletInfo.getString(Define.JSON_LIST_WALLET_INFO);
                    if (strWalletInfo != null) {
                        m_CoinList = new JSONArray();
                        setConfigParam(Define.CONFIG_LIST_WALLET_INFO, strWalletInfo);
                        JSONArray arr = new JSONArray(strWalletInfo);
                        m_JappWalletInfo = m_JWalletInfo;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject j = arr.getJSONObject(i);
                            try {
                                String strPubKey = j.getString(Define.JSON_PUBLIC_KEY);
                                String cointype = j.getString(Define.JSON_COIN_TYPE);
                                String result = getAddressFromAPI(strPubKey, cointype,"");
                                JSONObject jj = new JSONObject(result);
                                String wallet = jj.getString("fromAddress");


                                if (createdCointype.equals(cointype)) {

                                    setConfigParam(Define.CONFIG_DEFAULT_COINTYPE, cointype);
                                    setConfigParam(Define.CONFIG_DEFAULT_PUBLICKEY, strPubKey);
                                    setConfigParam(Define.CONFIG_DEFAULT_WALLET, wallet);
                                    double tmpexponent = jj.getDouble("exponent");
                                    setPowexponenet(tmpexponent);
                                }


                                JSONObject obj = new JSONObject();
                                obj.put(Define.CONFIG_DEFAULT_COINTYPE, cointype);
                                obj.put(Define.CONFIG_DEFAULT_WALLET, wallet);
                                obj.put(Define.CONFIG_DEFAULT_PUBLICKEY, strPubKey);
                                Log.i("Main", "onPageFinished - Activityreuslt 2 : m_CoinList" + obj);
                                m_CoinList.put(obj);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }


                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            checkEthereumAddress();
            ChangeWalletInfo();

            Toast.makeText(this, R.string.app_message_00030, android.widget.Toast.LENGTH_LONG).show();
            //wb.loadUrl(APP_URL+"main?imei="+Utils.getIMEI(getApplicationContext()));
            wb.reload();
        }
    }

    private void onActivityResult_NFCActivity_MODE_SET_NEW_KEY(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);
            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);

            Toast.makeText(this, R.string.app_message_00031, android.widget.Toast.LENGTH_LONG).show();
            m_JWalletInfo = new JSONObject();
            setConfigParam(Define.CONFIG_DEFAULT_COINTYPE, "");
            setConfigParam(Define.CONFIG_DEFAULT_PUBLICKEY, "");
            setConfigParam(Define.CONFIG_DEFAULT_WALLET, "");
            ChangeWalletInfo();

            wb.loadUrl(getAppUrl() + "main?imei=" + Utils.getIMEI(getApplicationContext()));
        }
    }

    private void onActivityResult_NFCActivity_MODE_COMPELETE_CREATE_KEY(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);
            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);

            String outparamstring = data.getStringExtra(Define.INTENT_PARAMS);

            Log.i("forDoc","onActivityResult MODE_COMPELTE_CREATE_KEY outparamstring : "+outparamstring);
            Log.i("forDoc","onActivityResult MODE_COMPELTE_CREATE_KEY strResult : "+strResult);
            Log.i("forDoc","onActivityResult MODE_COMPELTE_CREATE_KEY iSW : "+iSW);


            try {
                JSONObject outparam = new JSONObject(outparamstring);

                String tsn = outparam.getString(Define.JSON_TSN);
                if (!m_strTSN.equals(tsn)) {
                    Toast.makeText(MainActivity.this, R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }



            Toast.makeText(this, R.string.app_message_00031, android.widget.Toast.LENGTH_LONG).show();

            if(m_iCreateKeyMode == 1)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getResources().getString(R.string.app_message_00032));
                builder.setMessage(getResources().getString(R.string.app_message_00033));
                builder.setPositiveButton(getResources().getString(R.string.app_message_00032), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        clearWalletInfo();
                        terminateApp();
                    }
                });
                builder.setCancelable(false);

                builder.show();
            }
            else
            {
                wb.loadUrl(getAppUrl() + "main?imei=" + Utils.getIMEI(getApplicationContext()));
            }

        }
    }

    private void onActivityResult_NFCActivity_MODE_PREPARE_RESTORE_KEY(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);
            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            JSONObject json = new JSONObject();
            try {
                json.put(Define.JSON_TSN,m_strTSN);
                json.put(Define.JSON_PIN_NUMEBR,m_PIN);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Toast.makeText(this, R.string.app_message_00034, android.widget.Toast.LENGTH_LONG).show();


            runNFCActivityJSON(NFCActivity.MODE_COMPELETE_RESTORE_KEY,json);

        }
    }

    private void onActivityResult_NFCActivity_MODE_COMPELETE_RESTORE_KEY(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);
            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            JSONObject json = new JSONObject();
            try {
                json.put(Define.JSON_TSN,m_strTSN);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        AlertUtil.terminateSimpleEnd(this, getResources().getString(R.string.app_message_00032), getResources().getString(R.string.app_message_00035), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearWalletInfo();
                terminateApp();
            }
        });
    }

    private void onActivityResult_NFCActivity_MODE_CREATE_KEY(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);

            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            String outstring = data.getStringExtra(Define.INTENT_PARAMS);

            Log.i("forDoc","onActivityResult MODE_CREATE_KEY Act_result : "+outstring);
            Log.i("forDoc","onActivityResult MODE_CREATE_KEY strResult : "+strResult);
            Log.i("forDoc","onActivityResult MODE_CREATE_KEY iSW : "+iSW);

            try {
                int csLen = 8;
                String csn = m_strCSN;
                if (m_JWalletInfo.has(Define.JSON_CSN)) {
                    csn = m_JWalletInfo.getString(Define.JSON_CSN);
                }

                JSONObject outputparam = new JSONObject(outstring);

                String tsn = outputparam.getString(Define.JSON_TSN);
                if (!m_strTSN.equals(tsn)) {
                    Toast.makeText(MainActivity.this, R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return;
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private void onActivityResult_NFCActivity_MODE_DELETE_WALLET(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;


        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);
            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            String Act_result = data.getStringExtra(Define.INTENT_PARAMS);

            Log.i("forDoc","onActivityResult MODE_DELETE_WALLET Act_result : "+Act_result);
            Log.i("forDoc","onActivityResult MODE_DELETE_WALLET strResult : "+strResult);
            Log.i("forDoc","onActivityResult MODE_DELETE_WALLET iSW : "+iSW);

            try {
                JSONObject json_result = new JSONObject(Act_result);

                String tsn = json_result.getString(Define.JSON_TSN);
                if (!m_strTSN.equals(tsn)) {
                    Toast.makeText(MainActivity.this, R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return;
                }

                String cointype = json_result.getString(Define.JSON_COIN_TYPE);

                removeCoin(cointype);

            } catch (JSONException e) {
                e.printStackTrace();
            }


            Toast.makeText(this, R.string.app_message_00036, android.widget.Toast.LENGTH_LONG).show();
            synchronized (balanaceObj) {
                balanaceObj.obj = "";
                balanaceObj.bl = "";
                balanaceObj.ct = "";
            }

            synchronized (m_txseObj) {
                m_txseObj.obj = "";
                m_txseObj.ct = "";
            }

            setConfigParam(Define.CONFIG_DEFAULT_WALLET, "Create or select Wallet");
            wb.reload();
        }
    }

    private void  onActivityResult_NFCActivity_MODE_VERIFY_PIN(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String stroutparam = data.getStringExtra(Define.INTENT_PARAMS);
        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        Log.i("forDoc","onActivityResult MODE_VERIFY_PIN stroutparam : "+stroutparam);
        Log.i("forDoc","onActivityResult MODE_VERIFY_PIN strResult : "+strResult);
        Log.i("forDoc","onActivityResult MODE_VERIFY_PIN iSW : "+iSW);

        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {

            process_result_fail(requestCode,iSW);

            if (iSW == Define.SW_FILE_INVALID) {

                try {
                    JSONObject outparam = new JSONObject(stroutparam);
                    String needSeed = outparam.getString(Define.JSON_NEED_SEED);
                    //
                    final String val = needSeed;
                    wb.post(new Runnable() {
                        @Override
                        public void run() {
                            wb.evaluateJavascript(String.format("alertresetpin('%s')", val), null);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            isLoginFrag = true;
            try {
                JSONObject outparam = new JSONObject(stroutparam);
                String pinnumber = outparam.getString(Define.JSON_PIN_NUMEBR);

                String tsn = outparam.getString(Define.JSON_TSN);
                if (!m_strTSN.equals(tsn)) {
                    Toast.makeText(MainActivity.this, R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return;
                }

                m_PIN = pinnumber;
                setConfigParam(Define.CONFIG_PIN_NUMBER, pinnumber);
                setConfigParam(Define.CONFIG_SET_PIN, Define.CONFIG_TRUE_CODE);

                if (!m_JWalletInfo.getString(Define.JSON_SEED).equals(Define.CONFIG_TRUE_CODE)) {
                    /**
                     * 순서 변경 정보등의
                     * 입력 절차를 빼고 진행 한다.
                     *
                     */
                    m_iCreateKeyMode = 0;
                    runCreateKeyProcess(null);

                            /*JSONObject json_params= new JSONObject();

                            //String ordering = m_OrderingNumbers.tohex

                            String HexOrdering = SimpleCrypto.toHex("000000");

                            String hexpassPhrase = SimpleCrypto.toHex("");

                            json_params.put(Define.JSON_PIN_NUMEBR,getConfigParam(Define.JSON_PIN_NUMEBR));
                            json_params.put(Define.JSON_ORDERING,HexOrdering);
                            json_params.put(Define.JSON_PASSPHRASE,hexpassPhrase);

                            Toast.makeText(MainActivity.this, "키생성 절차를 시작합니다.\n 카드를 다시 터치 하세요", Toast.LENGTH_LONG).show();

                            runNFCActivityJSON(NFCActivity.MODE_CREATE_KEY ,json_params);
                            return ;*/
                } else {

                    wb.loadUrl(getAppUrl() + "main?imei=" + Utils.getIMEI(getApplicationContext()));
                    return;
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

    private void onActivityResult_NFCActivity_MODE_RESET_NEW_PIN(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);
            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            String strJson = data.getStringExtra(Define.INTENT_PARAMS);

            try {
                JSONObject outparam = new JSONObject(strJson);
                String tsn = outparam.getString(Define.JSON_TSN);
                m_PIN = outparam.getString(Define.JSON_PIN_NUMEBR);
                if (!m_strTSN.equals(tsn)) {
                    Toast.makeText(MainActivity.this, R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return;
                }

                setConfigParam(Define.CONFIG_PIN_NUMBER, m_PIN);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(MainActivity.this, R.string.app_message_00037, Toast.LENGTH_LONG).show();
        m_iCreateKeyMode = 0;
        runCreateKeyProcess(null);
    }

    private void onActivityResult_NFCActivity_MODE_RESET_PIN(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
            process_result_fail(requestCode,iSW);
            return;
        } else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            String strJson = data.getStringExtra(Define.INTENT_PARAMS);

            try {
                JSONObject outparam = new JSONObject(strJson);
                String tsn = outparam.getString(Define.JSON_TSN);
                m_PIN = outparam.getString(Define.JSON_PIN_NUMEBR);
                if (!m_strTSN.equals(tsn)) {
                    Toast.makeText(MainActivity.this, R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return;
                }

                setConfigParam(Define.CONFIG_PIN_NUMBER, m_PIN);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(MainActivity.this, R.string.app_message_00038, Toast.LENGTH_LONG).show();
        wb.post(new Runnable() {
            @Override
            public void run() {
                wb.loadUrl(getAppUrl()+"main?imei="+Utils.getIMEI(getApplicationContext()));
            }
        });
    }

    private void onActivityResult_NFCActivity_MODE_CHANGE_PIN(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL))
        {
            process_result_fail(requestCode,iSW);
            return ;
        }
        else
        {
            playSound(getApplicationContext(),Define.SOUND_TAG_OUT);
            String strJson = data.getStringExtra(Define.INTENT_PARAMS);

            Log.i("forDoc","onActivityResult MODE_CHANGE_PIN strJson : "+strJson);
            Log.i("forDoc","onActivityResult MODE_CHANGE_PIN strResult : "+strResult);
            Log.i("forDoc","onActivityResult MODE_CHANGE_PIN iSW : "+iSW);


            try {
                JSONObject outparam = new JSONObject(strJson);
                String tsn = outparam.getString(Define.JSON_TSN);
                m_PIN = outparam.getString(Define.JSON_PIN_NUMEBR);
                if(!m_strTSN.equals(tsn))
                {
                    Toast.makeText(MainActivity.this, R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return ;
                }

                setConfigParam(Define.CONFIG_PIN_NUMBER,m_PIN);

            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        Toast.makeText(MainActivity.this, R.string.app_message_00037, Toast.LENGTH_LONG).show();
        wb.post(new Runnable() {
            @Override
            public void run() {
                wb.loadUrl(getAppUrl()+"main?imei="+Utils.getIMEI(getApplicationContext()));
            }
        });
    }

    private void onActivityResult_NFCActivity_MODE_SET_NEWPIN(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL))
        {
            process_result_fail(requestCode,iSW);
            return ;
        }
        else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            String strJson = data.getStringExtra(Define.INTENT_WALLENT_INFO);

            try {
                JSONObject outparam = new JSONObject(strJson);
                String tsn = outparam.getString(Define.JSON_TSN);
                if (!m_strTSN.equals(tsn)) {
                    Toast.makeText(MainActivity.this, R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return;
                }

                setConfigParam(Define.CONFIG_PIN_NUMBER, m_PIN);
                setConfigParam(Define.CONFIG_SET_PIN, Define.CONFIG_TRUE_CODE);


                m_JWalletInfo = outparam;


                Log.d("WALLET_INFO", m_JWalletInfo.toString());

                if (!m_JWalletInfo.getString(Define.JSON_SEED).equals(Define.CONFIG_TRUE_CODE)) {
                    /**
                     *
                     * 순서 변경의 정보를 빼고 진행 한다.
                     *
                     */

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onActivityResult_QrCodeReadActivity_REQUEST_CODE_SCAN_OR_CODE(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        final String address;

        if (resultCode == RESULT_OK) {
            address = data.getStringExtra(QrCodeReadActivity.KEY_EXTRA_ADDRESS);
            String resultAddress = address ;
            String resultAmount = "";
            if(IBan.isIban(address))
            {
                IBANItem item = IBan.decodeQRString(address);
                resultAddress = item.Address;
                resultAmount = item.amount;

            }
            else {
                boolean invokedException = false;
                try {
                    JSONObject json = new JSONObject(address);
                    resultAddress = json.getString(Define.JSON_TO_ADDRESS);
                    resultAmount = json.getString(Define.JSON_SIGN_AMOUNT);

                } catch (JSONException e) {
                    e.printStackTrace();
                    invokedException = true;

                }

                if(invokedException)
                {
                    String[] strr = resultAddress.split(":");
                    if(strr.length > 1)
                    {
                        resultAddress =strr[1];
                    }
                }
            }



            final String retAddress =resultAddress;
            final String retAmount = resultAmount;
            wb.post(new Runnable() {
                @Override
                public void run() {
                    wb.evaluateJavascript(String.format("setAmount('%s')", retAmount), null);
                    wb.evaluateJavascript(String.format("setSender('%s')", retAddress), null);
                }
            });


        } else {

        }
    }


    private void onActivityResult_NFCActivity_MODE_REGIST(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strVal = data.getStringExtra(Define.INTENT_RESULT);

        if (strVal.equals(Define.INTENT_RESULT_OK)) {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);

            Toast.makeText(this, R.string.app_message_00039, android.widget.Toast.LENGTH_LONG).show();
            wb.post(new Runnable() {
                @Override
                public void run() {
                    String token = FirebaseInstanceId.getInstance().getToken();
                    wb.evaluateJavascript(String.format("createWallet('%s','%s')", Utils.getIMEI(getApplicationContext()), token), null);
                }
            });
        }
        else {
            Toast.makeText(this, R.string.app_message_00045, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void onActivityResult_NFCActivity_MODE_SIGN(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strVal = data.getStringExtra(Define.INTENT_RESULT);

        if (strVal.equals(Define.INTENT_RESULT_OK))
        {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);

            Toast.makeText(this, R.string.app_message_00040, android.widget.Toast.LENGTH_LONG).show();
            Log.i("API READY RESULT", "MODE_AUTH");
            String strResult = data.getStringExtra(Define.INTENT_RESULT);
            int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
            if (strResult.equals(Define.INTENT_RESULT_FAIL)) {
                String strLog = String.format(getResources().getString(R.string.app_message_00041) + " : %04X", iSW);
                Log.d("ERROR", strLog);
                Toast.makeText(this, strLog, android.widget.Toast.LENGTH_LONG).show();

                wb.loadUrl(getAppUrl() + "main?imei=" + Utils.getSetttingInfo(getApplicationContext(), "imei"));
                return;

            RestResult rr = new RestResult();

            String json_string = data.getStringExtra("JSON_READY");
            String strtoAddress = "";
            double amount1 = 0;
            JSONObject json = null;
            try {
                json = new JSONObject(json_string);
                strtoAddress = json.getString("toAddress");
                amount1 = TestClass.parseDouble(json.getString("amount"),"9");

            } catch (JSONException e) {

            } catch (JSONException e) {

            }
//                        Toast.makeText(this, "마무리 작업을 하고 있습니다.", android.widget.Toast.LENGTH_LONG).show();
            rr = call_REST(Define.URL_API_READY, obj);
            Log.i("API READY RESULT", rr.m_strResult);

            // 태그 후 결과를 받는다.
//                        Log.d("Second Ready strRsult",rr.m_strResult);

            try {

                if (rr.m_strResult != null && rr.m_strResult != "failed") {
                    JSONObject jRes = new JSONObject(rr.m_strResult);
                    strResult = jRes.getString("result");
                    if (strResult.equals("ok"))
                        Toast.makeText(this, R.string.app_message_00043, android.widget.Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(this, R.string.app_message_00042, android.widget.Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.app_message_00042, android.widget.Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            wb.loadUrl(getAppUrl() + "main?imei=" + Utils.getSetttingInfo(getApplicationContext(), "imei"));
        }
        else {
            Toast.makeText(this, R.string.app_message_00045, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void onActivityResult_NFCActivity_MODE_AUTH(final int requestCode, int resultCode, final Intent data)
    {
        if(data == null) return ;

        String strVal = data.getStringExtra(Define.INTENT_RESULT);

        if (strVal.equals(Define.INTENT_RESULT_OK)) {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);

            Toast.makeText(this, R.string.app_message_00044, android.widget.Toast.LENGTH_LONG).show();
            // 한번이라도 인증을 했다면 키는 이미 셋팅이 되었다고 한다.

            if (isInitKey()) {
                wb.post(new Runnable() {
                    @Override
                    public void run() {
                        wb.evaluateJavascript(String.format("goMain('%s')", Utils.getIMEI(getApplicationContext())), null);
                    }
                });
            } else {

                //키가 생성이 안된 경우이므로 키를 생성 해주고 워드 리스트로 이동한다.
                wb.post(new Runnable() {
                    @Override
                    public void run() {
                        String token = FirebaseInstanceId.getInstance().getToken();
                        wb.evaluateJavascript(String.format("goWordlist('%s','%s')", Utils.getIMEI(getApplicationContext()), token), null);
                    }
                });
            }
        }
        else {
            Toast.makeText(this, R.string.app_message_00045, android.widget.Toast.LENGTH_LONG).show();
        }
    }


    private void onActivityResult_Define_MODE_PICK_INPUT_FILE_IMAGE(final int requestCode, int resultCode, final Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mFilePathCallback == null) {
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
                Uri[] results = new Uri[]{getResultUri(data)};

                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            } else {
                if (mUploadMessage == null) {
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
                Uri result = getResultUri(data);

                Log.d(getClass().getName(), "openFileChooser : "+result);
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }
        else {
            if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
            if (mUploadMessage != null) mUploadMessage.onReceiveValue(null);
            mFilePathCallback = null;
            mUploadMessage = null;
        }
    }

    private void  onActivityResult_NFCActivity_MODE_INIT_ZERO_PIN(final int requestCode, int resultCode, final Intent data)
    {
        Log.d("TTTT","onActivityResult_NFCActivity_MODE_INIT_ZERO_PIN");

        String strResult = data.getStringExtra(Define.INTENT_RESULT);
        int iSW = data.getIntExtra(Define.INTENT_STATUS_WORD, 0);
        if (strResult.equals(Define.INTENT_RESULT_FAIL))
        {
            process_result_fail(requestCode,iSW);
            return ;
        }
        else {
            playSound(getApplicationContext(), Define.SOUND_TAG_OUT);
            String strJson = data.getStringExtra(Define.INTENT_WALLENT_INFO);

            try {
                JSONObject outparam = new JSONObject(strJson);
                String tsn = outparam.getString(Define.JSON_TSN);
                if (!m_strTSN.equals(tsn)) {
                    Toast.makeText(MainActivity.this, R.string.app_message_00027, Toast.LENGTH_LONG).show();
                    return;
                }

                setConfigParam(Define.CONFIG_PIN_NUMBER, m_PIN);
                setConfigParam(Define.CONFIG_SET_PIN, Define.CONFIG_TRUE_CODE);


                m_JWalletInfo = outparam;


                Log.d("WALLET_INFO", m_JWalletInfo.toString());

                if (!m_JWalletInfo.getString(Define.JSON_SEED).equals(Define.CONFIG_TRUE_CODE)) {
                    /**
                     *
                     * 순서 변경의 정보를 빼고 진행 한다.
                     *
                     */
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }





    private Uri getResultUri(Intent data) {
        Uri result = null;
        if(data == null || TextUtils.isEmpty(data.getDataString())) {
            // If there is not data, then we may have taken a photo
            if(mCameraPhotoPath != null) {
                result = Uri.parse(mCameraPhotoPath);
            }
        } else {
            String filePath = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                filePath = data.getDataString();
            } else {
                filePath = "file:" + RealPathUtil.getRealPath(this, data.getData());
            }
            result = Uri.parse(filePath);
        }

        return result;
    }

    public void removeCoin(String arg_cointype)
    {
        String strWalletInfo = null;
        try {
            strWalletInfo = m_JWalletInfo.getString(Define.JSON_LIST_WALLET_INFO);

            if(strWalletInfo != null)
            {
                m_CoinList = new JSONArray();

                JSONArray arr = new JSONArray(strWalletInfo);


                if(arr.length() > 0)
                {
                    int findindex =-1 ;
                    for (int i=0; i < arr.length(); i++) {
                        JSONObject j = arr.getJSONObject(i);
                        try {
                            String strPubKey = j.getString(Define.JSON_PUBLIC_KEY);
                            String cointype = j.getString(Define.JSON_COIN_TYPE);
                            String result = getAddressFromAPI(strPubKey, cointype,"");
                            JSONObject jj = new JSONObject(result);
                            String wallet = jj.getString("fromAddress");
                            if (cointype.equals(arg_cointype)) {
                                findindex = i;
                                continue;
                            }


                            JSONObject obj = new JSONObject();
                            obj.put(Define.CONFIG_DEFAULT_COINTYPE, cointype);
                            obj.put(Define.CONFIG_DEFAULT_WALLET, wallet);
                            obj.put(Define.CONFIG_DEFAULT_PUBLICKEY, strPubKey);
                            Log.i("Main", "onPageFinished - Activityreuslt 3 : m_CoinList" + obj);

                            m_CoinList.put(obj);
                        } catch (JSONException e) {

                        }
                    }

                    if(findindex>=0) {
                        arr.remove(findindex);
                    }
                    findindex = -1;
                }
                m_JWalletInfo.put(Define.JSON_LIST_WALLET_INFO,arr.toString());
                m_JappWalletInfo = m_JWalletInfo;
                setConfigParam(Define.CONFIG_LIST_WALLET_INFO,arr.toString());
                wb.post(new Runnable() {
                    @Override
                    public void run() {
                        wb.reload();
                    }
                });
                ChangeWalletInfo();
                checkEthereumAddress();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


      public Handler m_updateUIHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {


                  //  Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                  //  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   // startActivity(intent);
                   // wb.evaluateJavascript(String.format("goMain('%s')", Utils.getIMEI(getApplicationContext())), null);
                    wb.reload();


                Toast.makeText(getApplicationContext(), R.string.app_message_00046, android.widget.Toast.LENGTH_LONG).show();
            }
        }
    };

    private Handler m_getbalanceHandler = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){

        }

    };

    private Handler m_txsHandler = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){

        }
    };

    // 버젼 체크
    String marketVersion, verSion;
    boolean m_diffVersion = false;
    AlertDialog.Builder mDialog;


    class ThreadSyncObject{
        public boolean isruning = true;
        public Object obj = null;
        public String bl = "";
        public String ct = "";
        public String pk = "";
        public String contract = "";
        public String addtional = "";
    }

    private class getMarketVersion extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {

            try {
                Document doc = Jsoup
                        .connect(
                                "https://play.google.com/store/apps/details?id=io.kaiser.kaiserwallet" )
                        .get();
                Elements Version = doc.select("div[itemprop='description']");

                for (Element v : Version) {
                    if (v.attr("itemprop").equals("description")) {
                        String[] line  = v.text().split("\n");
                        if(line.length > 0) {
                            String[] version = line[0].split(" ");
                            marketVersion = version[0].trim();
                        }
                        else
                        {
                            marketVersion = "";
                        }
                    }
                }
                return marketVersion;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            PackageInfo pi = null;
            try {
                pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            verSion = pi.versionName;
            //verSion = "1.0.6.0";
            marketVersion = result;

            if (!verSion.equals(marketVersion)) {
                m_diffVersion = true;

                AlertUtil.showSimpleCore(getApplicationContext(), getResources().getString(R.string.app_message_00047), getResources().getString(R.string.app_message_00049), getResources().getString(R.string.app_message_00048), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent marketLaunch = new Intent(
                                Intent.ACTION_VIEW);
                        marketLaunch.setData(Uri
                                .parse("https://play.google.com/store/apps/details?id=io.kaiser.kaiserwallet"));
                        startActivity(marketLaunch);
                        finish();
                    }
                }, null, null);
            }
            else{
                OnCreate2();
            }

            super.onPostExecute(result);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public static void playSound(final Context context, final int type)
    {

        new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                MediaPlayer mediaPlayer = new MediaPlayer();
                int resId = -1;
                switch (type)
                {
                    case Define.SOUND_TAG_OUT:
                        resId=R.raw.tag_end;
                        break;
                }

                if (resId != -1)
                {
                    mediaPlayer = MediaPlayer.create(context, resId);
                    mediaPlayer.setLooping(false);
                    mediaPlayer.setVolume(1,1);
                    mediaPlayer.start();

                    while (mediaPlayer.isPlaying() == true)
                    {
                    }
                }
            }
        }).start();
    }

    //ProgressDialog_show_윤규현_2018.07.24
    private void showProgress(String msg, Context mContext)
    {
        if(pd == null)
        {
            pd = new ProgressDialog(mContext);
            pd.setCancelable(false);
        }
        pd.setMessage(msg);
        pd.show();
    }
    //ProgressDialog_open_윤규현_2018.07.24
    final Handler showProgressHandler_1 = new Handler()
    {
        public void handleMessage(Message msg)
        {
            showProgress(getResources().getString(R.string.app_message_00050), MainActivity.this);
        }
    };

    //ProgressDialog_close_윤규현_2018.07.24
    final Handler showProgressHandler_2 = new Handler()
    {
        public void handleMessage(Message msg)
        {
            if(pd != null && pd.isShowing())
            {
                pd.dismiss();
            }
        }
    };


    final Handler showProgressBLE = new Handler()
    {
        public void handleMessage(Message msg)
        {
            showProgress("연결중입니다.", MainActivity.this);
        }
    };

    final Handler dismissProgressBLE = new Handler()
    {
        public void handleMessage(Message msg)
        {
            if(pd != null && pd.isShowing())
            {
                pd.dismiss();
            }
        }
    };




    /**
     * Proceed the purchase operation
     *
     * @param withFingerprint {@code true} if the purchase was made by using a fingerprint
     * @param cryptoObject the Crypto object
     */
    public void onSuccessFingerPrint(boolean withFingerprint,
                            @Nullable FingerprintManager.CryptoObject cryptoObject) {

        m_bFinishFinger = true;
        Log.d("","");
        checkAppEnvironmentafterRunNFC();
    }

    // Show confirmation, if fingerprint was used show crypto information.
    private void showConfirmation(byte[] encrypted) {
//        findViewById(R.id.confirmation_message).setVisibility(View.VISIBLE);
        if (encrypted != null) {
//            TextView v = findViewById(R.id.encrypted_message);
//            v.setVisibility(View.VISIBLE);
//            v.setText(Base64.encodeToString(encrypted, 0 /* flags */));
        }
    }




}



