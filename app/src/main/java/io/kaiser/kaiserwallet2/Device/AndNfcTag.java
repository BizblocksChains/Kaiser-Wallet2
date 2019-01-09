package io.kaiser.kaiserwallet2.Device;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import java.io.IOException;

/*
    And : Android
 */
public class AndNfcTag implements INfcTag {

    private Object mtx = new Object();

    private IsoDep mIsoDep = null;

    public AndNfcTag()
    {

    }

    @Override
    public void setCore(Object obj)
    {
        Tag tag = (Tag)obj;
        mIsoDep = IsoDep.get(tag);
    }

    @Override
    public void connect() throws IOException
    {
        if(mIsoDep != null)
        {
            mIsoDep.connect();
        }
    }

    @Override
    public void disConnect() {

        mIsoDep = null;
    }

    @Override
    public void setTimeOut(int timeout) {

        if(mIsoDep != null)
        {
            mIsoDep.setTimeout(timeout);
        }
    }

    @Override
    public byte[] transceive(byte[] data) throws IOException {

        if(mIsoDep != null)
        {
            return mIsoDep.transceive(data);
        }

        return null;
    }

    @Override
    public boolean checkGate() {
        synchronized (mtx) {
            try {
                mtx.wait(); //lock
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    @Override
    public void openGate() {
        synchronized (mtx){
            mtx.notifyAll(); //unlock
        }

    }
}
