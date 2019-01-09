package io.kaiser.kaiserwallet2.Device.Bluetooth;

import android.Manifest;
import android.content.Context;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import io.kaiser.kaiserwallet2.MainActivity;
import mpay.sdk.lib.BTLib;
import mpay.sdk.lib.CommonLib;
import mpay.sdk.lib.interfaces.BluetoothListener;
import mpay.sdk.lib.interfaces.CommandListener;
import mpay.sdk.lib.model.DevItem;

import static io.kaiser.kaiserwallet2.BuildConfig.IS_BASIC_NFC;

public class BLESingleton implements CommandListener, BluetoothListener {

    //Final Datas
    public final String BT_TAG = "BLESingleton"; //Log Tag
    private final int BT_DEVICE_NAME_INPUT_LENGTH_FROM_USER = 9; //유저가 입력해야되는 블루투스 기기 이름의 길이

    //SyncronizedObject
    Object objBleScanLatch = new Object(); //BLE Scan
    Object objBleConnectLatch = new Object(); //BLE Connect
    Object objPiccLatch = new Object(); //PICC

    //Latchs
    CountDownLatch bleScanLatch = null; //BLE Scan
    CountDownLatch bleConnectLatch = null; //BLE Connect
    CountDownLatch piccControlLatch = null; //PICC

    //Basic
    private static BLESingleton instance = null;
    private Context context;
    private BTLib btLib = null;
    private CommonLib commonLib = null;



    //Device
    private String lastNeedSearchDeviceName = null; //마지막으로 찾기 원하는 디바이스의 이름
    private boolean isActivatePicc = false; //PICC기기의 활성화 여부
    private String lastPiccAccessData = null; //PICC기기의 반환된 마지막 데이터

    private long cmdTimeOut = 1000 * 5;

    //Variables
    public String result = "";
    private ArrayList<DevItem> mConnectDevItems = null;

    private boolean retConnect = false;


    //Singleton Constructor
    public static BLESingleton getInstance(Context ctx){
        if (instance == null) {
            synchronized (BLESingleton.class) {
                if (instance == null) {
                    instance = new BLESingleton(ctx);
                }

            }
        }
        return instance;
    }

    //Constructor
    public BLESingleton(Context ctx){
        context = ctx;

        initBLE();
    }

    public Boolean initBLE(){

        //NFC 모델 앱이라면 false;
        if(IS_BASIC_NFC)
        {
            return false;
        }

        try
        {
            if (btLib == null)
            {
                btLib = new BTLib(context, true);
                commonLib = btLib;
                btLib.setCommandListener(this);
                btLib.setBTListener(this);
                commonLib.setCommandListener(this);

                Log.v(BT_TAG, "BTlib create!!");

                return btLib.btStart();
            }
            return true;
        } catch (Exception e)
        {
            e.printStackTrace();
            Log.v(BT_TAG, "BT initial failed : " + e.toString());
            btLib = null;
        }
        return false;
    }

    public synchronized void BLEPause(){

        //NFC 모델 앱이라면 return;
        if(IS_BASIC_NFC)
        {
            return;
        }

        if (commonLib != null){
            commonLib.setCommandListener(null);
        }
    }

    public void BLEFinish(){

        //NFC 모델 앱이라면 return;
        if(IS_BASIC_NFC)
        {
            return;
        }

        if (commonLib != null)
        {

            commonLib.setCommandListener(null);
            if (btLib != null)
            {
                btLib.btStop();
            }
            commonLib = null;
        }
    }

    public void setCmdTimeOut(long timeOut){
        cmdTimeOut = timeOut;
    }


    ///////////////////////////////////////////////////////////////////////////////////
    // BLE

