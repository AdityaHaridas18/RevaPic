package com.cranopy.revapic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import android.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.os.Handler;
import android.widget.ProgressBar;
import com.airbnb.lottie.LottieAnimationView;
import org.json.JSONException;
import org.json.JSONObject;

public class LoadingActivity extends AppCompatActivity {

    private OkHttpClient client;
    private TextView loadingTextView;
    private ProgressBar progressBar;
    private LottieAnimationView loadingAnimation;
    private Handler handler;
    private int currentProgress = 0;
    private String[] loadingStates = {
        "Uploading image...",
        "Extracting image...",
        "Restoring image...",
        "Enhancing image...",
        "Loading image...",
        "Fetching image..."
    };
    private int currentState = 0;

    private static final int MAX_IMAGE_SIZE = 1024; // Max dimension in pixels for resizing
    private static final int JPEG_COMPRESSION_QUALITY = 80; // JPEG quality (0-100)
    private static final float CONTRAST_VALUE = 1.2f; // Increase contrast by 20%
    private static final float SATURATION_VALUE = 1.2f; // Increase saturation by 20%

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        loadingTextView = findViewById(R.id.loadingText);
        progressBar = findViewById(R.id.progressBar);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        handler = new Handler();

        // Set up progress bar
        progressBar.setMax(100);
        startLoadingAnimation();
        startProgressUpdates();

        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        String imageUriString = getIntent().getStringExtra("image_uri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            try {
                String base64Image = encodeImageToBase64(imageUri);
                if (base64Image != null) {
                    enhanceImage(base64Image);
                } else {
                    loadingTextView.setText("Error: Failed to encode image.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                loadingTextView.setText("Error reading image: " + e.getMessage());
            }
        } else {
            loadingTextView.setText("Error: No image URI received.");
        }
    }

