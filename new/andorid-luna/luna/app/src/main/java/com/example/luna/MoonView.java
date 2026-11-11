package com.example.luna;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class MoonView extends View {

    private Paint paint;
    private double phase = 0;
    private float radius;

    public MoonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoonView(Context context) {
        super(context);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public void setPhase(double phase) {
        this.phase = phase;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        radius = Math.min(getWidth(), getHeight()) * 0.38f;

        // Moon shadow
        paint.setShader(null);
        paint.setColor(Color.rgb(35, 40, 54));
        canvas.drawCircle(cx, cy, radius, paint);

        // Moon surface
        paint.setShader(new RadialGradient(
                cx - radius * 0.3f,
                cy - radius * 0.3f,
                radius * 1.5f,
                new int[]{
                        Color.rgb(245, 244, 226),
                        Color.rgb(195, 195, 181),
                        Color.rgb(110, 113, 115)
                },
                null,
                Shader.TileMode.CLAMP
        ));

        // Clip illumination inside Moon
        canvas.save();

        Path circle = new Path();
        circle.addCircle(cx, cy, radius, Path.Direction.CW);
        canvas.clipPath(circle);

        canvas.drawCircle(cx, cy, radius, paint);
        paint.setShader(null);

        // Draw illuminated portion
        paint.setColor(Color.rgb(235, 235, 215));

        Path litPath = new Path();

        float terminator = (float) Math.cos(phase * 2 * Math.PI);

        if (phase <= 0.5) {

            // Waxing: right side illuminated
            float x = cx + radius * terminator;

            litPath.moveTo(cx, cy - radius);

            litPath.cubicTo(
                    cx + radius, cy - radius,
                    cx + radius, cy + radius,
                    cx, cy + radius
            );

            litPath.cubicTo(
                    x, cy + radius,
                    x, cy - radius,
                    cx, cy - radius
            );

        } else {

            // Waning: left side illuminated
            float x = cx - radius * terminator;

            litPath.moveTo(cx, cy - radius);

            litPath.cubicTo(
                    cx - radius, cy - radius,
                    cx - radius, cy + radius,
                    cx, cy + radius
            );

            litPath.cubicTo(
                    x, cy + radius,
                    x, cy - radius,
                    cx, cy - radius
            );
        }

        litPath.close();
        canvas.drawPath(litPath, paint);

        // Subtle lunar surface details
        paint.setColor(Color.argb(35, 70, 70, 65));

        canvas.drawCircle(
                cx - radius * 0.25f,
                cy - radius * 0.2f,
                radius * 0.12f, paint);

        canvas.drawCircle(
                cx + radius * 0.3f,
                cy + radius * 0.22f,
                radius * 0.16f, paint);

        canvas.drawCircle(
                cx - radius * 0.05f,
                cy + radius * 0.42f,
                radius * 0.07f, paint);

        canvas.restore();
    }
}
