package com.example.floralens;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraXConfig;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.DataType;

import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.nio.MappedByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final int RESPONSE_CODE = 1291;
    private static final int RESPONSE_CODE_2 = 2433;
    ImageView imageView;
    TextView textView;
    MappedByteBuffer tfliteModel;
    ImageProcessor imgproc = new ImageProcessor.Builder()
            .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .add(new NormalizeOp(0, 255)).build();
    Interpreter tflite;
    TensorImage tfimage;
    TensorBuffer output;
    int probabilityTensorIndex = 0;
    TensorProcessor probabilityProcessor;
    byte[] result;
    int rotation;

    private final String[] PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private final int CAMERA_REQUEST_CODE = 1001;

    Button camera;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        camera = findViewById(R.id.button);

        probabilityProcessor = new TensorProcessor.Builder().build();

        //tflite model access
        try {
            tfliteModel = FileUtil.loadMappedFile(getApplicationContext(),"Tomato_Mode.tflite");
            tflite = new Interpreter(tfliteModel);
            System.out.println("Read model successfully!");
            Log.i("tag", "model read");
        }
        catch(Exception e) {
            Log.e("tfliteSupport", "Error reading model", e);
        }

        //defining tflite objects
        int[] probabilityShape = tflite.getOutputTensor(probabilityTensorIndex).shape();
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();
        output = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);


        //camera run, calls intent with camera activity
        camera.setOnClickListener(v -> {
            try
            {
                //Try starting the Drawing activity
                Intent intent = new Intent(MainActivity.this, CameraActivity1.class);
                startActivityForResult(intent, RESPONSE_CODE);
            }
            catch(Exception ex)
            {
                //A problem occurred, check Logcat
                Log.i("tag", "printed stack trace");
                ex.printStackTrace();
            }
        });
    }

    // Processes tensor, makes prediction
    public float[] tfProcess(Bitmap bitmap) {

        tfimage = new TensorImage(DataType.FLOAT32);
        tfimage.load(bitmap);
        tfimage = imgproc.process(tfimage);

        Log.i("tag", "processed image");

        int[] dims = new int[]{1, 224, 224, 3};
        tflite.resizeInput(0, dims);

        tflite.run(tfimage.getBuffer(), output.getBuffer().rewind());
        Log.i("tag", "prediction finished!");

        output = probabilityProcessor.process(output);
        Log.i("tag", "prediction processed");

        return output.getFloatArray();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data.getExtras() == null) {
            System.out.println("bruh");
        }
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESPONSE_CODE) {
            if (resultCode == Activity.RESULT_OK) {

                Log.i("tag", "onActivityResult");
                Bundle bundle = data.getExtras();
                Log.i("tag", "bundle extracted");
                result = bundle.getByteArray("result");
                Log.i("tag", "got byte array");
                rotation = bundle.getInt("rotation");
                Log.i("tag", "got rotation");

                Bitmap bitmap = BitmapFactory.decodeByteArray(result, 0, result.length);
                Matrix mat = new Matrix();
                mat.setRotate((float) (rotation));

                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
                Bitmap transformed = Bitmap.createBitmap(rotated, 0, 180, 1080, 1080);

                float[] array = tfProcess(transformed);

                String[] classes = new String[]{
                        "Bacterial Spot", "Early Blight", "Late Blight", "Leaf Mold", "Septoria Leaf Spot",
                        "Spider Mites (Two-Spotted)", "Target Spot", "Yellow Leaf Curl Virus", "Mosaic Virus",
                        "Healthy"
                };

                // max prob calculation
                float max = 0;
                int maxIndex = 0;
                for (int i = 0; i < array.length; i++) {
                    if (array[i] > max) {
                        max = array[i];
                        maxIndex = i;
                    }
                }

                // intent block for data to be sent to result activity
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra("disease", classes[maxIndex]);
                intent.putExtra("index", maxIndex);
                intent.putExtra("probability", max);
                intent.putExtra("result", result);
                intent.putExtra("rotation", rotation);

                startActivityForResult(intent, RESPONSE_CODE_2);

                Log.i("tag", Integer.toString(maxIndex));
                Log.i("tag", classes[maxIndex]);
                Log.i("tag", Float.toString(max));
            }
        }
    }

    //PERMS
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
}