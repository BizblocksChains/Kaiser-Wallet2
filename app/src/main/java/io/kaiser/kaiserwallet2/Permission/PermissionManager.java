package io.kaiser.kaiserwallet2.Permission;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import io.kaiser.kaiserwallet2.Util.ToastUtil;

import io.kaiser.kaiserwallet2.BuildConfig;
import io.kaiser.kaiserwallet2.R;

import static android.app.Activity.RESULT_CANCELED;
import static android.content.Context.FINGERPRINT_SERVICE;


/*
    앱의 권한 매니져
    - 추후 작업으로 권한 매니저만 실행하면, 이유를 설명하고 권한을 받아오는 로직을 만들어야 한다.

 */


public class PermissionManager {

    public static final int REQUEST_CODE_PERMISSION = 1; //기본적인 권한을 가져올때 사용됩니다 (기본:지문인식)
    public static final int REQUEST_GALLARY_CODE_PERMISSION = 2; //갤러리에 접근할때 사용됩니다

    private Activity mActivity;

    private String mStrLastPermission;
    private int mIntLastRequestCode;

    private PermissionResultListener mPermissionResultListener;


    public interface PermissionResultListener {
        void granted();
        void denied();
    }

    public PermissionManager(Activity activity)
    {
        mActivity = activity;

    }

    public void checkAppPermission(PermissionResultListener permissionResultListener) {

        mPermissionResultListener = permissionResultListener;

        int currentApiVersion;

        currentApiVersion = Build.VERSION.SDK_INT;

        if (currentApiVersion >= Build.VERSION_CODES.M) {
            if (!isGrantedAppPermission(Manifest.permission.READ_PHONE_STATE)) {
                requestAppPermission(Manifest.permission.READ_PHONE_STATE, REQUEST_CODE_PERMISSION);
                return;
            }
        }

        if(mPermissionResultListener != null)
        {
            mPermissionResultListener.granted();
        }
    }

    //권한이 승인 되었는지 확인
    public boolean isGrantedAppPermission(String strPermission) {
        if (ContextCompat.checkSelfPermission(mActivity, strPermission) == PackageManager.PERMISSION_DENIED) {
            return false;
        }

        return true;
    }

    private void requestAppPermission(String strPermission, int requestCode) {
        mStrLastPermission = strPermission;
        mIntLastRequestCode = requestCode;

        ActivityCompat.requestPermissions(mActivity, new String[]{strPermission}, requestCode);
    }

    //지문이 등록되었는지 확인
    public boolean hasEnrolledFingerprints()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            FingerprintManager fingerprintManager = (FingerprintManager) mActivity.getSystemService(FINGERPRINT_SERVICE);
            return fingerprintManager.hasEnrolledFingerprints();
        }
        else
        {
            return FingerprintManagerCompat.from(mActivity).hasEnrolledFingerprints();
        }
    }

    //지문을 지원하는 기기인지 확인
    public boolean isSupportFingerPrintDevice()
    {
        //개발할땐 지문인증 화면이 출력되지 않습니다.
        if(!BuildConfig.USE_FINGER_PRINT)
        {
            return false;
        }

        return FingerprintManagerCompat.from(mActivity).isHardwareDetected();

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION:
                if ((grantResults.length <= 0) || (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    mActivity.setResult(RESULT_CANCELED);

                    if(mPermissionResultListener!=null) {
                        mPermissionResultListener.denied();
                    }
                } else {
                    if(mPermissionResultListener!=null) {
                        mPermissionResultListener.granted();
                    }
                }
            case REQUEST_GALLARY_CODE_PERMISSION:
                if ((grantResults.length <= 0)
                        || (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    mActivity.setResult(RESULT_CANCELED);
                } else {
                    //..
                }
                break;
        }
    }
}
