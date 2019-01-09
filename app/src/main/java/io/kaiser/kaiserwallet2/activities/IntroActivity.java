package io.kaiser.kaiserwallet2.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import io.kaiser.kaiserwallet2.MainActivity;
import io.kaiser.kaiserwallet2.R;
import io.kaiser.kaiserwallet2.Utils;
import io.kaiser.kaiserwallet2.databases.FireRemoteConfigSingletone;
import io.kaiser.kaiserwallet2.databases.IFireRemoteConfigResult;
import io.kaiser.kaiserwallet2.databinding.ActivityIntroBinding;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static io.kaiser.kaiserwallet2.BuildConfig.BUILD_DEBUG;

public class IntroActivity extends AppCompatActivity {

    private final String INTRO_IMAGE_IDX_KEY = "IntroImageIdx";
    private final float TIME_DEBUG = 0.3f;
    private final float TIME_RELEASE = 1.3f;
    private final long DELAY_TIME = (long)((BUILD_DEBUG ? TIME_DEBUG : TIME_RELEASE) * 1000);

    private ActivityIntroBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_intro);

        //Load
        String strIntroImageIdx = Utils.getSetttingInfo(this, INTRO_IMAGE_IDX_KEY);
        int intIntroImageIdx = 0;
        if(strIntroImageIdx!=null && strIntroImageIdx.length()!=0)
        {
            intIntroImageIdx = Integer.parseInt(strIntroImageIdx);
        }

        //Save
        Utils.setSetttingInfo(this, INTRO_IMAGE_IDX_KEY, ""+((++intIntroImageIdx)%2));


        //ImageResourceId
        int resDrawableId = Utils.getDrawableResId(this, "model_0"+(intIntroImageIdx));

        //SetImage
        binding.ivModel.setImageResource(resDrawableId);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FireRemoteConfigSingletone.getFirstInstance(this, new IFireRemoteConfigResult() {
            @Override
            public void onFetch(boolean success) {

                goMain();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //뒤로가기 방지
            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void goMain()
    {
        //Go Main
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        }, DELAY_TIME);
    }

}
