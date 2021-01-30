package com.cardrenewer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView imageView;
    TextView text;
    Button first_network, second_network;

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;

    String[] camera_permission;
    String[] carrierNames;
    String renewNumber;


    Uri image_uri;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        imageView = findViewById(R.id.image_view);
        text = findViewById(R.id.text);
        first_network = findViewById(R.id.first_network);
        second_network = findViewById(R.id.second_network);
        first_network.setOnClickListener(this);
        second_network.setOnClickListener(this);

        carrierNames = new String[2];


        //camera permission
        camera_permission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.SEND_SMS};

        //check camera permission
        if (!checkCameraPermission()) {
            //request camera permission and storage for higher resolution picture
            requestCameraPermission();
        } else {
            openCamera();
        }
    }


    private void openCamera() {
        //intent to open camera to take picture and save to storage to get high resolution
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image to renew");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);

    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, camera_permission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        boolean result2 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == (PackageManager.PERMISSION_GRANTED);
        boolean result3 = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == (PackageManager.PERMISSION_GRANTED);
        return result && result1 && result2 && result3;
    }

    //handle permission result
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean phoneSteteAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                boolean sendSmsAccepted = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                if (cameraAccepted && writeStorageAccepted && phoneSteteAccepted && sendSmsAccepted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                CropImage.activity(image_uri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
        }
        //get cropped image
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                Uri resultUri = result.getUri();
                imageView.setImageURI(resultUri);

                //get drawable bitmap for text recognition
                BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();

                TextRecognizer recognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                if (!recognizer.isOperational()) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                } else {
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<TextBlock> items = recognizer.detect(frame);
                    StringBuilder sb = new StringBuilder();
                    //get text from sb until no text
                    for (int i = 0; i < items.size(); i++) {
                        TextBlock myitem = items.valueAt(i);
                        sb.append(myitem.getValue());
                        sb.append("\n");
                    }
                    //set text to text view
                    text.setText(sb.toString());
                    StringBuilder final_sb = new StringBuilder();
                    for (int i = 0; i < sb.length(); i++) {
                        if (sb.charAt(i) != ' ') {
                            char c = sb.charAt(i);
                            final_sb.append(c);

                        }
                    }
                    renewNumber = final_sb.toString();
                    carrierNames = getNetworkOperator(this);
                    if (carrierNames[0] != null) {
                        first_network.setText(carrierNames[0]);
                        first_network.setVisibility(View.VISIBLE);
                    } else {
                        first_network.setVisibility(View.INVISIBLE);
                    }
                    if (carrierNames[1] != null) {
                        second_network.setText(carrierNames[1]);
                        second_network.setVisibility(View.VISIBLE);
                    } else {
                        second_network.setVisibility(View.INVISIBLE);
                    }


                }
            }

        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Exception error = result.getError();
            Toast.makeText(this, "Error to crop " + error, Toast.LENGTH_SHORT).show();
        }
    }

    //sim providers info
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    public String[] getNetworkOperator(final Context context) {
        // Get System TELEPHONY service reference
        String[] carrierNames = new String[2];
        try {
            final String permission = android.Manifest.permission.READ_PHONE_STATE;
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) && (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)) {
                final List<SubscriptionInfo> subscriptionInfos = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
                for (int i = 0; i < subscriptionInfos.size(); i++) {
                    carrierNames[i] = (subscriptionInfos.get(i).getCarrierName().toString());
                }

            } else {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                // Get carrier name (Network Operator Name)
                carrierNames[0] = (telephonyManager.getNetworkOperatorName());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return carrierNames;
    }

    //get clicked button and send the sms
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onClick(View v) {
        String networkProvider="";
        Pattern p = Pattern.compile("\\d{1,16}");
        Matcher m = p.matcher(renewNumber);
        if (m.lookingAt()) {
            SmsManager smsManager = null;
            switch (v.getId()) {
                case R.id.first_network:
                    networkProvider = first_network.getText().toString();
                    smsManager = SmsManager.getSmsManagerForSubscriptionId(0);
                    break;
                case R.id.second_network:
                    networkProvider = second_network.getText().toString();
                    smsManager = SmsManager.getSmsManagerForSubscriptionId(1);
                    break;
                default:
                    Toast.makeText(this, "Unable to find service provider", Toast.LENGTH_SHORT).show();
            }

            String number;
            //sms manager
            Toast.makeText(this,networkProvider,Toast.LENGTH_SHORT).show();
            if (networkProvider.equals("GR COSMOTE")) {
                //number = "1314";
                number = "1314";
                smsManager.sendTextMessage(number, null, "ΑΝΑ " + renewNumber, null, null);
                Toast.makeText(this, "Επιτυχής", Toast.LENGTH_SHORT).show();
            } else if (networkProvider.equals("VODAFONEGREECE2021")) {
                //number = "1252";
                number = "1252";
                smsManager.sendTextMessage(number, null, "Α " + renewNumber, null, null);
                Toast.makeText(this, "Επιτυχής", Toast.LENGTH_SHORT).show();
            } else if (networkProvider.equals("WIND")) {
                number = "1268";
                smsManager.sendTextMessage(number, null, renewNumber, null, null);
                Toast.makeText(this, "Επιτυχής", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "provider number not included", Toast.LENGTH_SHORT).show();
            }

            exit();
        } else {
            Toast.makeText(this, "Μη έγκυρο στιγμιότυπο", Toast.LENGTH_SHORT).show();
            exit();
        }
    }

    public void exit() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                System.exit(0);
            }
        }, 2000);

    }

}