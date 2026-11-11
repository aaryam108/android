package com.example.luna;

import java.util.Calendar;
import java.util.TimeZone;

public class LunarCalculator {

    public static final double SYNODIC_MONTH = 29.530588853;
    private static final double NEW_MOON_EPOCH =
            947182440000.0; // 6 Jan 2000, 18:14 UTC

    public static class MoonData {
        public double age;
        public double illumination;
        public double phase;
        public String phaseName;
    }

    public static MoonData calculate(Calendar date) {

        Calendar utcDate = (Calendar) date.clone();
        utcDate.setTimeZone(TimeZone.getTimeZone("UTC"));

        double timestamp = utcDate.getTimeInMillis();

        double days = (timestamp - NEW_MOON_EPOCH)
                / 86400000.0;

        double age = days % SYNODIC_MONTH;

        if (age < 0) {
            age += SYNODIC_MONTH;
        }

        double phase = age / SYNODIC_MONTH;

        double illumination =
                (1 - Math.cos(2 * Math.PI * phase)) / 2;

        MoonData data = new MoonData();

        data.age = age;
        data.phase = phase;
        data.illumination = illumination * 100;
        data.phaseName = getPhaseName(phase);

        return data;
    }

    public static String getPhaseName(double phase) {

        if (phase < 0.0625 || phase >= 0.9375)
            return "New Moon";

        if (phase < 0.1875)
            return "Waxing Crescent";

        if (phase < 0.3125)
            return "First Quarter";

        if (phase < 0.4375)
            return "Waxing Gibbous";

        if (phase < 0.5625)
            return "Full Moon";

        if (phase < 0.6875)
            return "Waning Gibbous";

        if (phase < 0.8125)
            return "Last Quarter";

        return "Waning Crescent";
    }

    public static Calendar findNextPhase(
            Calendar startDate, double targetPhase) {

        Calendar result = (Calendar) startDate.clone();

        result.set(Calendar.HOUR_OF_DAY, 12);
        result.set(Calendar.MINUTE, 0);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i <= 35; i++) {

            MoonData data = calculate(result);

            double difference =
                    Math.abs(data.phase - targetPhase);

            difference = Math.min(
                    difference, 1.0 - difference);

            if (difference < 0.012) {
                return result;
            }

            result.add(Calendar.DAY_OF_MONTH, 1);
        }

        return result;
    }
}
