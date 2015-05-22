
package com.zxing.qrcodelib.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.zxing.qrcodelib.decode.Decode;

import java.io.IOException;
import java.lang.reflect.Method;

public final class CameraManager {
    private static final String TAG = CameraManager.class.getSimpleName();

    private static final int MIN_FRAME_WIDTH = 240;

    private static final int MIN_FRAME_HEIGHT = 240;

    private static final int MAX_FRAME_WIDTH = 675;

    private static final int MAX_FRAME_HEIGHT = 0;

    private final Context context;

    private final CameraConfigurationManager configManager;

    private Camera camera;

    private AutoFocusManager autoFocusManager;

    private Rect framingRect;

    private Rect framingRectInPreview;

    private boolean initialized;

    private boolean previewing;

    private int requestedCameraId = -1;

    private int requestedFramingRectWidth;

    private int requestedFramingRectHeight;

    private float topOffsetScale;

    private PreviewCallback previewCallback;

    public CameraManager(Context context) {
        this.context = context;
        this.configManager = new CameraConfigurationManager(context);
    }

    public synchronized void openDriver(SurfaceHolder holder, float topOffsetScale)
            throws IOException {
        this.topOffsetScale = topOffsetScale;
        Camera theCamera = this.camera;
        if (theCamera == null) {
            if (this.requestedCameraId >= 0)
                theCamera = OpenCameraInterface.open(this.requestedCameraId);
            else {
                theCamera = OpenCameraInterface.open();
            }

            if (theCamera == null) {
                throw new IOException();
            }
            this.camera = theCamera;
        }
        theCamera.setPreviewDisplay(holder);
        if (!this.initialized) {
            this.initialized = true;
            this.configManager.initFromCameraParameters(theCamera, holder);
            if ((this.requestedFramingRectWidth > 0) && (this.requestedFramingRectHeight > 0)) {
                setManualFramingRect(this.requestedFramingRectWidth,
                        this.requestedFramingRectHeight);
                this.requestedFramingRectWidth = 0;
                this.requestedFramingRectHeight = 0;
            }
        }

        Camera.Parameters parameters = theCamera.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten();
        try {
            this.configManager.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);

            if (parametersFlattened != null) {
                parameters = theCamera.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    theCamera.setParameters(parameters);
                    this.configManager.setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }
    }

    public synchronized boolean isOpen() {
        return this.camera != null;
    }

    public synchronized void closeDriver() {
        if (this.camera != null) {
            this.camera.release();
            this.camera = null;

            this.framingRect = null;
            this.framingRectInPreview = null;
        }
    }

    public synchronized void startPreview(Decode decode) {
        Camera theCamera = this.camera;
        if ((theCamera != null) && (!this.previewing)) {
            theCamera.startPreview();
            this.previewing = true;
            this.autoFocusManager = new AutoFocusManager(this.context, this.camera);
            this.previewCallback = new PreviewCallback(this.configManager, decode);
            decode.start();
        }
    }

    public synchronized void stopPreview() {
        if (this.autoFocusManager != null) {
            this.autoFocusManager.stop();
            this.autoFocusManager = null;
        }
        if ((this.camera != null) && (this.previewing)) {
            this.camera.setOneShotPreviewCallback(null);
            this.camera.stopPreview();
            this.previewing = false;
        }
    }

    public synchronized void setTorch(boolean newSetting) {
        if ((newSetting != this.configManager.getTorchState(this.camera)) && (this.camera != null)) {
            if (this.autoFocusManager != null) {
                this.autoFocusManager.stop();
            }
            this.configManager.setTorch(this.camera, newSetting);
            if (this.autoFocusManager != null)
                this.autoFocusManager.start();
        }
    }

    public synchronized void requestPreviewFrame() {
        Camera theCamera = this.camera;
        if ((theCamera != null) && (this.previewing))
            theCamera.setOneShotPreviewCallback(this.previewCallback);
    }

    public synchronized Rect getFramingRect() {
        if (this.framingRect == null) {
            if (this.camera == null) {
                return null;
            }
            Point screenResolution = this.configManager.getScreenResolution();
            if (screenResolution == null) {
                return null;
            }

            int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH,
                    MAX_FRAME_WIDTH);
            int height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_WIDTH, width);
            System.out.println(screenResolution.x);
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (int) ((screenResolution.y - height) * this.topOffsetScale);
            this.framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset
                    + height);
            Log.d(TAG, "Calculated framing rect: " + this.framingRect);
        }
        return this.framingRect;
    }

    private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8;
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

    public synchronized Rect getFramingRectInPreview() {
        if (this.framingRectInPreview == null) {
            Rect framingRect = getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            Point cameraResolution = this.configManager.getCameraResolution();
            Point screenResolution = this.configManager.getScreenResolution();
            if ((cameraResolution == null) || (screenResolution == null)) {
                return null;
            }

            rect.left = (rect.left * cameraResolution.y / screenResolution.x);
            rect.right = (rect.right * cameraResolution.y / screenResolution.x);
            rect.top = (rect.top * cameraResolution.x / screenResolution.y);
            rect.bottom = (rect.bottom * cameraResolution.x / screenResolution.y);
            this.framingRectInPreview = rect;
        }
        return this.framingRectInPreview;
    }

    public synchronized void setManualCameraId(int cameraId) {
        this.requestedCameraId = cameraId;
    }

    public synchronized void setManualFramingRect(int width, int height) {
        if (this.initialized) {
            Point screenResolution = this.configManager.getScreenResolution();
            if (width > screenResolution.x) {
                width = screenResolution.x;
            }
            if (height > screenResolution.y) {
                height = screenResolution.y;
            }
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            this.framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset
                    + height);
            Log.d(TAG, "Calculated manual framing rect: " + this.framingRect);
            this.framingRectInPreview = null;
        } else {
            this.requestedFramingRectWidth = width;
            this.requestedFramingRectHeight = height;
        }
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview();
        if (rect == null) {
            System.out.println("-------------------null");
            return null;
        }

        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(),
                rect.height(), false);
    }

    public void setDisplayOrientation(Camera camera, int angle) {
        try {
            Method down = camera.getClass().getMethod("setDisplayOrientation", new Class[] {
                Integer.TYPE
            });

            if (down != null)
                down.invoke(camera, new Object[] {
                    Integer.valueOf(angle)
                });
        } catch (Exception localException) {
        }
    }
}
