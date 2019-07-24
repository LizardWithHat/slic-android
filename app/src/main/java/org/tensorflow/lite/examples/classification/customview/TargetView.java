package org.tensorflow.lite.examples.classification.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class TargetView extends ImageView {

    private int inputWidth = 0;
    private int inputHeight = 0;

    public TargetView(final Context context) {
        this(context, null);
    }

    public TargetView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TargetView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onDraw(Canvas c){
        super.onDraw(c);
        Rect target = new Rect(0, 0, inputWidth, inputHeight);
        target.offset(getWidth() / 2 - inputWidth / 2, getHeight() / 2 - inputHeight / 2);
        Paint p = new Paint();
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5.0f);
        c.drawRect(target, p);
    }

    public void setInputWidth(int inputWidth) {
        this.inputWidth = inputWidth;
    }

    public void setInputHeight(int inputHeight) {
        this.inputHeight = inputHeight;
    }
}

