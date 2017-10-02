package com.destiny.sta.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.destiny.sta.R;
import com.destiny.sta.ui.activity.CameraActivity;

import java.io.File;

/**
 * Created by Bobo on 8/29/2017.
 */

public class PictureFragment extends Fragment {

    public static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final int REQUEST_PERMISSIONS = 0;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public interface ImageCallback {
        void onImageCaptured(String path);
    }

    private ImageCallback imageCallback;

    private String imagePath = null;

    private ImageButton imageButton;
    private ImageView imageView;
    private Button submitButton;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ImageCallback) {
            imageCallback = (ImageCallback) context;
        } else {
            throw new IllegalArgumentException("Activity must implement ImageCallback!");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.take_picture);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_picture, container, false);

        imageButton = view.findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermissions();
            }
        });

        imageView = view.findViewById(R.id.imageView);

        submitButton = view.findViewById(R.id.submitButton);
        submitButton.setEnabled(false);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageCallback.onImageCaptured(imagePath);
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                String imagePath = data.getStringExtra("image_path");
                this.imagePath = imagePath;

                File image = new File(imagePath);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap imageBitmap = BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);
                //imageBitmap = Bitmap.createScaledBitmap(imageBitmap, parent.getWidth(), parent.getHeight(), true);

                imageView.setVisibility(View.VISIBLE);
                imageView.setImageBitmap(imageBitmap);
                imageButton.setVisibility(View.GONE);
                submitButton.setEnabled(true);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_picture, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                deletePicture();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void deletePicture() {
        if (imagePath != null) {
            imageView.setVisibility(View.GONE);
            imageView.setImageBitmap(null);
            imageButton.setVisibility(View.VISIBLE);
            submitButton.setEnabled(false);

            imagePath = null;
        }
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(PERMISSIONS[0]) &&
                    shouldShowRequestPermissionRationale(PERMISSIONS[1])) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            } else {
                requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            }
        } else {
            startCameraActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    startCameraActivity();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }
            default:
                break;
        }
    }

    private void startCameraActivity() {
        Intent intent = new Intent(getActivity(), CameraActivity.class);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

}
