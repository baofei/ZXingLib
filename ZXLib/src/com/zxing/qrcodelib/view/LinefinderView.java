
package com.zxing.qrcodelib.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatMath;

public final class LinefinderView extends ViewfinderView {
    private int i = 0;

    private final Rect mRect;

    private final GradientDrawable mDrawable;

    private Drawable lineDrawable;

    private final int left = 0x6099CC33;

    private final int center = 0xFF00CC00;

    private final int right = 0x6099CC33;

    private CharSequence text;

    private Paint textPaint;

    private float width;

    private float hight = 24.0F;

    private int textColor = -1;

    public LinefinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.mRect = new Rect();
        this.mDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {
                left, left, center, center, center, right, right
        });

        this.textPaint = new Paint();
        this.textPaint.setTextSize(this.hight);
        this.textPaint.setColor(this.textColor);
        if (!TextUtils.isEmpty(this.text))
            this.width = FloatMath.ceil(Layout.getDesiredWidth(this.text, new TextPaint(
                    this.textPaint)));
    }

    public void setLineDrawable(int resId) {
        this.lineDrawable = getResources().getDrawable(resId);
    }

    public CharSequence getText() {
        return this.text;
    }

    public void setText(CharSequence text, int color, int textSize) {
        this.text = text;
        if (this.textPaint == null) {
            this.textPaint = new Paint();
        }
        if (color != -1) {
            this.textColor = color;
            this.textPaint.setColor(color);
        }
        if (textSize != -1) {
            this.hight = textSize;
            this.textPaint.setTextSize(textSize);
        }
        this.width = FloatMath.ceil(Layout.getDesiredWidth(text, new TextPaint(this.textPaint)));
    }

    @Override
    protected boolean isDrawLine() {
        return false;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if ((this.resultBitmap == null) && (!isDrawLine()) && (this.frame != null)) {
            this.paint.setColor(center);
            this.paint.setAlpha(255);

            canvas.drawRect(this.frame.left, this.frame.top, this.frame.left + 15,
                    this.frame.top + 5, this.paint);
            canvas.drawRect(this.frame.left, this.frame.top, this.frame.left + 5,
                    this.frame.top + 15, this.paint);

            canvas.drawRect(this.frame.right - 15, this.frame.top, this.frame.right,
                    this.frame.top + 5, this.paint);
            canvas.drawRect(this.frame.right - 5, this.frame.top, this.frame.right,
                    this.frame.top + 15, this.paint);

            canvas.drawRect(this.frame.left, this.frame.bottom - 5, this.frame.left + 15,
                    this.frame.bottom, this.paint);
            canvas.drawRect(this.frame.left, this.frame.bottom - 15, this.frame.left + 5,
                    this.frame.bottom, this.paint);

            canvas.drawRect(this.frame.right - 15, this.frame.bottom - 5, this.frame.right,
                    this.frame.bottom, this.paint);
            canvas.drawRect(this.frame.right - 5, this.frame.bottom - 15, this.frame.right,
                    this.frame.bottom, this.paint);

            if (!TextUtils.isEmpty(this.text)) {
                canvas.drawText(this.text, 0, this.text.length(), (getWidth() - this.width) / 2.0F,
                        this.frame.bottom + this.hight + this.hight, this.textPaint);
            }

            this.paint.setColor(center);

            this.paint.setAlpha(SCANNER_ALPHA[this.scannerAlpha]);

            this.scannerAlpha = ((this.scannerAlpha + 1) % SCANNER_ALPHA.length);

            if ((i += 5) < frame.bottom - frame.top) {
                if (this.lineDrawable == null) {
                    this.mDrawable.setShape(0);

                    this.mDrawable.setGradientType(0);

                    this.mDrawable.setCornerRadii(new float[] {
                            15.0F, 15.0F, 15.0F, 15.0F, 15.0F, 15.0F, 15.0F, 15.0F
                    });

                    this.mRect.set(this.frame.left + 10, this.frame.top + this.i,
                            this.frame.right - 10, this.frame.top + 1 + this.i);

                    this.mDrawable.setBounds(this.mRect);

                    this.mDrawable.draw(canvas);
                } else {
                    this.mRect.set(this.frame.left - 6, this.frame.top + this.i - 6,
                            this.frame.right + 6, this.frame.top + 6 + this.i);
                    this.lineDrawable.setBounds(this.mRect);
                    this.lineDrawable.draw(canvas);
                }
                invalidate();
            } else {
                this.i = 0;
                postInvalidateDelayed(ANIMATION_DELAY);
            }
        }
    }
}
