package com.example.vinylplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class VinylView extends View {
    private final Paint p = new Paint(3);
    private float rotation = 0f;
    private boolean spinning = false;
    private final int[] grooves = {0xFF24262A,0xFF303238,0xFF202226,0xFF34363B,0xFF202226,0xFF303238};

    public VinylView(Context c) { super(c); }
    public VinylView(Context c, AttributeSet a) { super(c,a); }
    public void setSpinning(boolean value) { spinning = value; invalidate(); }
    public void advance() { if (spinning) { rotation += 2.2f; if (rotation >= 360f) rotation -= 360f; invalidate(); } }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth()/2f, cy = getHeight()/2f;
        float radius = Math.min(getWidth(),getHeight()) * .46f;
        canvas.save();
        canvas.rotate(rotation,cx,cy);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF08090B); canvas.drawCircle(cx,cy,radius,p);
        for (int i=0;i<grooves.length;i++) {
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(i%2==0?2f:1f);
            p.setColor(grooves[i]); canvas.drawCircle(cx,cy,radius*(.88f-i*.075f),p);
        }
        p.setStyle(Paint.Style.FILL); p.setColor(0xFFB66D3E);
        canvas.drawCircle(cx,cy,radius*.34f,p);
        p.setColor(0xFF24201D); canvas.drawCircle(cx,cy,radius*.29f,p);
        p.setColor(0xFFE8B06D); canvas.drawCircle(cx,cy,radius*.045f,p);
        p.setColor(0xFF5D3928); canvas.drawCircle(cx,cy,radius*.19f,p);
        p.setColor(0xFFE8B06D); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(radius*.105f);
        canvas.drawText("SIDE A",cx,cy-radius*.035f,p);
        p.setTextSize(radius*.052f); canvas.drawText("VINYL • 33⅓ RPM",cx,cy+radius*.105f,p);
        canvas.restore();
        // small record-player spindle highlight
        p.setStyle(Paint.Style.FILL); p.setColor(0xFFD9D4CA);
        canvas.drawCircle(cx,cy,Math.max(3,radius*.018f),p);
    }
}
