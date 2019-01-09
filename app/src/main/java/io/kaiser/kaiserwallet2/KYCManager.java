package io.kaiser.kaiserwallet2;


import android.content.Context;
import android.telephony.TelephonyManager;

import com.google.firebase.iid.FirebaseInstanceId;
import com.journeyapps.barcodescanner.Util;

public class KYCManager {

    private Context mContext;
    private static KYCManager uniqueInstance = null;
    private KYCInfo mKYCInfo = null;

    private  KYCManager(Context context){

        mContext = context;

        mKYCInfo = new KYCInfo();

        //PhoneNumber
        setPhoneNumber(Utils.getPhoneNumber(context));

        //IMEI
        setIMEI(Utils.getIMEI(context));

        //FCMToken
        String token = FirebaseInstanceId.getInstance().getToken();
        setFCMToken(token);
    }

    public static KYCManager getInstance(Context context){
        if (uniqueInstance == null){

            synchronized (KYCManager.class){
                if (uniqueInstance == null){
                    uniqueInstance = new KYCManager(context);
                }
            }
        }
        return uniqueInstance;
    }


    public void setIMEI(String imei){
        mKYCInfo.strIMEI = imei;
    }
    public void setPhoneNumber(String phoneNumber){
        mKYCInfo.strPhoneNumber = phoneNumber;
    }
    public void setFCMToken(String fcmToken){
        mKYCInfo.strFCMToken = fcmToken;
    }
    public void setCardTSN(String cardTSN){
        mKYCInfo.strCardTSN = cardTSN;
    }
    public void setKaiserWalletAddress(String address) { mKYCInfo.strKaiserWalletAddress = address; }


    public KYCInfo getKYCInfo(){
        return mKYCInfo;
    }


}