    //블루투스 기기 스캔
    public boolean bleScanDevice(String deviceName){

        synchronized (objBleScanLatch) {

            bleScanLatch = new CountDownLatch(1);

            lastNeedSearchDeviceName = deviceName;

            btLib.scanBTDevice();

            try {
                bleScanLatch.await(); //lock
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    //블루투스 연결 확인
    public boolean isBleConnect() {
        
        //NFC 모델 앱이라면 false;
        if(IS_BASIC_NFC)
        {
            return false;
        }

        return btLib.isConnectBTDevice();
    }

    //블루투스 연결 해제
    public boolean bleDisconnect() {

        //연결 되어있을 경우만 연결 해제
        if(isBleConnect())
        {
            synchronized (objBleConnectLatch)
            {
                bleConnectLatch = new CountDownLatch(1);

                btLib.disconnectBTDevice();

                try {
                    bleConnectLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        return true;
    }

    //블루투스 기기 연결
    public boolean bleConnectDevice(String deviceName, String deviceAddress){

        //접속할 대상 확인
        boolean useDeviceName = false;
        if(deviceName != null && deviceName.length() == BT_DEVICE_NAME_INPUT_LENGTH_FROM_USER){
            useDeviceName = true;
            lastNeedSearchDeviceName = deviceName;
        }

        //접속할 주소 확인
        boolean useDeviceAddress = false;
        if(!useDeviceName) {
            if(deviceAddress != null && deviceAddress.length() > 0){
                useDeviceAddress = true;
            }
        }

        //접속할 대상이 없으면 반환
        if(!useDeviceName && !useDeviceAddress){
            return false;
        }


        //접속할 기기 목록 확인
        DevItem findDevItem = null;
        if(mConnectDevItems == null || mConnectDevItems.size() == 0) {
            return false;
        }
        else {
            for(DevItem devItem : mConnectDevItems)
            {
                Log.d("BLE debug", "scan device : "+devItem.dev_name);
                if(useDeviceName)
                {
                    String devName = devItem.dev_name;

                    if(devName != null && devName.contains(deviceName)) {
                        findDevItem = devItem;
                        break;
                    }
                }
                else if(useDeviceAddress)
                {
                    String devAddress = devItem.dev_address;

                    if(devAddress != null && devAddress.equals(deviceAddress)) {
                        findDevItem = devItem;
                        break;
                    }
                }
            }
        }

        //접속할 기기가 있는지 확인
        if(findDevItem == null) {
            return false;
        }

        //접속

        synchronized (objBleConnectLatch)
        {
            // Latch 2 Step (onBluetoothDeviceConnecting, onBluetoothDeviceConnected)
            bleConnectLatch = new CountDownLatch(2);

            //btLib.connectBTDeviceByName()함수는 정상적으로 동작하지 않기 때문에 Address만 사용한다.
            String address = findDevItem.dev_address;
            if(address != null && address.length() > 0)
            {
                bleConnectByAddress(findDevItem.dev_address);
            }
            else
            {
                return false;
            }

            try {
                bleConnectLatch.await(); //lock
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        return retConnect;
    }

    //블루투스 주소로 연결
    public boolean bleConnectByAddress(String addr) {
        return btLib.connectBTDeviceByAddress(addr);
    }

    //블루투스 이름으로 연결
    public void bleConnectByName(String name) {
        btLib.connectBTDeviceByName(name);
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////
    // PICC

    public boolean piccActivate()
    {
        boolean ret = false;

        //Already Activate.
        if(isActivatePicc)
        {
            return true;
        }

        synchronized (objPiccLatch)
        {
            piccControlLatch = new CountDownLatch(1);

            if(commonLib != null){
                commonLib.cmdPICCActivate(cmdTimeOut);
            }

            try{
                piccControlLatch.await(); //lock
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                return false;
            }


            ret =  isActivatePicc;

        }

        return ret;
    }

    public boolean piccDeactivate() {
        boolean ret = false;

        synchronized (objPiccLatch) {
            piccControlLatch = new CountDownLatch(1);

            if(commonLib != null){
                commonLib.cmdPICCDeactivate(cmdTimeOut);
            }

            try{
                piccControlLatch.await();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                ret = false;
            }

            ret = isActivatePicc;
        }

        return ret;
    }

    public String piccAccess(String cAPDU) {
        String ret = null;

        synchronized (objPiccLatch) {
            piccControlLatch = new CountDownLatch(1);

            if (commonLib != null) {
                commonLib.cmdPICCAccess(cAPDU, cmdTimeOut);
            }

            try {
                piccControlLatch.await();
                ret = lastPiccAccessData;
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                ret = null;
            }

        }

        return ret;
    }








    ///////////////////////////////////////////////////////////////////////////////////////
    // Common Public Method

    //스캔 후 deviceName으로 연결한다.
    public boolean scanToConnectBle(String deviceName)
    {
        //해제
        boolean retDisconnect = bleDisconnect();
        if(!retDisconnect)
        {
            return false;
        }

        //스캔
        boolean retScan = bleScanDevice(deviceName);
        if(!retScan)
        {
            return false;
        }

        //접속
        boolean retConnect = bleConnectDevice(deviceName, null);
        if(!retConnect)
        {
            return false;
        }

        return true;
    }






    /////////////////////////////////////////////////////////////////////////////
    // Override - System


    @Override
    public void onReaderResponse(int returnCode, String returnMessage, String functionName) {
        Log.i(BT_TAG, "onReaderResponse func:"+functionName+",retMsg:"+returnMessage);

        isActivatePicc = false;
        piccControlLatch.countDown(); //unlock part.
    }

    @Override
    public void onSDKResponse(int i, String s, String s1) {
        Log.i(BT_TAG, "onSDKResponse");
    }

    @Override
    public void onSetUseVersion(boolean b) {
        Log.i(BT_TAG, "onSetUseVersion");
    }

    @Override
    public void onGetVersion(boolean b, String s) {
        Log.i(BT_TAG, "onGetVersion");
    }

    /////////////////////////////////////////////////////////////////////////////
    // Override - BLE

    @Override
    public void onBluetoothState(boolean b) {
        Log.i(BT_TAG, "onBluetoothState");
    }

    @Override
    public void onBluetoothDeviceScaning() {
        Log.i(BT_TAG, "onBluetoothDeviceScaning");
        if(mConnectDevItems==null) {
            mConnectDevItems = new ArrayList<>();
        }
        else {
            mConnectDevItems.clear();
        }
    }

    @Override
    public void onBluetoothDeviceFound(DevItem devItem) {
        Log.i(BT_TAG, "onBluetoothDeviceFound name : "+devItem.dev_name);
        Log.i(BT_TAG, "onBluetoothDeviceFound addr : "+devItem.dev_address);

        mConnectDevItems.add(devItem);

        //블루투스 서치에서 원하는 기기이름이 나오는지 찾기
        if(lastNeedSearchDeviceName != null && lastNeedSearchDeviceName.length() == BT_DEVICE_NAME_INPUT_LENGTH_FROM_USER) {
            String devName = devItem.dev_name;

            if(devName != null && devName.length() >= BT_DEVICE_NAME_INPUT_LENGTH_FROM_USER) {
                if(devName.contains(lastNeedSearchDeviceName)) {
                    //원하는 기기 이름이 나왔다면 검색을 중지함을 알림.
                    Log.i(BT_TAG,"onBluetoothDeviceFound - Request Stop");
                    bleScanLatch.countDown();
                }
            }
        }

    }

    @Override
    public void onBluetoothDeviceScanOver() {
        Log.i(BT_TAG, "onBluetoothDeviceScanOver");

        bleScanLatch.countDown();
    }

    @Override
    public void onBluetoothDeviceBounding() {
        Log.i(BT_TAG, "onBluetoothDeviceBounding");
    }

    @Override
    public void onBluetoothDeviceBoundSuccess() {
        Log.i(BT_TAG, "onBluetoothDeviceBoundSuccess");
        bleConnectLatch.countDown();
    }

    @Override
    public void onBluetoothDeviceBoundFailed() {
        Log.i(BT_TAG, "onBluetoothDeviceBoundFailed");
    }

    @Override
    public void onBluetoothDeviceConnecting() {
        Log.i(BT_TAG, "onBluetoothDeviceConnecting");
        bleConnectLatch.countDown(); //step 1
    }

    @Override
    public void onBluetoothDeviceConnected() {
        Log.i(BT_TAG, "onBluetoothDeviceConnected");
        retConnect = true;
        bleConnectLatch.countDown(); //step 2
    }

    @Override
    public void onBluetoothDeviceConnectFailed() {
        Log.i(BT_TAG, "onBluetoothDeviceConnectFailed");
        retConnect = false;
        bleConnectLatch.countDown(); //step 2
    }

    @Override
    public void onBluetoothDeviceDisconnected() {
        Log.i(BT_TAG, "onBluetoothDeviceDisconnected");
    }

    @Override
    public void onSetBluetoothDeviceName(boolean b) {
        Log.i(BT_TAG, "onSetBluetoothDeviceName");
    }



    /////////////////////////////////////////////////////////////////////////////
    // Override - Reader


    @Override
    public void onGetReaderSN(boolean b, String s) {
        Log.i(BT_TAG, "onGetReaderSN");
    }

    @Override
    public void onSetReaderSN(boolean b) {
        Log.i(BT_TAG, "onSetReaderSN");
    }

    @Override
    public void onDetectBattery(boolean b, String s) {
        Log.i(BT_TAG, "onDetectBattery");
    }

    @Override
    public void onSetSleepTimer(boolean b) {
        Log.i(BT_TAG, "onSetSleepTimer");
    }

    @Override
    public void onICCStatus(boolean b, String s) {
        Log.i(BT_TAG, "onICCStatus");
    }

    @Override
    public void onICCPowerOn(boolean b, String s) {
        Log.i(BT_TAG, "onICCPowerOn");
    }

    @Override
    public void onICCPowerOff(boolean b) {
        Log.i(BT_TAG, "onICCPowerOff");
    }

    @Override
    public void onICCAccess(boolean b, String s) {
        Log.i(BT_TAG, "onICCAccess");
    }

    @Override
    public void onGetCardInfo(boolean b, String s, String s1, String s2, String s3) {
        Log.i(BT_TAG, "onGetCardInfo");
    }

    @Override
    public void onPICCActivate(boolean b, String s) { //무조건 메인쓰레드에서 동작함
        Log.i(BT_TAG, "onPICCActivate Card SN: "+s);

        //synchronized (objPiccLatch) {
            isActivatePicc = b;
        //}
        piccControlLatch.countDown();

    }

    @Override
    public void onPICCDeactivate(boolean b) {
        Log.i(BT_TAG, "onPICCDeactivate");

        isActivatePicc = false;
        piccControlLatch.countDown();
    }

    @Override
    public void onPICCRate(boolean b, String s) {
        Log.i(BT_TAG, "onPICCRate");
    }

    @Override
    public void onPICCAccess(boolean b, String s) { //무조건 메인쓰레드에서 동작함
        Log.i(BT_TAG, "onPICCAccess");

        //synchronized (objPiccLatch) {
            lastPiccAccessData = s;
        //}

        piccControlLatch.countDown();
    }

    @Override
    public void onMifareAuth(boolean b) {
        Log.i(BT_TAG, "onMifareAuth");
    }

    @Override
    public void onMifareReadBlock(boolean b, String s) {
        Log.i(BT_TAG, "onMifareReadBlock");
    }

    @Override
    public void onMifareWriteBlock(boolean b) {
        Log.i(BT_TAG, "onMifareWriteBlock");
    }

    @Override
    public void onMifareIncrement(boolean b) {
        Log.i(BT_TAG, "onMifareIncrement");
    }

    @Override
    public void onMifareDecrement(boolean b) {
        Log.i(BT_TAG, "onMifareDecrement");
    }

    @Override
    public void onSetICCPort(boolean b) {
        Log.i(BT_TAG, "onSetICCPort");
    }

    @Override
    public void onSelectMemoryCardType(boolean b) {
        Log.i(BT_TAG, "onSelectMemoryCardType");
    }

    @Override
    public void onMemoryCardPowerOn(boolean b, String s) {
        Log.i(BT_TAG, "onMemoryCardPowerOn");
    }

    @Override
    public void onMemoryCardGetType(boolean b, String s, String s1) {
        Log.i(BT_TAG, "onMemoryCardGetType");
    }

    @Override
    public void onMemoryCardReadData(boolean b, String s) {
        Log.i(BT_TAG, "onMemoryCardReadData");
    }

    @Override
    public void onMemoryCardWriteData(boolean b) {
        Log.i(BT_TAG, "onMemoryCardWriteData");
    }

    @Override
    public void onMemoryCardPowerOff(boolean b) {
        Log.i(BT_TAG, "onMemoryCardPowerOff");
    }

    @Override
    public void onMemoryCardReadErrorCounter(boolean b, int i) {
        Log.i(BT_TAG, "onMemoryCardReadErrorCounter");
    }

    @Override
    public void onMemoryCardVerifyPSC(boolean b) {
        Log.i(BT_TAG, "onMemoryCardVerifyPSC");
    }

    @Override
    public void onMemoryCardGetPSC(boolean b, String s) {
        Log.i(BT_TAG, "onMemoryCardGetPSC");
    }

    @Override
    public void onMemoryCardModifyPSC(boolean b) {
        Log.i(BT_TAG, "onMemoryCardModifyPSC");
    }

    @Override
    public void onMemoryCardReadDataWithProtectBit(boolean b, String s) {
        Log.i(BT_TAG, "onMemoryCardReadDataWithProtectBit");
    }

    @Override
    public void onMemoryCardWriteDataWithProtectBit(boolean b) {
        Log.i(BT_TAG, "onMemoryCardWriteDataWithProtectBit");
    }

    @Override
    public void onMemoryCardReadProtectionData(boolean b, String s) {
        Log.i(BT_TAG, "onMemoryCardReadProtectionData");
    }

    @Override
    public void onMemoryCardWriteProtectionData(boolean b) {
        Log.i(BT_TAG, "onMemoryCardWriteProtectionData");
    }

    @Override
    public void onGiveUpAction(boolean b) {
        Log.i(BT_TAG, "onGiveUpAction");

    }
}