    private String encodeImageToBase64(Uri imageUri) throws IOException {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }

            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap == null) {
                return null;
            }

            // Apply contrast and saturation
            Bitmap adjustedBitmap = adjustContrastAndSaturation(originalBitmap, CONTRAST_VALUE, SATURATION_VALUE);

            int width = adjustedBitmap.getWidth();
            int height = adjustedBitmap.getHeight();

            float bitmapRatio = (float) width / (float) height;
            if (bitmapRatio > 1) { // Landscape
                if (width > MAX_IMAGE_SIZE) {
                    width = MAX_IMAGE_SIZE;
                    height = (int) (width / bitmapRatio);
                }
            } else { // Portrait or Square
                if (height > MAX_IMAGE_SIZE) {
                    height = MAX_IMAGE_SIZE;
                    width = (int) (height * bitmapRatio);
                }
            }

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(adjustedBitmap, width, height, true);

            outputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, outputStream);

            byte[] bytes = outputStream.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    private Bitmap adjustContrastAndSaturation(Bitmap originalBitmap, float contrast, float saturation) {
        Bitmap finalBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), originalBitmap.getConfig());
        Canvas canvas = new Canvas(finalBitmap);
        Paint paint = new Paint();

        // Apply saturation
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(saturation);

        // Apply contrast
        ColorMatrix contrastMatrix = new ColorMatrix();
        float scale = contrast;
        float translate = (-0.5f * scale + 0.5f) * 255;
        contrastMatrix.set(new float[] {
                scale, 0, 0, 0, translate,
                0, scale, 0, 0, translate,
                0, 0, scale, 0, translate,
                0, 0, 0, 1, 0
        });

        ColorMatrix combinedMatrix = new ColorMatrix();
        combinedMatrix.postConcat(saturationMatrix);
        combinedMatrix.postConcat(contrastMatrix);

        paint.setColorFilter(new ColorMatrixColorFilter(combinedMatrix));
        canvas.drawBitmap(originalBitmap, 0, 0, paint);

        return finalBitmap;
    }

    private void adjustContrastAndSaturation(Bitmap bitmap) {
        // Create a new bitmap with the same dimensions
        Bitmap adjustedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(adjustedBitmap);

        // Create a Paint object with ColorMatrix
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();

        // Adjust contrast (1.0 is normal, 1.1 is very slightly increased)
        float contrast = 1.1f;
        float scale = contrast + 1.f;
        float translate = (-.5f * scale + .5f) * 255.f;
        float[] array = new float[] {
            scale, 0, 0, 0, translate,
            0, scale, 0, 0, translate,
            0, 0, scale, 0, translate,
            0, 0, 0, 1, 0
        };
        colorMatrix.set(array);

        // Adjust saturation (1.0 is normal, 1.05 is very slightly increased)
        float saturation = 1.05f;
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(saturation);
        colorMatrix.postConcat(saturationMatrix);

        // Apply the color matrix
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        // Replace the original bitmap with the adjusted one
        bitmap.recycle();
        bitmap = adjustedBitmap;
    }

    private void enhanceImage(String base64Image) {
        String json = "{\"model\":\"7de2ea26c616d5bf2245ad0d5e24f0ff9a6204578a5c876db53142edd9d2cd56\",\"image\":\"data:image/jpeg;base64," + base64Image + "\"}";
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // OPTIONS request
        Request optionsRequest = new Request.Builder()
                .url("https://us-central1-ai-apps-prod.cloudfunctions.net/restorePhoto")
                .header("Accept", "*/*")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .header("Origin", "https://www.photorestore.io")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "cross-site")
                .header("Sec-Fetch-Dest", "empty")
                .header("Referer", "https://www.photorestore.io/")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Priority", "u=1, i")
                .method("OPTIONS", RequestBody.create(new byte[0], null))
                .build();

        client.newCall(optionsRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (loadingTextView != null) {
                        loadingTextView.setText("Error with OPTIONS request: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        if (loadingTextView != null) {
                            loadingTextView.setText("OPTIONS request failed: " + response.code());
                        }
                    });
                } else {
                    // POST request after successful OPTIONS
                    RequestBody body = RequestBody.create(json, JSON);
                    Request postRequest = new Request.Builder()
                            .url("https://us-central1-ai-apps-prod.cloudfunctions.net/restorePhoto")
                            .header("Host", "us-central1-ai-apps-prod.cloudfunctions.net")
                            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                            .header("Content-Type", "application/json")
                            .header("Accept", "*/*")
                            .header("Origin", "https://www.photorestore.io")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                            .header("Sec-Fetch-Mode", "cors")
                            .header("Sec-Fetch-Site", "cross-site")
                            .header("Sec-Fetch-Dest", "empty")
                            .header("Referer", "https://www.photorestore.io/")
                            .header("Accept-Encoding", "gzip, deflate, br")
                            .header("Accept-Language", "en-US,en;q=0.9")
                            .header("Priority", "u=1, i")
                            .post(body)
                            .build();

                    client.newCall(postRequest).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                if (loadingTextView != null) {
                                    loadingTextView.setText("POST request failed: " + e.getMessage());
                                }
                                new Handler().postDelayed(LoadingActivity.this::finish, 2000); // Finish after 2 seconds
                            });
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseBody = response.body().string();
                                try {
                                    JSONObject jsonObject = new JSONObject(responseBody);
                                    String enhancedImageUrl = jsonObject.getString("restored_image"); // Assuming the key is 'restored_image'

                                    runOnUiThread(() -> {
                                        Intent intent = new Intent(LoadingActivity.this, ResultActivity.class);
                                        intent.putExtra("original_image_uri", getIntent().getStringExtra("image_uri"));
                                        intent.putExtra("enhanced_image_url", enhancedImageUrl);
                                        startActivity(intent);
                                        finish(); // Finish LoadingActivity after starting ResultActivity
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    runOnUiThread(() -> {
                                        if (loadingTextView != null) {
                                            loadingTextView.setText("Error parsing API response: " + e.getMessage());
                                        }
                                        new Handler().postDelayed(LoadingActivity.this::finish, 2000); // Finish after 2 seconds
                                    });
                                }
                            } else {
                                runOnUiThread(() -> {
                                    if (loadingTextView != null) {
                                        loadingTextView.setText("POST request failed: " + response.code() + " " + response.message());
                                    }
                                    new Handler().postDelayed(LoadingActivity.this::finish, 2000); // Finish after 2 seconds
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void startLoadingAnimation() {
        loadingAnimation.setAnimation("loading_animation.json");
        loadingAnimation.playAnimation();
    }

    private void startProgressUpdates() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentProgress < 100) {
                    currentProgress += 2;
                    progressBar.setProgress(currentProgress);
                    
                    // Update loading text based on progress
                    if (currentProgress % 20 == 0 && currentState < loadingStates.length) {
                        loadingTextView.setText(loadingStates[currentState]);
                        currentState++;
                    }
                    
                    handler.postDelayed(this, 100);
                }
            }
        }, 100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
} 