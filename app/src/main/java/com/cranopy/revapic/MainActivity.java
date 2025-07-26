package com.cranopy.revapic;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import android.util.Base64;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import androidx.annotation.Nullable;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int PICK_IMAGE = 1;
    private GridView galleryGridView;
    private ImageAdapter imageAdapter;
    private ArrayList<String> mediaPaths;
    private Button photosButton;
    private Button videosButton;
    private ImageView galleryButton;
    private ImageView settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        galleryGridView = findViewById(R.id.galleryGridView);
        photosButton = findViewById(R.id.photosButton);
        videosButton = findViewById(R.id.videosButton);
        galleryButton = findViewById(R.id.galleryButton);
        settingsButton = findViewById(R.id.settingsButton);

        mediaPaths = new ArrayList<>();
        imageAdapter = new ImageAdapter(this, mediaPaths);
        galleryGridView.setAdapter(imageAdapter);

        galleryGridView.setOnItemClickListener((parent, view, position, id) -> {
            showImageDialog(mediaPaths.get(position));
        });

        photosButton.setOnClickListener(v -> {
            loadImages();
        });

        videosButton.setOnClickListener(v -> {
            loadVideos();
        });

        galleryButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        checkPermissions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Intent intent = new Intent(this, LoadingActivity.class);
                intent.putExtra("image_uri", selectedImageUri.toString());
                startActivity(intent);
            }
        }
    }

    private void showImageDialog(String imagePath) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_image_preview);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setDimAmount(0.8f);

        ImageView closeButton = dialog.findViewById(R.id.closeButton);
        ShapeableImageView dialogImageView = dialog.findViewById(R.id.dialogImageView);
        Button enhanceButton = dialog.findViewById(R.id.enhanceButton);
        Button removeAdsButton = dialog.findViewById(R.id.removeAdsButton);

        Glide.with(this)
                .load(imagePath)
                .into(dialogImageView);

        closeButton.setOnClickListener(v -> dialog.dismiss());

        enhanceButton.setOnClickListener(v -> {
            String imageUriString = saveImageToCacheAndGetUri(imagePath);
            if (imageUriString != null) {
                Intent intent = new Intent(MainActivity.this, LoadingActivity.class);
                intent.putExtra("image_uri", imageUriString);
                startActivity(intent);
                dialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Failed to prepare image for enhancement.", Toast.LENGTH_SHORT).show();
            }
        });

        removeAdsButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Remove Ads & Limits clicked!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private String saveImageToCacheAndGetUri(String imagePath) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                return null;
            }

            File cacheDir = getCacheDir();
            File tempFile = new File(cacheDir, "temp_image_" + System.currentTimeMillis() + ".jpeg"); // Use a unique name

            fis = new FileInputStream(imageFile);
            fos = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            Uri contentUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    tempFile);
            return contentUri.toString();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving image to cache", Toast.LENGTH_SHORT).show();
            return null;
        } finally {
            try {
                if (fis != null) fis.close();
                if (fos != null) fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            } else {
                loadImages();
            }
        } else {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                loadImages();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImages();
            } else {
                Toast.makeText(this, "Permission denied. Cannot display images.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadImages() {
        mediaPaths.clear();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");

        if (cursor != null) {
            int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            while (cursor.moveToNext()) {
                mediaPaths.add(cursor.getString(column_index_data));
            }
            cursor.close();
            imageAdapter.notifyDataSetChanged();
        }
    }

    private void loadVideos() {
        mediaPaths.clear();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Video.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Video.Media.DATE_ADDED + " DESC");

        if (cursor != null) {
            int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            while (cursor.moveToNext()) {
                mediaPaths.add(cursor.getString(column_index_data));
            }
            cursor.close();
            imageAdapter.notifyDataSetChanged();
        }
    }
}