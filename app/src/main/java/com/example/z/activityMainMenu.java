package com.example.z;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
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
import android.widget.Toast;

import org.apache.commons.validator.routines.UrlValidator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.EnumMap;
import java.util.Map;

public class activityMainMenu extends Activity {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 100;
    private static final int SELECT_PHOTO = 10;
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
    private Button repeat,contin,share,genereate,shareBarCode,copy,scanFromGallery;
    private EditText editTextBarCode;


    static {
        System.loadLibrary("iconv");
    }

    private Uri URIBarcode;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        isStoragePermissionGranted();

        autoFocusHandler = new Handler();
        repeat = (Button) findViewById(R.id.repeat);
        contin = (Button) findViewById(R.id.contin);
        share = (Button) findViewById(R.id.share);
        genereate = (Button) findViewById(R.id.generateNewBarCode);
        shareBarCode = (Button) findViewById(R.id.shareBarCode);
        copy = (Button) findViewById(R.id.copy);
        scanFromGallery = (Button) findViewById(R.id.button);

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

        scanFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 10);

            }
        });

        shareBarCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, URIBarcode);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/*");
                startActivity(intent);
            }
        });
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final android.content.ClipboardManager clipboardManager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("Source Text", tvShtrih.getText());
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(),"Barcode copied to clipboard",Toast.LENGTH_LONG).show();
            }
        });
        genereate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    generateCodeImage(editTextBarCode.getText().toString(),"barcode.jpg");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        contin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String barCode = tvShtrih.getText().toString();
                String[] schemes = {"http","https"}; // DEFAULT schemes = "http", "https", "ftp"
                UrlValidator urlValidator = new UrlValidator(schemes);
                if (urlValidator.isValid(barCode)) {
                    Intent intent = new Intent(activityMainMenu.this, WebPage.class);
                    intent.putExtra("link", barCode);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(),"Sorry, this is dont link.",Toast.LENGTH_LONG).show();
                }
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

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //Log.v(TAG,"Permission is granted");
                return true;
            } else {

                //Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            //Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    private void generateCodeImage(String text, String filename) throws IOException {

        //if(isStoragePermissionGranted()) {
            Bitmap bitmap = null;
            try {
                bitmap = encodeAsBitmap(text, BarcodeFormat.QR_CODE, 150, 150);
                ivShtrih.setImageBitmap(bitmap);
            } catch (WriterException e) {
                e.printStackTrace();
            }
            // Assume block needs to be inside a Try/Catch block.
            String path = Environment.getExternalStorageDirectory().toString();
            OutputStream fOut = null;
            Integer counter = 0;
            File file = new File(path, filename); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
            fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            fOut.flush(); // Not really required
            fOut.close(); // do not forget to close the stream

            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
            URIBarcode = Uri.fromFile(file);
        //}
        //return file.getAbsolutePath();
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

    public void scanBarCodeFromGallery(Bitmap bmp){



        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] pixels = new int[width*height];

        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        Image barcode = new Image(width, height, "RGB4");
        barcode.setData(pixels);

        int result = scanner.scanImage(barcode.convert("Y800"));
        if (result != 0) {
            SymbolSet syms = scanner.getResults();
            for (Symbol sym : syms) {
                lastScannedCode = sym.getData();
                if ((lastScannedCode != null) & (!barcodeScanned)) {
                    shtrihLayout.setVisibility(View.VISIBLE);
                    tvShtrih.setText(lastScannedCode);
                    barcodeScanned = true;
                    //Toast.makeText(getApplicationContext(),lastScannedCode,Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(selectedImage);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);
                    scanBarCodeFromGallery(yourSelectedImage);
                }
        }
    }
}



