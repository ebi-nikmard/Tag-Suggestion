/*
 * The `CameraActivity` class represents an activity that allows users to capture images using the device camera.
 * It incorporates the CameraX API for camera functionality and provides the ability to suggest tags for the captured image.
 */

package com.tag.tag.project.camera.view;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tag.tag.R;
import com.tag.tag.configure.Core;
import com.tag.tag.interfaces.TagSuggestionHandler;
import com.tag.tag.project.ParentActivity;
import com.tag.tag.project.tag.model.TagModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import cz.msebera.android.httpclient.Header;

public class CameraActivity extends ParentActivity implements TagSuggestionHandler {
    private ExecutorService cameraExecutor;
    private PreviewView pv_preview;
    private ImageCapture imageCapture;
    private FloatingActionButton fab_capture;
    private FloatingActionButton fab_reload;
    private ImageView iv_captured_image;
    private ToggleButton tb_camera_type;
    private LinearLayout ll_complete;
    private MaterialButton mb_ai;
    private ProgressBar pb_progressbar;
    private ChipGroup cg_tags;
    private int currentCamera = CameraSelector.LENS_FACING_BACK;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private ClipboardManager clipboardManager;
    private File photoFile;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_camera);
            setView();
            onClick();
        } catch (Exception e){
            // Log any exceptions that may occur during initialization
            Log.i(Core.getInstance().TAG, e.toString());
        }
    }

    @Override
    public void setView() {
        // Initialize views from the layout and set up necessary resources
        pv_preview = findViewById(R.id.activity_Camera_pv_preview);
        fab_capture = findViewById(R.id.activity_Camera_fab_capture);
        fab_reload = findViewById(R.id.activity_Camera_fab_reload);
        iv_captured_image = findViewById(R.id.activity_Camera_iv_captured_image);
        tb_camera_type = findViewById(R.id.activity_Camera_tb_camera_type);
        ll_complete = findViewById(R.id.activity_camera_ll_complete);
        mb_ai = findViewById(R.id.activity_camera_mb_ai);
        pb_progressbar = findViewById(R.id.activity_camera_pb_progressbar);
        cg_tags = findViewById(R.id.activity_camera_cg_tags);

        cameraExecutor = Executors.newSingleThreadExecutor();
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    }

    @Override
    public void onClick() {
        // Set up click listeners for various views and initiate camera functionality
        tb_camera_type.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentCamera = isChecked ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            startCamera();
        });

        fab_capture.setOnClickListener(v -> captureImage());

        fab_reload.setOnClickListener(v -> reload());

        // Check and request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        mb_ai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Initiate tag suggestion request using the captured photo file
                Core.getInstance().getNetworkHelper().tagSuggestionRequest(CameraActivity.this, photoFile);
            }
        });
    }

    // Check if all required permissions are granted
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted
                return false;
            }
        }
        // All permissions granted
        return true;
    }

    // Reset UI elements when reloading the camera
    private void reload(){
        cg_tags.setVisibility(View.GONE);
        ll_complete.setVisibility(View.GONE);
        tb_camera_type.setVisibility(View.VISIBLE);
        fab_reload.setVisibility(View.GONE);
        fab_capture.setVisibility(View.VISIBLE);
        iv_captured_image.setVisibility(View.GONE);
        photoFile = null;
    }

    // Display the captured image on the UI with correct orientation
    private void displayCapturedImage(File imageFile) {
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        int rotation = getImageRotation(imageFile.getAbsolutePath());
        iv_captured_image.setImageBitmap(rotateBitmap(bitmap, rotation));
        iv_captured_image.setVisibility(View.VISIBLE);
        fab_capture.setVisibility(View.GONE);
        tb_camera_type.setVisibility(View.GONE);
    }

    // Retrieve the rotation angle of the captured image
    private int getImageRotation(String imagePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // Rotate the bitmap based on the specified angle
    private Bitmap rotateBitmap(Bitmap bitmap, int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    // Start the camera with the selected lens facing
    private void startCamera() {
        ProcessCameraProvider cameraProvider;
        try {
            cameraProvider = ProcessCameraProvider.getInstance(this).get();
        } catch (Exception e) {
            // Log any errors that may occur while getting the camera provider
            Log.e(Core.getInstance().TAG, "Error getting camera provider: " + e.getMessage());
            return;
        }
        bindCameraUseCases(cameraProvider, currentCamera);
    }

    // Bind camera use cases such as preview and image capture
    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider, int lensFacing) {
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();

        imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();

        preview.setSurfaceProvider(pv_preview.getSurfaceProvider());

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    // Create a directory to store captured images
    private String getBatchDirectoryName() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "YourAppDirectoryName");

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                // Log an error message if directory creation fails
                Log.e("CameraX", "Failed to create directory");
            }
        }

        return dir.getAbsolutePath();
    }

    // Generate a unique filename for each captured image
    private String getFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        return "IMG_" + timestamp + ".jpg";
    }

    // Capture an image and save it to the specified file
    private void captureImage() {
        photoFile = new File(getBatchDirectoryName(), getFileName());
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // Update UI when the image is successfully captured
                        fab_reload.setVisibility(View.VISIBLE);
                        fab_capture.setVisibility(View.GONE);
                        ll_complete.setVisibility(View.VISIBLE);
                        displayCapturedImage(photoFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        // Log an error message if image capture fails
                        Log.e(Core.getInstance().TAG, "Photo capture failed: " + exception.getMessage());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        // Shut down the camera executor when the activity is destroyed
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Handle the result of the permission request
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                // Display a message and finish the activity if permissions are not granted
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onStartRequest() {
        // Update UI when the tag suggestion request starts
        mb_ai.setVisibility(View.GONE);
        pb_progressbar.setVisibility(View.VISIBLE);
        fab_reload.setVisibility(View.GONE);
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        // Handle the successful response from the server
        try {
            // Parse the JSON response and extract tags
            Core.getInstance().imageTags.clear();
            JSONObject json = new JSONObject(new String(responseBody, StandardCharsets.UTF_8));
            JSONObject result = json.getJSONObject("result");
            JSONArray tags = result.getJSONArray("tags");

            for (int i = 0; i < tags.length(); i++) {
                // Create TagModel objects and add them to the tag list
                JSONObject tagObject = tags.getJSONObject(i);
                String confidence = tagObject.getString("confidence");
                JSONObject tag = tagObject.getJSONObject("tag");
                String tagName = tag.getString("en");
                Core.getInstance().imageTags.add(new TagModel(confidence, tagName));
            }

            // Create Chip views for each tag and add them to the ChipGroup
            for (int i = 0; i < Core.getInstance().imageTags.size(); i++) {
                Chip chip = new Chip(this);
                chip.setText(Core.getInstance().imageTags.get(i).getTagName());
                chip.setClickable(true);
                int finalI = i;
                chip.setOnClickListener(v -> {
                    // Copy the clicked tag to the clipboard
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("tag", Core.getInstance().imageTags.get(finalI).getTagName()));
                    Toast.makeText(CameraActivity.this, "Tag Copied!", Toast.LENGTH_SHORT).show();
                });

                cg_tags.addView(chip);
            }

        } catch (Exception e) {
            // Log any exceptions that may occur during JSON parsing
            Log.i(Core.getInstance().TAG, Core.mContext.getString(R.string.server_problem));
        }
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        // Log the failure and display a generic error message
        Log.i(Core.getInstance().TAG, Core.mContext.getString(R.string.server_problem));
    }

    @Override
    public void onFinishRequest() {
        // Update UI when the tag suggestion request finishes
        pb_progressbar.setVisibility(View.GONE);
        cg_tags.setVisibility(View.VISIBLE);
        fab_reload.setVisibility(View.VISIBLE);
    }
}
