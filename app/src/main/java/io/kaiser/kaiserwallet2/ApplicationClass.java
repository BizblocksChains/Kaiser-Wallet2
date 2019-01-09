package io.kaiser.kaiserwallet2;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

import io.kaiser.kaiserwallet2.Threads.ReaperThread;

public class ApplicationClass extends Application {

    ReaperThread reaperThread = new ReaperThread();

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            FirebaseInstanceId.getInstance().deleteInstanceId();
        } catch (IOException e) {
            e.printStackTrace();
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                reaperThread.wakeMeUpFromSleep();
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if(level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        {
            reaperThread.makeMeSleep();
        }
    }

}
