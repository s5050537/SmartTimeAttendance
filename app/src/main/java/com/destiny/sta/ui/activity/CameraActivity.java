package com.destiny.sta.ui.activity;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.destiny.sta.R;
import com.destiny.sta.view.Preview;

import io.reactivex.Observable;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Bobo on 8/29/2017.
 */

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private final CompositeDisposable disposables = new CompositeDisposable();

    private final int DEFAULT_TIME_LEFT = 60;
    private int timeLeft = DEFAULT_TIME_LEFT;

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

        if (timeLeft == 0) {
            setTimer(timeLeft, true);
        } else {
            setTimer(timeLeft = DEFAULT_TIME_LEFT, true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();

        disposables.clear();
        setTimer(timeLeft, false);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        setTimer(timeLeft = DEFAULT_TIME_LEFT, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setTimer(0, false);
    }

    private void setTimer(final int timeLimit, final boolean trigger) {
        Observable<Long> observable;
        if (timeLimit == 0) {
            observable = Observable.just(0L);
        } else {
            observable = Observable.interval(1, TimeUnit.SECONDS);
        }
        disposables.clear();
        disposables.add(observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Long>() {
                    @Override
                    public void onNext(@NonNull Long seconds) {
                        if (seconds == timeLimit) {
                            if (trigger) {
                                finish();
                                timeLeft = DEFAULT_TIME_LEFT;
                            }
                            disposables.clear();
                            return;
                        }
                        timeLeft--;
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                }));
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
        setDisplayOrientation();
        setCameraRotation();
        setAutoFocusMode();
        setPictureSize();
        setPreviewSize();
    }

    private void setDisplayOrientation() {
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

    private void setPictureSize() {
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        int width = 0;
        int height = 0;

        int maxSize = 1000000;
        int minDiff = Integer.MAX_VALUE;
        for (Camera.Size pictureSize : supportedPictureSizes) {
            int size = (int) (pictureSize.width * pictureSize.height * (4 / 8f));
//            Log.v(TAG, "picture width: " + pictureSize.width + " height: " + pictureSize.height + " size: " + size / 1000f + "KB");
            if (size > maxSize) continue;
            if (size <= maxSize) {
                if (Math.abs(size - maxSize) < minDiff) {
                    width = pictureSize.width;
                    height = pictureSize.height;
                    minDiff = Math.abs(size - maxSize);
                }
            }
        }
//        Log.v(TAG, "selected picture width: " + width + " height: " + height);
        if (width != 0 && height != 0) {
            parameters.setPictureSize(width, height);
        } else {
            Camera.Size minPictureSize = supportedPictureSizes.get(0);
            parameters.setPictureSize(minPictureSize.width, minPictureSize.height);
        }
        camera.setParameters(parameters);
    }

    private void setPreviewSize() {
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        supportedPreviewSizes = sortSizes(supportedPreviewSizes);
        Camera.Size previewSize;

        // We fit the aspect ratio of TextureView to the size of preview we picked.
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            previewSize = getOptimalPreviewSize(supportedPreviewSizes, previewWidth, previewHeight);
            previewSize = chooseOptimalSize(supportedPreviewSizes, previewWidth, previewHeight, previewWidth, previewHeight,
                    supportedPreviewSizes.get(supportedPreviewSizes.size() - 1));
            preview.setAspectRatio(previewSize.width, previewSize.height);
        } else {
//            previewSize = getOptimalPreviewSize(supportedPreviewSizes, previewHeight, previewWidth);
            previewSize = chooseOptimalSize(supportedPreviewSizes, previewHeight, previewWidth, previewHeight, previewWidth,
                    supportedPreviewSizes.get(supportedPreviewSizes.size() - 1));
            preview.setAspectRatio(previewSize.height, previewSize.width);
        }
        Log.v(TAG, "preview width: " + previewSize.width + " height: " + previewSize.height);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        camera.setParameters(parameters);
    }

    private void startPreview() {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (camera != null) {
                camera.setPreviewDisplay(holder);
                camera.startPreview();

                preview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                    }
                });
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

//    private void refreshGallery(File file) {
//        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//        mediaScanIntent.setData(Uri.fromFile(file));
//        sendBroadcast(mediaScanIntent);
//    }

    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {

        }
    };

    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };

    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(final byte[] data, Camera camera) {
            preview.setOnClickListener(null);

            Observable<File> observable = Observable.fromCallable(new Callable<File>() {
                @Override
                public File call() throws Exception {
                    deleteAllFiles();

                    FileOutputStream outStream = null;
                    File outFile = null;
                    try {
                        outFile = createImageFile();

                        outStream = new FileOutputStream(outFile);
                        outStream.write(data);
                        outStream.flush();
                        Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (outStream != null)
                            try {
                                outStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }

                    return outFile;
                }
            });

            disposables.add(observable
                    // Run on a background thread
                    .subscribeOn(Schedulers.io())
                    // Be notified on the main thread
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(new Function<File, String>() {
                        @Override
                        public String apply(File file) throws Exception {
                            return file == null ? null : file.getAbsolutePath();
                        }
                    })
                    .subscribeWith(new DisposableObserver<String>() {
                        @Override
                        public void onNext(@NonNull String imagePath) {
                            if (imagePath != null) {
                                Intent intent = new Intent();
                                intent.putExtra("image_path", imagePath);
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {

                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG, "onPictureTaken - jpeg");
                        }
                    }));
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

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Camera.Size chooseOptimalSize(List<Camera.Size> choices, int textureViewWidth,
                                                 int textureViewHeight, int maxWidth, int maxHeight, Camera.Size aspectRatio) {
        Log.v(TAG, "aspect ratio width: " + aspectRatio.width + " height: " + aspectRatio.height);

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Camera.Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Camera.Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.width;
        int h = aspectRatio.height;
        for (Camera.Size option : choices) {
            Log.v(TAG, "supported preview width: " + option.width + " height: " + option.height);
            if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w) {
                if (option.width >= textureViewWidth &&
                        option.height >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return bigEnough.get(0);
        } else if (notBigEnough.size() > 0) {
            return notBigEnough.get(notBigEnough.size() - 1);
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices.get(0);
        }
    }

    private List<Camera.Size> sortSizes(List<Camera.Size> sizes) {
        if (sizes.size() > 1) {
            int sizeCount = sizes.size();
            for (int i = sizeCount - 1; i > 0; i--) {
                for (int j = 0; j < i; j++) {
                    int currentWidth = sizes.get(j).width;
                    int currentHeight = sizes.get(j).height;
                    int nextWidth = sizes.get(j + 1).width;
                    int nextHeight = sizes.get(j + 1).height;

                    if (currentWidth > nextWidth) {
                        Camera.Size size = sizes.remove(j);
                        sizes.add(j + 1, size);
                    } else if (currentWidth == nextWidth) {
                        if (currentHeight > nextHeight) {
                            Camera.Size size = sizes.remove(j);
                            sizes.add(j + 1, size);
                        }
                    }
                }
            }
        }

        return sizes;
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

            Log.v(TAG, "holder width: " + width + " height: " + height);
        }

        openCamera();
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        holder = null;

        closeCamera();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    private void deleteAllFiles() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && storageDir.isDirectory()) {
            for (String child : storageDir.list()) {
                boolean deleted = new File(storageDir, child).delete();
            }
        }
    }

}
