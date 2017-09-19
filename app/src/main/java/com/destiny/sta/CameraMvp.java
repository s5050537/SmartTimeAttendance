package com.destiny.sta;

import android.hardware.Camera;
import android.view.SurfaceHolder;

import java.util.List;

/**
 * Created by Bobo on 9/14/2017.
 */

public interface CameraMvp {

    interface View {

        void setSupportedPreviewSizes(List<Camera.Size> supportedPreviewSizes);

        SurfaceHolder getSurfaceHolder();

    }

    interface Presenter {

        void attachView(View view);

        void detachView();

        void startPreview(Camera.Size previewSize, int rotation);

        void stopPreview();
    }

}
