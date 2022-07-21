package com.example.floralens;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {
    TextView name;
    TextView desc;
    TextView prob;
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        name = findViewById(R.id.name);
        desc = findViewById(R.id.desc);
        desc.setMovementMethod(LinkMovementMethod.getInstance());
        prob = findViewById(R.id.prob);
        image = findViewById(R.id.imageView);

        Bundle extras = getIntent().getExtras();
        String name1 = extras.getString("disease");
        int index = extras.getInt("index");
        float prob1 = extras.getFloat("probability");
        byte[] result = extras.getByteArray("result");
        int rotation = extras.getInt("rotation");

        Bitmap bitmap = BitmapFactory.decodeByteArray(result, 0, result.length);
        Matrix mat = new Matrix();
        mat.setRotate((float)(rotation));

        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
        Bitmap transformed = Bitmap.createBitmap(rotated, 0, 180, 1080, 1080);

        image.setImageBitmap(transformed);
        name.setText(name1);
        prob.setText(Float.toString(prob1));

        if (index == 0) {
            desc.setText(R.string.bacterial_spot);
        }
        if (index == 1) {
            desc.setText(R.string.early_blight);
        }
        if (index == 2) {
            desc.setText(R.string.late_blight);
        }
        if (index == 3) {
            desc.setText(R.string.leaf_mold);
        }
        if (index == 4) {
            desc.setText(R.string.leaf_spot);
        }
        if (index == 5) {
            desc.setText(R.string.spider_mite);
        }
        if (index == 6) {
            desc.setText(R.string.target_spot);
        }
        if (index == 7) {
            desc.setText(R.string.yellow_leaf);
        }
        if (index == 8) {
            desc.setText(R.string.mosaic_virus);
        }
        if (index == 9) {
            desc.setText(R.string.healthy);
        }

    }

}