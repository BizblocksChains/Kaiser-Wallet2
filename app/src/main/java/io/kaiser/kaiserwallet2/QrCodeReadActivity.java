package io.kaiser.kaiserwallet2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QrCodeReadActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private static final String TAG = QrCodeReadActivity.class.getSimpleName();
    public static final int REQUEST_CODE_PERMISSION_CAMERA = 1;

    public static final int REQEUST_CODE_SCAN_QR_CODE = 21001;
    public static final String KEY_EXTRA_ADDRESS = "key.extra.address";

    private ZXingScannerView mScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);

        checkCameraPermission();
    }

    public static Bitmap getQrCode(String content)
    {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String scanQRImage(Bitmap bMap) {


        String contents = null;

        float aspectRatio = bMap.getWidth() /
                (float) bMap.getHeight();
        int width = 480;
        int height = Math.round(width / aspectRatio);

        bMap = Bitmap.createScaledBitmap(
                bMap, width, height, false);


        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new QRCodeReader();

        try {

            Result result = reader.decode(bitmap);
            contents = result.getText();
        }
        catch (Exception e) {
            Log.e("QrTest", "Error decoding barcode", e);
        }
        return contents;
    }

    private void checkCameraPermission() {
        int currentApiVersion;

        currentApiVersion = Build.VERSION.SDK_INT;

        if (currentApiVersion >= Build.VERSION_CODES.M) {
            if (!isGrantedCameraPermission()) {
                requestCameraPermission();
            }
        }
    }

    private boolean isGrantedCameraPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            return false;
        }

        return true;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_CAMERA:
                if ((grantResults.length <= 0)
                        || (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        Intent intent;

        Log.d(TAG, rawResult.getText());

        intent = new Intent();

        intent.putExtra(KEY_EXTRA_ADDRESS, rawResult.getText());

        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onResume() {
        int currentApiVersion;

        super.onResume();

        currentApiVersion = Build.VERSION.SDK_INT;

        if (currentApiVersion >= Build.VERSION_CODES.M) {
            if (isGrantedCameraPermission()) {
                if (mScannerView == null) {
                    mScannerView = new ZXingScannerView(this);
                    setContentView(mScannerView);
                }

                mScannerView.setResultHandler(this);
                mScannerView.startCamera();

            } else {
                requestCameraPermission();
            }
        } else {
            //version < 6.0
            if (mScannerView == null) {
                mScannerView = new ZXingScannerView(this);
                setContentView(mScannerView);
            }

            mScannerView.setResultHandler(this);
            mScannerView.startCamera();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mScannerView.stopCamera();
    }
}
