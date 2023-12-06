/*
 * The `TagSuggestionActivity` class represents an activity for suggesting tags based on images.
 * It allows users to choose images from the gallery, take pictures using the camera, and receive tag suggestions.
 */

package com.tag.tag.project.tag.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tag.tag.R;
import com.tag.tag.configure.Core;
import com.tag.tag.interfaces.TagSuggestionHandler;
import com.tag.tag.project.camera.view.CameraActivity;
import com.tag.tag.project.ParentActivity;
import com.tag.tag.project.tag.model.TagModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import androidx.annotation.Nullable;
import cz.msebera.android.httpclient.Header;

public class TagSuggestionActivity extends ParentActivity implements TagSuggestionHandler {
    private RelativeLayout rl_choose_from_gallery;
    private RelativeLayout rl_take_a_picture;
    private LinearLayout ll_complete;
    private LinearLayout ll_choose_camera_gallery;
    private ChipGroup cg_tags;
    private FloatingActionButton fab_reload;
    private ProgressBar pb_progressbar;
    private MaterialButton mb_ai;
    private TextView tv_app_description;
    private ImageView iv_preview;
    private static final int REQUEST_CODE_PICK_IMAGE = 20;
    private ClipboardManager clipboardManager;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_tag_suggestion);

            // Set up the views and handle clicks
            setView();
            onClick();
        } catch (Exception e) {
            // Log any exceptions that may occur during initialization
            Log.i(Core.getInstance().TAG, e.toString());
        }
    }

    @Override
    public void setView() {
        // Initialize views from the layout
        rl_choose_from_gallery = findViewById(R.id.activity_tag_suggestion_rl_choose_from_gallery);
        rl_take_a_picture = findViewById(R.id.activity_tag_suggestion_rl_take_a_picture);
        iv_preview = findViewById(R.id.activity_tag_suggestion_iv_preview);
        ll_choose_camera_gallery = findViewById(R.id.activity_tag_suggestion_ll_choose_camera_gallery);
        ll_complete = findViewById(R.id.activity_tag_suggestion_ll_complete);
        fab_reload = findViewById(R.id.activity_tag_suggestion_fab_reload);
        pb_progressbar = findViewById(R.id.activity_tag_suggestion_pb_progressbar);
        mb_ai = findViewById(R.id.activity_tag_suggestion_mb_ai);
        tv_app_description = findViewById(R.id.activity_tag_suggestion_tv_app_description);
        cg_tags = findViewById(R.id.activity_tag_suggestion_cg_tags);
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    }

    @Override
    public void onClick() {
        // Set click listeners for various views
        rl_choose_from_gallery.setOnClickListener(view1 -> openGallery());
        rl_take_a_picture.setOnClickListener(view1 -> startActivity(new Intent(this, CameraActivity.class)));
        fab_reload.setOnClickListener(view -> {
            // Reset UI when the reload button is clicked
            fab_reload.setVisibility(View.GONE);
            ll_complete.setVisibility(View.GONE);
            ll_choose_camera_gallery.setVisibility(View.VISIBLE);
            iv_preview.setVisibility(View.GONE);
            tv_app_description.setVisibility(View.VISIBLE);
            cg_tags.setVisibility(View.GONE);
            file = null;
        });

        mb_ai.setOnClickListener(view -> {
            // Initiate tag suggestion request
            Core.getInstance().getNetworkHelper().tagSuggestionRequest(this, file);
        });
    }

    private void openGallery() {
        // Open the gallery to choose an image
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            // Handle the result of choosing an image from the gallery
            Uri selectedImageUri = data.getData();
            try {
                // Display the selected image and prepare the file for upload
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                iv_preview.setImageBitmap(bitmap);
                iv_preview.setVisibility(View.VISIBLE);
                ll_complete.setVisibility(View.VISIBLE);
                fab_reload.setVisibility(View.VISIBLE);
                tv_app_description.setVisibility(View.GONE);
                ll_choose_camera_gallery.setVisibility(View.GONE);
                file = new File(Core.mContext.getCacheDir(), "file.png");
                OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, os);
                os.close();
            } catch (IOException e) {
                // Log any exceptions that may occur during image processing
                Log.i(Core.getInstance().TAG, e.toString());
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
                    Toast.makeText(TagSuggestionActivity.this, "Tag Copied!", Toast.LENGTH_SHORT).show();
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
