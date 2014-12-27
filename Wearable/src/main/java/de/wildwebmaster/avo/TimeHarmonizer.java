package de.wildwebmaster.avo;

import java.util.Calendar;

/**
 * Created by vahldiek on 12/26/14.
 */
public class TimeHarmonizer {

    private long time = 0;
    private Calendar c;


    TimeHarmonizer(long time) {
        this.time = time;
        this.c = Calendar.getInstance();
        c.setTimeInMillis(time);
    }

    public static int getHourOfTS(long ts) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ts);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public static int getMinutesOfTS(long ts) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ts);
        return calendar.get(Calendar.MINUTE);
    }

    public static int getDayOfTS(long ts) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ts);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int calcTickDiff(int numTicks, int first, int second) {
        if (second > first)
            return second - first;
        else
            return numTicks - first + second;
    }

    public int getHours() {
        return c.get(Calendar.HOUR_OF_DAY);
    }

    public int getMinutes() {
        return c.get(Calendar.MINUTE);
    }

    public int getDay() {
        return c.get(Calendar.DAY_OF_MONTH);
    }

    public int getTick(int numTicks) {

        int intermedTicks = numTicks / 12;

        int h = this.getHours();
        if (h >= 12)
            h -= 12; // get it on a 12 hour scale

        int min = this.getMinutes();
        int group = 0;
        for (int i = 1; i <= intermedTicks; i++) {
            if (((float) min) < i * (60 / intermedTicks)) {
                group = i - 1;
                break;
            }
        }

        return h * intermedTicks + group;
    }
}
