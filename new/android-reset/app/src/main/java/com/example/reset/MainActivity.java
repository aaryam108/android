
package com.example.reset;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView breathingText;
    private TextView timerText;
    private View breathingCircle;

    private Button startButton;
    private Button resetButton;

    private CountDownTimer phaseTimer;
    private CountDownTimer sessionTimer;

    private int phaseIndex = 0;
    private int remainingSeconds = 4;
    private int elapsedSeconds = 0;

    private boolean isRunning = false;
    private boolean sessionFinished = false;

    private final String[] phases = {
            "Breathe in",
            "Hold",
            "Breathe out"
    };

    private final int[] durations = {
            4, 2, 6
    };

    private float[] scales = {
            1.25f, 1.25f, 0.75f
    };

    private static final int SESSION_DURATION = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        breathingText = findViewById(R.id.breathingText);
        timerText = findViewById(R.id.timerText);
        breathingCircle = findViewById(R.id.breathingCircle);

        startButton = findViewById(R.id.startButton);
        resetButton = findViewById(R.id.resetButton);

        updateTimer();

        startButton.setOnClickListener(v -> {
            if (isRunning) {
                pauseBreathing();
            } else {
                startBreathing();
            }
        });

        resetButton.setOnClickListener(v -> resetBreathing());
    }

    private void startBreathing() {

        if (sessionFinished) {
            resetBreathing();
        }

        isRunning = true;

        startButton.setText("PAUSE");

        if (elapsedSeconds == 0 && phaseIndex == 0
                && remainingSeconds == durations[0]) {
            startSessionTimer();
        }

        startPhase();
    }

    private void startSessionTimer() {

        sessionTimer = new CountDownTimer(
                (SESSION_DURATION - elapsedSeconds) * 1000L,
                1000
        ) {

            @Override
            public void onTick(long millisUntilFinished) {

                elapsedSeconds++;

                updateTimer();
            }

            @Override
            public void onFinish() {

                elapsedSeconds = SESSION_DURATION;

                updateTimer();

                finishSession();
            }

        }.start();
    }

    private void startPhase() {

        if (!isRunning) return;

        breathingText.setText(phases[phaseIndex]);

        animateCircle(
                scales[phaseIndex],
                durations[phaseIndex] * 1000L
        );

        phaseTimer = new CountDownTimer(
                remainingSeconds * 1000L,
                1000
        ) {

            @Override
            public void onTick(long millisUntilFinished) {

                remainingSeconds =
                        (int) Math.ceil(millisUntilFinished / 1000.0);

                breathingText.setText(
                        phases[phaseIndex] + "\n" + remainingSeconds
                );
            }

            @Override
            public void onFinish() {

                if (!isRunning) return;

                phaseIndex = (phaseIndex + 1) % phases.length;

                remainingSeconds = durations[phaseIndex];

                startPhase();
            }

        }.start();
    }

    private void animateCircle(float targetScale, long duration) {

        float currentScale = breathingCircle.getScaleX();

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(
                breathingCircle,
                "scaleX",
                currentScale,
                targetScale
        );

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(
                breathingCircle,
                "scaleY",
                currentScale,
                targetScale
        );

        AnimatorSet animatorSet = new AnimatorSet();

        animatorSet.playTogether(scaleX, scaleY);

        animatorSet.setDuration(duration);

        animatorSet.setInterpolator(
                new AccelerateDecelerateInterpolator()
        );

        animatorSet.start();
    }

    private void pauseBreathing() {

        isRunning = false;

        if (phaseTimer != null) {
            phaseTimer.cancel();
        }

        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        breathingCircle.animate().cancel();

        breathingText.setText("Paused");

        startButton.setText("RESUME");
    }

    private void resetBreathing() {

        isRunning = false;
        sessionFinished = false;

        if (phaseTimer != null) {
            phaseTimer.cancel();
        }

        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        breathingCircle.animate().cancel();

        breathingCircle.setScaleX(1f);
        breathingCircle.setScaleY(1f);

        phaseIndex = 0;
        remainingSeconds = durations[0];
        elapsedSeconds = 0;

        breathingText.setText("Ready to breathe");

        startButton.setText("START BREATHING");

        updateTimer();
    }

    private void finishSession() {

        isRunning = false;
        sessionFinished = true;

        if (phaseTimer != null) {
            phaseTimer.cancel();
        }

        breathingCircle.animate().cancel();

        breathingText.setText("Well done!\nTake a moment.");

        startButton.setText("START AGAIN");
    }

    private void updateTimer() {

        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;

        timerText.setText(
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        minutes,
                        seconds
                )
        );
    }

    @Override
    protected void onDestroy() {

        if (phaseTimer != null) {
            phaseTimer.cancel();
        }

        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        super.onDestroy();
    }
}
