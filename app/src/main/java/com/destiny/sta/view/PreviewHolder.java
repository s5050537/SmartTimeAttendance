package com.destiny.sta.view;

import java.util.List;

import android.content.Context;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.destiny.sta.CameraMvp;

/**
 * Created by Bobo on 8/29/2017.
 */

public class PreviewHolder extends ViewGroup implements CameraMvp.View {

    public final String TAG = PreviewHolder.class.getSimpleName();

    private SurfaceHolder surfaceHolder = null;

    private Size mPreviewSize = null;
    private List<Size> mSupportedPreviewSizes = null;

    public PreviewHolder(Context context, SurfaceView surfaceView) {
        super(context);
        addView(surfaceView);
    }

//    public void setCamera(Camera camera) {
//        mCamera = camera;
//        if (mCamera != null) {
//            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
//            requestLayout();
//
//            // get Camera parameters
//            Camera.Parameters params = mCamera.getParameters();
//
//            List<String> focusModes = params.getSupportedFocusModes();
//            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
//                // set the focus mode
//                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//                // set Camera parameters
//                mCamera.setParameters(params);
//            }
//        }
//    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            if(width < height) {
                mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, height, width);
            } else {
                mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);
            surfaceHolder = ((SurfaceView) child).getHolder();

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                if (width < height) {
                    previewWidth = mPreviewSize.height;
                    previewHeight = mPreviewSize.width;
                } else {
                    previewWidth = mPreviewSize.width;
                    previewHeight = mPreviewSize.height;
                }
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    @Override
    public void setSupportedPreviewSizes(List<Size> supportedPreviewSizes) {
        mSupportedPreviewSizes = supportedPreviewSizes;
        requestLayout();
    }

    @Override
    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
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
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }
}
