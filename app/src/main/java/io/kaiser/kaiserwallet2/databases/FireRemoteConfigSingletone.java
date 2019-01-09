package io.kaiser.kaiserwallet2.databases;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import io.kaiser.kaiserwallet2.BuildConfig;
import io.kaiser.kaiserwallet2.R;


public class FireRemoteConfigSingletone {

    private static FireRemoteConfigSingletone instance = null;

    private Activity mActivity = null;
    private FirebaseRemoteConfig mFirebaseRemoteConfig = null;
    private IFireRemoteConfigResult mIFireRemoteConfigResult = null;

    //Singleton Constructor
    public static FireRemoteConfigSingletone getFirstInstance(Activity activity, IFireRemoteConfigResult iFireRemoteConfigResult){
        if (instance == null) {
            synchronized (FireRemoteConfigSingletone.class) {
                if (instance == null) {
                    instance = new FireRemoteConfigSingletone(activity, iFireRemoteConfigResult);
                }

            }
        }
        return instance;
    }

    public static FireRemoteConfigSingletone getInstance()
    {
        return instance;
    }

    //Constructor
    public FireRemoteConfigSingletone(Activity activity, IFireRemoteConfigResult iFireRemoteConfigResult){

        //Activity
        mActivity = activity;

        //Interface
        mIFireRemoteConfigResult = iFireRemoteConfigResult;

        //Delay
        long fetchTime = BuildConfig.DEBUG ? 0 : (1*60*60);//개발중일땐 fetch시간이 없도록 만들기, //1hour in seconds

        //RemoteConfig
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetch(fetchTime)
                .addOnCompleteListener(mActivity, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mFirebaseRemoteConfig.activateFetched();
                            mIFireRemoteConfigResult.onFetch(true);
                        } else {
                            mIFireRemoteConfigResult.onFetch(false);
                        }
                    }

                });
    }

    //Get Value
    public boolean getBooleanForKey(String key)
    {
        return mFirebaseRemoteConfig.getBoolean(key);
    }


}
