package io.kaiser.kaiserwallet2.Device;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.kaiser.kaiserwallet2.Device.Bluetooth.BLESingleton;
import io.kaiser.kaiserwallet2.Utils;


/*
    BB : BizBlocks
 */

public class BBNfcTag implements INfcTag {

    int mTimeOut = 0;

    BLESingleton mBleInstance = null;

    public BBNfcTag()
    {

    }

    @Override
    public void setCore(Object obj)
    {
        Context context = (Context)obj;

        mBleInstance = BLESingleton.getInstance(context);
    }

    @Override
    public void connect() throws IOException {
        mBleInstance.piccActivate();
    }

    @Override
    public void disConnect() {
        mBleInstance.piccDeactivate();
    }

    @Override
    public void setTimeOut(int timeout) {
        mTimeOut = timeout;
    }

    @Override
    public byte[] transceive(byte[] data) throws IOException {

        String strData = Utils.ByteToStr(data);

        String ret = mBleInstance.piccAccess(strData);

        byte[] retBytes = Utils.StrToByte(ret);

        return retBytes;
    }

    @Override
    public boolean checkGate() {

        if(mBleInstance != null)
        {
            //PiccActive가 끊기면 제조에서 Listener지원이 없어서 알 방법이 없기 때문에, 무조건 activate전에는 deactivate를 호출한다.
            mBleInstance.piccDeactivate();

            return mBleInstance.piccActivate();
        }

        return false;
    }

    @Override
    public void openGate() {

    }
}
