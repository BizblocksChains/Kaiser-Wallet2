package io.kaiser.kaiserwallet2.Threads;

import io.kaiser.kaiserwallet2.Device.DeviceContext;

public class KaiserCardThread extends Thread implements Runnable {

    //Thread Variables
    private final long TIME_GAP = 20; //20ms
    private boolean isAlive = true;

    //Device Variables
    private DeviceContext mDeviceContext;

    //Constructor
    public KaiserCardThread(DeviceContext deviceContext)
    {
        mDeviceContext = deviceContext;
    }

    @Override
    public void run() {
        super.run();

        while(isAlive)
        {
            boolean open = false;
            if(mDeviceContext.mNfcTag != null)
            {
                open = mDeviceContext.mNfcTag.checkGate();
            }

            if(open)
            {
                if(mDeviceContext.mOnDeviceContext != null)
                {
                    boolean retCall = mDeviceContext.mOnDeviceContext.callProcess();

                    if(retCall)
                    {
                        isAlive = false;
                        break;
                    }
                }
            }


            //Sleep
            try {
                Thread.sleep(TIME_GAP);
            } catch (InterruptedException e) {
                e.printStackTrace();
                isAlive = false;
            }
        }



    }

    public void stopThread()
    {
        isAlive = false;
    }
}
