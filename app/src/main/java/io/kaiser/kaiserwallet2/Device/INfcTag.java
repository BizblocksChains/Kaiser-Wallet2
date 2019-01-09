package io.kaiser.kaiserwallet2.Device;

import java.io.IOException;

public interface INfcTag {

    public static final int TIME_OUT = 50;

    public void setCore(Object obj);

    public void connect() throws IOException;

    public void disConnect() throws IOException;

    public void setTimeOut(int timeout);

    public byte[] transceive(byte[] data) throws IOException;

    public boolean checkGate();

    public void openGate();

}
