package com.cranopy.revapic;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import android.graphics.Rect;
import android.util.Log;
import android.widget.TextView;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.app.Dialog;
import com.airbnb.lottie.LottieAnimationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ResultActivity extends AppCompatActivity {

    private ImageView beforeImageView;
    private ImageView afterImageView;
    private View sliderView;
    private RelativeLayout centerHandle;
    private Button saveButton;
    private RelativeLayout rootLayout;
    private TextView beforeText;
    private TextView afterText;

    private float sliderPosition; // X-coordinate of the slider line
    private static final String TAG = "ResultActivity"; // Tag for logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        beforeImageView = findViewById(R.id.beforeImageView);
        afterImageView = findViewById(R.id.afterImageView);
        sliderView = findViewById(R.id.sliderView);
        centerHandle = findViewById(R.id.centerHandle);
        saveButton = findViewById(R.id.saveButton);
        rootLayout = findViewById(R.id.rootLayout);
        beforeText = findViewById(R.id.beforeText);
        afterText = findViewById(R.id.afterText);

        // Log initial dimensions of key views (might be 0 before layout)
        Log.d(TAG, "onCreate - Initial afterImageView width: " + afterImageView.getWidth() + ", height: " + afterImageView.getHeight());
        Log.d(TAG, "onCreate - Initial centerHandle width: " + centerHandle.getWidth() + ", height: " + centerHandle.getHeight());
        Log.d(TAG, "onCreate - Initial sliderView width: " + sliderView.getWidth() + ", height: " + sliderView.getHeight());
        Log.d(TAG, "onCreate - Initial rootLayout width: " + rootLayout.getWidth() + ", height: " + rootLayout.getHeight());

        // Set scale type to centerCrop for both images
        beforeImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        afterImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        String enhancedImageUrl = getIntent().getStringExtra("enhanced_image_url");
        String originalImageUriString = getIntent().getStringExtra("original_image_uri");

        // Load original image into beforeImageView (left side)
        if (originalImageUriString != null && !originalImageUriString.isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(originalImageUriString))
                    .into(beforeImageView);
        }

        // Load enhanced image into afterImageView (right side)
        if (enhancedImageUrl != null && !enhancedImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(enhancedImageUrl)
                    .into(afterImageView);
        }

        // Set initial slider position after the layout is complete
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // Log dimensions after global layout
                Log.d(TAG, "onGlobalLayout - afterImageView width: " + afterImageView.getWidth() + ", height: " + afterImageView.getHeight());
                Log.d(TAG, "onGlobalLayout - centerHandle width: " + centerHandle.getWidth() + ", height: " + centerHandle.getHeight());
                Log.d(TAG, "onGlobalLayout - sliderView width: " + sliderView.getWidth() + ", height: " + sliderView.getHeight());
                Log.d(TAG, "onGlobalLayout - rootLayout width: " + rootLayout.getWidth() + ", height: " + rootLayout.getHeight());

                // Set initial position to center
                sliderPosition = rootLayout.getWidth() / 2f;
                
                // Position handle in the middle vertically
                centerHandle.setY(rootLayout.getHeight() / 2f - centerHandle.getHeight() / 2f);
                
                updateSliderPosition();
                Log.d(TAG, "Initial slider position after global layout: " + sliderPosition);
            }
        });

        centerHandle.setOnTouchListener(new View.OnTouchListener() {
            private float lastX;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // Get handle and parent widths for clamping (re-fetch each time for accuracy)
                float handleWidth = centerHandle.getWidth(); // Now using float
                float parentWidth = ((View) centerHandle.getParent()).getWidth(); // Now using float

                Log.d(TAG, "onTouch - handleWidth: " + handleWidth + ", parentWidth: " + parentWidth);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getRawX();
                        Log.d(TAG, "ACTION_DOWN - lastX: " + lastX);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float currentRawX = event.getRawX();
                        float deltaX = currentRawX - lastX;
                        lastX = currentRawX;

                        // Calculate potential new sliderPosition (center of the line/handle)
                        float potentialSliderPosition = sliderPosition + deltaX; // Now using float

                        // Clamp sliderPosition to keep the handle fully within bounds
                        // sliderPosition represents the center of the handle/line
                        float minSliderPosition = handleWidth / 2f;
                        float maxSliderPosition = parentWidth - handleWidth / 2f;

                        Log.d(TAG, "ACTION_MOVE - potentialSliderPosition: " + potentialSliderPosition + ", minSliderPosition: " + minSliderPosition + ", maxSliderPosition: " + maxSliderPosition);

                        if (potentialSliderPosition < minSliderPosition) {
                            sliderPosition = minSliderPosition;
                        } else if (potentialSliderPosition > maxSliderPosition) {
                            sliderPosition = maxSliderPosition;
                        } else {
                            sliderPosition = potentialSliderPosition;
                        }

                        updateSliderPosition();
                        Log.d(TAG, "ACTION_MOVE - final sliderPosition: " + sliderPosition);
                        return true;
                    default:
                        return false;
                }
            }
        });

        saveButton.setOnClickListener(v -> {
            saveImage();
        });
    }

    private void updateSliderPosition() {
        // Update slider line position
        sliderView.setX(sliderPosition - sliderView.getWidth() / 2f);
        sliderView.invalidate();
        Log.d(TAG, "SliderView X: " + sliderView.getX());

        // Update handle position using sliderPosition
        centerHandle.setX(sliderPosition - centerHandle.getWidth() / 2f);
        centerHandle.invalidate();
        Log.d(TAG, "CenterHandle X: " + centerHandle.getX());

        // Update text positions to follow the slider
        beforeText.setX(sliderPosition - beforeText.getWidth() - 20); // 20dp padding from slider
        afterText.setX(sliderPosition + 20); // 20dp padding from slider
        beforeText.invalidate();
        afterText.invalidate();

        // Update clipping of afterImageView (Enhanced Image)
        int viewHeight = afterImageView.getHeight();
        // Clip the left part of the afterImageView, so only the portion from sliderPosition to the right edge is visible
        // This reveals the original image on the left as slider moves right
        afterImageView.setClipBounds(new Rect((int)sliderPosition, 0, afterImageView.getWidth(), viewHeight));
        afterImageView.invalidate();
        Log.d(TAG, "AfterImageView clipBounds: " + afterImageView.getClipBounds());
    }

    private void saveImage() {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Get the enhanced image bitmap
        afterImageView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(afterImageView.getDrawingCache());
        afterImageView.setDrawingCacheEnabled(false);

        // Save the image
        String fileName = "RevaPic_" + System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        ContentResolver resolver = getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream out = resolver.openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();

            // Hide progress dialog and show success dialog
            progressDialog.dismiss();
            showSuccessDialog();
        } catch (IOException e) {
            e.printStackTrace();
            progressDialog.dismiss();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccessDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_save_success);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        LottieAnimationView successAnimation = dialog.findViewById(R.id.successAnimation);
        Button selectAnotherButton = dialog.findViewById(R.id.selectAnotherButton);

        successAnimation.setAnimation("success_animation.json");
        successAnimation.playAnimation();

        selectAnotherButton.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }
} 