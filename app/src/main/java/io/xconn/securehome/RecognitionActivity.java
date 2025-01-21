package io.xconn.securehome;
import android.os.Bundle;
import io.xconn.securehome.utils.ImageCaptureHelper;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;


public class RecognitionActivity extends AppCompatActivity {
    private ImageCaptureHelper imageCaptureHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        ImageView imageView = findViewById(R.id.imageView2);
        imageCaptureHelper = new ImageCaptureHelper(this, imageView);

        findViewById(R.id.gallerycard).setOnClickListener(v -> imageCaptureHelper.openGallery());
        findViewById(R.id.cameracard).setOnClickListener(v -> imageCaptureHelper.checkPermissionsAndOpenCamera());
    }
}