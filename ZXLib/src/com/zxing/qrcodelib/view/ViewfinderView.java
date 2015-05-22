
package com.zxing.qrcodelib.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.zxing.qrcodelib.camera.CameraManager;

import java.util.ArrayList;
import java.util.List;

public class ViewfinderView extends View {
    protected static final int[] SCANNER_ALPHA = {
            0, 64, 128, 192, 255, 192, 128, 64
    };

    public static final long ANIMATION_DELAY = 80L;

    public static final int CURRENT_POINT_OPACITY = 0xA0;

    public static final int MAX_RESULT_POINTS = 20;

    public static final int POINT_SIZE = 6;

    protected CameraManager cameraManager;

    protected final Paint paint;

    protected Bitmap resultBitmap;

    private final int maskColor = 0x60000000;

    private final int resultColor = 0xb0000000;

    private final int laserColor = 0xffcc0000;

    private final int resultPointColor = 0xc0ffff00;

    protected int scannerAlpha;

    private List<ResultPoint> possibleResultPoints;

    private List<ResultPoint> lastPossibleResultPoints;

    protected Rect frame;

    protected Rect previewFrame;

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.paint = new Paint(1);
        this.scannerAlpha = 0;
        this.possibleResultPoints = new ArrayList<ResultPoint>(5);
        this.lastPossibleResultPoints = null;
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    protected boolean isDrawLine() {
        return true;
    }

    @Override
    @SuppressLint({
        "DrawAllocation"
    })
    public void onDraw(Canvas canvas) {
        if (this.cameraManager == null) {
            this.frame = null;
            this.previewFrame = null;
            postInvalidateDelayed(ANIMATION_DELAY);
            return;
        }
        if (this.frame == null) {
            this.frame = this.cameraManager.getFramingRect();
        }
        if (this.previewFrame == null) {
            this.previewFrame = this.cameraManager.getFramingRectInPreview();
        }
        if ((this.frame == null) || (this.previewFrame == null)) {
            postInvalidateDelayed(ANIMATION_DELAY);
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0.0F, 0.0F, width, this.frame.top, this.paint);
        canvas.drawRect(0.0F, this.frame.top, this.frame.left, this.frame.bottom + 1, this.paint);
        canvas.drawRect(this.frame.right + 1, this.frame.top, width, this.frame.bottom + 1,
                this.paint);
        canvas.drawRect(0.0F, this.frame.bottom + 1, width, height, this.paint);

        if (this.resultBitmap != null) {
            this.paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(this.resultBitmap, null, this.frame, this.paint);
        } else if (isDrawLine()) {
            this.paint.setColor(laserColor);
            this.paint.setAlpha(SCANNER_ALPHA[this.scannerAlpha]);
            this.scannerAlpha = ((this.scannerAlpha + 1) % SCANNER_ALPHA.length);
            int middle = this.frame.height() / 2 + this.frame.top;
            canvas.drawRect(this.frame.left + 2, middle - 1, this.frame.right - 1, middle + 2,
                    this.paint);

            float scaleX = this.frame.width() / this.previewFrame.width();
            float scaleY = this.frame.height() / this.previewFrame.height();

            List<ResultPoint> currentPossible = this.possibleResultPoints;
            List<ResultPoint> currentLast = this.lastPossibleResultPoints;
            int frameLeft = this.frame.left;
            int frameTop = this.frame.top;
            if (currentPossible.isEmpty()) {
                this.lastPossibleResultPoints = null;
            } else {
                this.possibleResultPoints = new ArrayList<ResultPoint>(5);
                this.lastPossibleResultPoints = currentPossible;
                this.paint.setAlpha(CURRENT_POINT_OPACITY);
                this.paint.setColor(resultPointColor);
                synchronized (currentPossible) {
                    for (ResultPoint point : currentPossible) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX), frameTop
                                + (int) (point.getY() * scaleY), POINT_SIZE, this.paint);
                    }
                }
            }
            if (currentLast != null) {
                this.paint.setAlpha(CURRENT_POINT_OPACITY / 2);
                this.paint.setColor(resultPointColor);
                synchronized (currentLast) {
                    float radius = 3.0F;
                    for (ResultPoint point : currentLast) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX), frameTop
                                + (int) (point.getY() * scaleY), radius, this.paint);
                    }

                }

            }

            postInvalidateDelayed(ANIMATION_DELAY, this.frame.left - POINT_SIZE, this.frame.top
                    - POINT_SIZE, this.frame.right + POINT_SIZE, this.frame.bottom + POINT_SIZE);
        }
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    public void drawResultBitmap(Bitmap barcode) {
        this.resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = this.possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }
}
