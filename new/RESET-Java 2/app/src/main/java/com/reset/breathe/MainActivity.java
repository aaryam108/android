package com.reset.breathe;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int INHALE = 0, HOLD = 1, EXHALE = 2;
    private static final String[] PHASES = {"Breathe in", "Hold gently", "Breathe out"};
    private static final String[] NAMES = {"INHALE", "HOLD", "EXHALE"};
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Editable breathing durations, in seconds.
    private final int[] durations = {4, 4, 6};
    private int phase = INHALE;
    private int secondsLeft = durations[INHALE];
    private int cycles = 0;
    private boolean running = false;
    private boolean haptics = true;

    private TextView phaseText, timerText, hintText, cycleText, hapticsButton;
    private Button startButton;
    private BreathingView breathingView;
    private final TextView[] durationLabels = new TextView[3];
    private final TextView[] phaseNames = new TextView[3];
    private final LinearLayout[] phaseTiles = new LinearLayout[3];
    private ValueAnimator orbAnimator;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            secondsLeft--;
            if (secondsLeft <= 0) {
                phase = (phase + 1) % 3;
                if (phase == INHALE) cycles++;
                secondsLeft = durations[phase];
                if (haptics) softPulse();
                animatePhase();
                updateUi();
            } else {
                timerText.setText(String.valueOf(secondsLeft));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(17, 27, 27));
        getWindow().setNavigationBarColor(Color.rgb(17, 27, 27));
        buildInterface();
        updateUi();
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(22), dp(24), dp(16));
        root.setBackgroundColor(Color.rgb(17, 27, 27));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("R E S E T", 14, Color.rgb(189, 232, 214), true);
        TextView tagline = text("A little space for you.", 12, Color.rgb(168, 184, 177), false);
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.addView(logo);
        brand.addView(tagline);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        hapticsButton = text("HAPTICS  ON", 10, Color.rgb(189, 232, 214), true);
        hapticsButton.setPadding(dp(8), dp(12), 0, dp(12));
        hapticsButton.setOnClickListener(v -> {
            haptics = !haptics;
            hapticsButton.setText(haptics ? "HAPTICS  ON" : "HAPTICS  OFF");
            hapticsButton.setTextColor(haptics ? mint() : muted());
        });
        header.addView(hapticsButton);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        addFlexibleSpace(root);
        TextView overline = text("C O M E   B A C K   T O   Y O U R S E L F", 10, muted(), false);
        overline.setGravity(Gravity.CENTER);
        root.addView(overline);
        addSpace(root, 12);

        breathingView = new BreathingView(this);
        root.addView(breathingView, new LinearLayout.LayoutParams(dp(260), dp(260)));

        phaseText = text("Find your stillness", 20, mint(), true);
        phaseText.setGravity(Gravity.CENTER);
        root.addView(phaseText);
        timerText = text("", 14, muted(), false);
        timerText.setGravity(Gravity.CENTER);
        root.addView(timerText);
        addSpace(root, 5);

        hintText = text("A slower breath. A softer moment.", 12, muted(), false);
        hintText.setGravity(Gravity.CENTER);
        root.addView(hintText);
        addSpace(root, 18);

        TextView editTitle = text("YOUR BREATHING RHYTHM", 10, muted(), true);
        editTitle.setLetterSpacing(0.12f);
        root.addView(editTitle);
        addSpace(root, 8);

        LinearLayout rhythm = new LinearLayout(this);
        rhythm.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 3; i++) {
            final int index = i;
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(4), dp(9), dp(4), dp(9));
            tile.setBackgroundColor(Color.rgb(27, 42, 40));
            phaseTiles[i] = tile;

            phaseNames[i] = text(NAMES[i], 9, muted(), true);
            phaseNames[i].setLetterSpacing(0.08f);
            durationLabels[i] = text(durations[i] + "s", 16, Color.WHITE, true);
            LinearLayout controls = new LinearLayout(this);
            controls.setGravity(Gravity.CENTER);
            Button minus = smallButton("−");
            Button plus = smallButton("+");
            minus.setOnClickListener(v -> changeDuration(index, -1));
            plus.setOnClickListener(v -> changeDuration(index, 1));
            controls.addView(minus, new LinearLayout.LayoutParams(dp(38), dp(34)));
            controls.addView(plus, new LinearLayout.LayoutParams(dp(38), dp(34)));
            tile.addView(phaseNames[i]);
            addSpace(tile, 3);
            tile.addView(durationLabels[i]);
            tile.addView(controls);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) lp.leftMargin = dp(7);
            rhythm.addView(tile, lp);
        }
        root.addView(rhythm);

        addFlexibleSpace(root);
        startButton = new Button(this);
        startButton.setAllCaps(false);
        startButton.setText("BEGIN RESET");
        startButton.setTextColor(Color.rgb(17, 27, 27));
        startButton.setTextSize(12);
        startButton.setLetterSpacing(0.10f);
        startButton.setBackgroundTintList(ColorStateList.valueOf(mint()));
        startButton.setOnClickListener(v -> toggleSession());
        root.addView(startButton, new LinearLayout.LayoutParams(-1, dp(54)));

        addSpace(root, 8);
        cycleText = text("0 cycles completed", 10, muted(), false);
        cycleText.setGravity(Gravity.CENTER);
        root.addView(cycleText);
        setContentView(root);
    }

    private void changeDuration(int index, int amount) {
        int updated = Math.max(1, Math.min(30, durations[index] + amount));
        if (updated == durations[index]) return;
        durations[index] = updated;
        durationLabels[index].setText(updated + "s");
        if (running && phase == index) {
            // Apply edits immediately to the active phase and restart its smooth motion.
            secondsLeft = updated;
            timerText.setText(String.valueOf(secondsLeft));
            animatePhase();
        } else if (!running && index == phase) {
            secondsLeft = updated;
        }
    }

    private void toggleSession() {
        if (running) {
            running = false;
            handler.removeCallbacks(tick);
            stopOrbAnimator();
            phase = INHALE;
            secondsLeft = durations[INHALE];
            breathingView.setScaleValue(1f);
        } else {
            running = true;
            phase = INHALE;
            secondsLeft = durations[INHALE];
            if (haptics) softPulse();
            animatePhase();
            handler.postDelayed(tick, 1000);
        }
        updateUi();
    }

    private void animatePhase() {
        stopOrbAnimator();
        float from, to;
        if (phase == INHALE) {
            from = 0.82f; to = 1.14f;
            breathingView.setScaleValue(from);
        } else if (phase == HOLD) {
            from = 1.14f; to = 1.14f;
            breathingView.setScaleValue(from);
        } else {
            from = 1.14f; to = 0.82f;
            breathingView.setScaleValue(from);
        }
        if (phase == HOLD) return; // Stay expanded throughout the hold.
        orbAnimator = ValueAnimator.ofFloat(from, to);
        orbAnimator.setDuration(durations[phase] * 1000L);
        orbAnimator.setInterpolator(new LinearInterpolator());
        orbAnimator.addUpdateListener(animation ->
                breathingView.setScaleValue((float) animation.getAnimatedValue()));
        orbAnimator.start();
    }

    private void stopOrbAnimator() {
        if (orbAnimator != null) {
            orbAnimator.cancel();
            orbAnimator = null;
        }
    }

    private void updateUi() {
        if (phaseText == null) return;
        phaseText.setText(running ? PHASES[phase] : "Find your stillness");
        timerText.setText(running ? String.valueOf(secondsLeft) : "");
        hintText.setText(running ? "Follow the orb. Nothing to force." : "A slower breath. A softer moment.");
        startButton.setText(running ? "PAUSE SESSION" : "BEGIN RESET");
        cycleText.setText(String.format(Locale.getDefault(), "%d %s completed  ·  Edit each phase above",
                cycles, cycles == 1 ? "cycle" : "cycles"));
        for (int i = 0; i < 3; i++) {
            boolean selected = running && i == phase;
            phaseTiles[i].setBackgroundColor(selected ? Color.rgb(42, 69, 64) : Color.rgb(27, 42, 40));
            phaseNames[i].setTextColor(selected ? mint() : muted());
            durationLabels[i].setText(durations[i] + "s");
        }
    }

    private Button smallButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setTextColor(mint());
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(48, 72, 65)));
        return button;
    }

    private void softPulse() {
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager manager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = manager.getDefaultVibrator();
            } else {
                //noinspection deprecation
                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    vibrator.vibrate(VibrationEffect.createOneShot(35, 45));
                else {
                    //noinspection deprecation
                    vibrator.vibrate(35);
                }
            }
        } catch (Exception ignored) { }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(android.graphics.Typeface.create("sans-serif", bold ?
                android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        return view;
    }

    private void addSpace(LinearLayout parent, int heightDp) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private void addFlexibleSpace(LinearLayout parent) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int mint() { return Color.rgb(189, 232, 214); }
    private int muted() { return Color.rgb(168, 184, 177); }

    @Override protected void onPause() {
        super.onPause();
        if (running) {
            running = false;
            handler.removeCallbacks(tick);
            stopOrbAnimator();
            updateUi();
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(tick);
        stopOrbAnimator();
        super.onDestroy();
    }

    private static class BreathingView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float scaleValue = 1f;

        BreathingView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setScaleValue(float value) {
            scaleValue = value;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.34f;

            paint.setShader(new RadialGradient(cx, cy, radius * 1.8f,
                    new int[]{Color.argb(60, 189, 232, 214), Color.argb(18, 110, 170, 145), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius * 1.8f, paint);

            canvas.save();
            canvas.scale(scaleValue, scaleValue, cx, cy);
            paint.setShader(new RadialGradient(cx - radius * .25f, cy - radius * .30f, radius * 1.5f,
                    new int[]{Color.rgb(215, 245, 232), Color.rgb(141, 189, 167), Color.rgb(78, 118, 105)},
                    new float[]{0f, .58f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.2f);
            paint.setColor(Color.argb(65, 255, 255, 255));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.restore();
        }
    }
}
