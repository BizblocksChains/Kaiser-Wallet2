package io.kaiser.kaiserwallet2.Device;

public class DeviceContext {

    public interface OnDeviceContext {

        boolean callProcess();
    }

    public INfcTag mNfcTag;
    public OnDeviceContext mOnDeviceContext;

}
