package decoster.secretreader;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.icu.util.UniversalTimeScale;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import decoster.secretreader.barcode.BarcodeCaptureActivity;
import pl.tajchert.nammu.Nammu;
import pl.tajchert.nammu.PermissionCallback;

public class MainActivity extends AppCompatActivity {
    private static final int BARCODE_READER_REQUEST_CODE = 1;
    private static final int ACTIVITY_SELECT_IMAGE =2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button scan = (Button) findViewById(R.id.scan);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImageCameraOrGallery();

            }
        });

        Button create = (Button) findViewById(R.id.secret);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createInputDial();
            }
        });
        permission();
        Utilities.createPublicDir();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            Log.d("main", "result ok");
            if (requestCode == BARCODE_READER_REQUEST_CODE) {
                if (resultCode == CommonStatusCodes.SUCCESS) {

                    if (data != null) {
                        final Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                        Log.d("main", "barcodde: " + barcode.displayValue);
                        createPasswordDial(barcode.displayValue);

                    } else {
                        Toast.makeText(getApplicationContext(), R.string.no_barcode_captured, Toast.LENGTH_LONG).show();
                        Log.d("main", "error barcode");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), R.string.scan_error, Toast.LENGTH_LONG).show();
                    Log.d("main", "error scan");
                }

            } else if(requestCode == ACTIVITY_SELECT_IMAGE ) {

                if(resultCode == RESULT_OK) {
                    try {
                        if (data != null) {
                            Uri selectedImage = data.getData();
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                            BarcodeDetector detector = new BarcodeDetector.Builder(getApplicationContext()).setBarcodeFormats(Barcode.DATA_MATRIX | Barcode.QR_CODE).build();
                            if (!detector.isOperational()) {
                                Toast.makeText(this, "Could not set up the detector!", Toast.LENGTH_SHORT);
                                return;
                            }
                            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                            SparseArray<Barcode> barcodes = detector.detect(frame);
                            Barcode thisCode = barcodes.valueAt(0);
                            createPasswordDial(thisCode.displayValue);
                        } else {
                            Toast.makeText(getApplicationContext(), "no file selected", Toast.LENGTH_LONG).show();

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.d("main", "result not ok");
                }

            }
            else super.onActivityResult(requestCode, resultCode, data);
    }

    private void createPasswordDial(final String secret) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Enter password");

        final EditText input = new EditText(this);
        input.setInputType( InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD );
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setView(input);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String m_pw = input.getText().toString();
                createShowSecretDial(secret, m_pw);
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                dialog.dismiss();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createShowSecretDial(final String secret, final String password) {
        String msg = null;

        try {
            msg = Utilities.decrypt(secret, password);

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("The secret:");
        if (msg == null) {

            builder.setMessage("Error while decrypting message");
        }
        else {
            builder.setMessage(msg);
        }


        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
        });
        builder.show();




    }



    private void createInputDial() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View v = inflater.inflate(R.layout.dial_scan_layout, null);
        builder.setTitle("The secret");
        builder.setView(v);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                EditText pw1 = (EditText)v.findViewById(R.id.passwordinput);
                EditText pw2 = (EditText)v.findViewById(R.id.passwordinput2);
                EditText input2= (EditText)v.findViewById(R.id.secretinput);
                String password1 = pw1.getText().toString();
                String password2 = pw2.getText().toString();

                if(password1.equals(password2)) {
                    String secret =input2.getText().toString();
                    createQRCodeToPng(password1, secret);
                }
                else{
                    Toast.makeText(MainActivity.this, "Passwords must be the same", Toast.LENGTH_LONG);
                }

                dialog.dismiss();
            }
        });
        builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                dialog.dismiss();
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createQRCodeToPng(String password, String qrSecret) {
        try {
            String valueToQr = Utilities.encrypt(qrSecret, password);
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(valueToQr, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            saveImageToInternalStorage(bitmap);

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (WriterException e) {
            e.printStackTrace();
        }


    }

    public boolean saveImageToInternalStorage(Bitmap image) {

        try {
            if(Utilities.isExternalStorageWritable()) {
                File dir = Utilities.getPublicAlbumStorageDir("QRImages");
                File newFile = new File(dir, "qr_" + System.currentTimeMillis() + ".png");

                FileOutputStream fos = new FileOutputStream(newFile);
                image.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                Toast.makeText(getApplicationContext(), "image saved under " + newFile.getPath(), Toast.LENGTH_LONG).show();
                MediaScannerConnection.scanFile(this,
                        new String[] { newFile.toString() }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                Log.i("ExternalStorage", "-> uri=" + uri);
                            }
                        });
            }
            else {

            }
            // Use the compress method on the Bitmap object to write image to
             // the OutputStream


            return true;
        } catch (Exception e) {
            Log.e("saveToInternalStorage()", e.getMessage());
            return false;
        }
    }
    private void permission() {

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Nammu.askForPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, new PermissionCallback() {
                @Override
                public void permissionGranted() {
                    //Nothing, this sample saves to Public gallery so it needs permission
                }

                @Override
                public void permissionRefused() {
                    finish();
                }
            });
        }
    }
    public void captureImageCameraOrGallery() {

        final CharSequence[] options = { "Take photo", "Choose from library"};
        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this);

        builder.setTitle("Select");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if (options[which].equals("Take photo")) {
                    try {
                        Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                        startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
                    } catch (ActivityNotFoundException ex) {
                        Log.e("Main", ex.getMessage());

                    }

                } else if (options[which].equals("Choose from library")) {
                    Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, ACTIVITY_SELECT_IMAGE);
                }

            }
        });
        builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

}
