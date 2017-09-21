package com.destiny.sta.ui.activity;

import android.app.Activity;
import android.content.res.Configuration;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.destiny.sta.R;
import com.destiny.sta.view.Preview;

/**
 * Created by Bobo on 8/29/2017.
 */

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private Camera camera;
    private Camera.CameraInfo info;
    private Camera.Parameters parameters;

    private Preview preview;
    private SurfaceHolder holder;

    private int previewWidth = 0;
    private int previewHeight = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        getSupportActionBar().hide();

        preview = (Preview) findViewById(R.id.preview);
        preview.getHolder().addCallback(this);

//        preview = new Preview(this, surfaceView);
//        preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//        preview.setKeepScreenOn(true);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (holder != null) {
            openCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }

    private void openCamera() {
        if (camera == null) {
            int cameraCount = Camera.getNumberOfCameras();
            if (cameraCount > 0) {
                try {
                    int cameraId = getFrontFacingCameraId();
                    if (cameraId == -1) {
                        cameraId = 0;
                    }

                    camera = Camera.open(cameraId);
                    info = new Camera.CameraInfo();
                    Camera.getCameraInfo(cameraId, info);
                    parameters = camera.getParameters();

                    configCamera();
                    startPreview();

                    preview.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View arg0) {
                            camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                        }
                    });
                } catch (RuntimeException ex) {
                    //Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void closeCamera() {
        if (camera != null) {
            stopPreview();
            camera.release();
            camera = null;
            info = null;
            parameters = null;
        }
    }

    private void configCamera() {
        setPreviewOrientation();
        setCameraRotation();
        setAutoFocusMode();
    }

    private void setPreviewOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void setCameraRotation() {
        parameters.setRotation(info.orientation);
        camera.setParameters(parameters);
    }

    private void setAutoFocusMode() {
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            // set the focus mode
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            // set Camera parameters
            camera.setParameters(parameters);
        }
    }

    private void startPreview() {
        Camera.Size previewSize;
        List<Camera.Size> supportPreviewSizes = parameters.getSupportedPreviewSizes();

        // We fit the aspect ratio of TextureView to the size of preview we picked.
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            previewSize = getOptimalPreviewSize(supportPreviewSizes, previewWidth, previewHeight);
            preview.setAspectRatio(previewSize.width, previewSize.height);
        } else {
            previewSize = getOptimalPreviewSize(supportPreviewSizes, previewHeight, previewWidth);
            preview.setAspectRatio(previewSize.height, previewSize.width);
        }

        parameters.setPreviewSize(previewSize.width, previewSize.height);
        camera.setParameters(parameters);

        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (camera != null) {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    private void stopPreview() {
        if (camera != null) {
            camera.stopPreview();
        }
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {

        }
    };

    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };

    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            preview.setOnClickListener(null);

            String storagePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
            SaveImageTask saveImageTask = new SaveImageTask();
            saveImageTask.setListener(new SaveImageTask.OnSaveListener() {
                @Override
                public void onComplete(String imagePath) {
                    if(imagePath != null) {
                        Intent intent = new Intent();
                        intent.putExtra("image_path", imagePath);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                }
            });
            saveImageTask.execute(storagePath.getBytes(), data);
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };

    private int getFrontFacingCameraId() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return cameraInfo.facing;
            }
        }

        return -1;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int width, int height) {
        holder = surfaceHolder;

        if (previewWidth == 0 && previewHeight == 0) {
            previewWidth = width;
            previewHeight = height;

            Log.v(TAG, "preview width: " + width + " height: " + height);
        }

        openCamera();
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        holder = null;

        closeCamera();
    }

    private static class SaveImageTask extends AsyncTask<byte[], Void, String> {

        interface OnSaveListener {
            void onComplete(String imagePath);
        }

        OnSaveListener listener = null;

        @Override
        protected String doInBackground(byte[]... data) {
            File storageDir = new File(String.valueOf(data[0]));

            deleteAllFiles(storageDir);

            FileOutputStream outStream = null;
            File outFile = null;
            try {
                outFile = createImageFile(storageDir);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[1]);
                outStream.flush();
//                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(outStream != null)
                    try {
                        outStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }

            return outFile != null? outFile.getAbsolutePath() : null;
        }

        @Override
        protected void onPostExecute(String imagePath) {
            notifyListener(imagePath);
        }

        private void notifyListener(String imagePath) {
            if(listener != null) {
                listener.onComplete(imagePath);
            }
        }

        void setListener(OnSaveListener listener) {
            this.listener = listener;
        }

        private File createImageFile(File storageDir) throws IOException {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
//            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        }

        private void deleteAllFiles(File storageDir) {
//            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir != null && storageDir.isDirectory()) {
//                String[] children = dir.list();
//                for (int i = 0; i < children.length; i++) {
//                    new File(dir, children[i]).delete();
//                }
                for (String child : storageDir.list()) {
                    boolean deleted = new File(storageDir, child).delete();
                }
            }
        }
    }

}
