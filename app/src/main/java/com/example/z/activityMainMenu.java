package com.example.z;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.MenuItem;

import java.util.EnumMap;
import java.util.Map;

public class activityMainMenu extends Activity {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 100;
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    private FrameLayout preview;
    private RelativeLayout shtrihLayout,layoutScan;
    private LinearLayout layoutGenerate;
    private TextView tvShtrih;
    private ImageView ivShtrih;
    private ImageScanner scanner;
    private boolean barcodeScanned = false;
    private boolean previewing = true;
    private String lastScannedCode;
    private Image codeImage;
    private Button repeat,contin,share,genereate,shareBarCode;
    private EditText editTextBarCode;


    static {
        System.loadLibrary("iconv");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        autoFocusHandler = new Handler();
        repeat = (Button) findViewById(R.id.repeat);
        contin = (Button) findViewById(R.id.contin);
        share = (Button) findViewById(R.id.share);
        genereate = (Button) findViewById(R.id.generateNewBarCode);
        shareBarCode = (Button) findViewById(R.id.shareBarCode);
        preview = (FrameLayout) findViewById(R.id.cameraPreview);
        shtrihLayout = (RelativeLayout) findViewById(R.id.shtrih_layout_id);
        layoutGenerate = (LinearLayout) findViewById(R.id.layoutGenerate);
        layoutScan = (RelativeLayout) findViewById(R.id.layoutScan);
        tvShtrih = (TextView) findViewById(R.id.tvShtrihCode);
        ivShtrih = (ImageView) findViewById(R.id.ivShtrihCode);
        editTextBarCode = (EditText) findViewById(R.id.editTextBarCode);
        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);
        shareBarCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        genereate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateCodeImage(editTextBarCode.getText().toString());
            }
        });
        contin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(activityMainMenu.this, WebPage.class);
                intent.putExtra("link", tvShtrih.getText().toString());
                startActivity(intent);
            }
        });
        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shtrihLayout.setVisibility(View.INVISIBLE);
                barcodeScanned = false;
            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody = tvShtrih.getText().toString();
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(sharingIntent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeCamera();
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {
        Camera c = null;
        if (android.os.Build.VERSION.SDK_INT >= 23) {

            // Check if we have read/write permission
            // Kiểm tra quyền đọc/ghi dữ liệu vào thiết bị lưu trữ ngoài.
            int camer = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA);

            if (camer != PackageManager.PERMISSION_GRANTED) {
                // If don't have permission so prompt the user.
                this.requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_ID_READ_WRITE_PERMISSION);

            } else {
                c = Camera.open(0);
            }
        } else {
            c = Camera.open(0);
        }
       return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.cancelAutoFocus();
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void resumeCamera() {
        mCamera = getCameraInstance();
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        preview.removeAllViews();
        preview.addView(mPreview);
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            codeImage = new Image(size.width, size.height, "Y800");
            previewing = true;
            mPreview.refreshDrawableState();
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing && mCamera != null) {
                mCamera.autoFocus(autoFocusCB);
            }
        }
    };

    final Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            codeImage.setData(data);
            int result = scanner.scanImage(codeImage);
            if (result != 0) {
                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    lastScannedCode = sym.getData();
                    if ((lastScannedCode != null) & (!barcodeScanned)) {
                        shtrihLayout.setVisibility(View.VISIBLE);
                        tvShtrih.setText(lastScannedCode);
                        barcodeScanned = true;
                    }
                }
            }
            camera.addCallbackBuffer(data);
        }
    };

    // Mimic continuous auto-focusing
    final Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 5000);
        }
    };

    private void generateCodeImage(String text) {
        try {
            Bitmap bitmap = encodeAsBitmap(text, BarcodeFormat.QR_CODE, 150, 150);
            ivShtrih.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate bitmap image with QR or BAR code.
     *
     * @param code       encoded text
     * @param format     code standard
     * @param img_width  target image width
     * @param img_height target image height
     * @return Bitmap image with code
     * @throws WriterException exception
     */



    private Bitmap encodeAsBitmap(String code, BarcodeFormat format, int img_width, int img_height) throws WriterException {
        if (code == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        String encoding = guessAppropriateEncoding(code);
        if (encoding != null) {
            hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
            result = writer.encode(code, format, img_width, img_height, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //
        switch (requestCode) {
            case REQUEST_ID_READ_WRITE_PERMISSION: {

                // Note: If request is cancelled, the result arrays are empty.
                // Permissions granted (read/write).
                Camera c;
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    c = Camera.open(0);
                    //Toast.makeText(this, "Permission granted!", Toast.LENGTH_LONG).show();
                }
                // Cancelled or denied.
                else {
                    //Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;

    {
        mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_close:
                        finish();
                        return true;
                    case R.id.navigation_scan_barcode:
                        layoutGenerate.setVisibility(View.GONE);
                        layoutScan.setVisibility(View.VISIBLE);
                        return true;
                    case R.id.navigation_generate:
                        layoutGenerate.setVisibility(View.VISIBLE);
                        layoutScan.setVisibility(View.GONE);
                        return true;
                }
                return false;
            }
        };
    }
}



