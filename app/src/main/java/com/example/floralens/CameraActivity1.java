package com.example.floralens;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class
CameraActivity1 extends AppCompatActivity {

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    Camera camera;

    PreviewView previewView;
    ImageView captureImage;
    com.google.android.material.floatingactionbutton.FloatingActionButton capture;
    Intent returnIntent = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera1);

        previewView = findViewById(R.id.previewView);
        captureImage = findViewById(R.id.imageView);
        capture = findViewById(R.id.captureButton);

        startCamera();

    }

    private void startCamera() {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
                Log.i("tag", "camera bound to preview");


            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
                Log.i("tag", "error!");

            }
        }, ContextCompat.getMainExecutor(this));
    }


    @SuppressLint("UnsafeOptInUsageError")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(1080, 1440))
                .setTargetRotation(Surface.ROTATION_0)
                .build();
        Log.i("tag", "preview built");


        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        Log.i("tag", "camera selected");


        ImageCapture imageCapture = new ImageCapture.Builder()
                .setTargetResolution(new Size(1080, 1440))
                .setTargetRotation(Surface.ROTATION_0)
                .build();
        Log.i("tag", "image capture instantialized");

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        @SuppressLint("UnsafeOptInUsageError") UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .build();

        camera = cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);

        capture.setOnClickListener(v -> imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                // return info (image bitmap, rotation factor)
                returnIntent.putExtra("result", imageProxyToBitmap(image));
                Log.i("tag", "result intent 1");
                returnIntent.putExtra("rotation", image.getImageInfo().getRotationDegrees());
                Log.i("tag", "result intent 2");
                setResult(Activity.RESULT_OK, returnIntent);
                Log.i("tag", "picture worked");
                finish();
            }
            @Override
            public void onError(@NonNull ImageCaptureException error) {
                System.out.println("oof");
            }
        }));
    }


    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    // imageCapture output to bitmap converter (return byteArr)
    private byte[] imageProxyToBitmap(ImageProxy proxy) {
        ByteBuffer buffer = proxy.getPlanes()[0].getBuffer();
        buffer.rewind();

        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        byte[] cloned = bytes.clone();

        return cloned;
    }
}