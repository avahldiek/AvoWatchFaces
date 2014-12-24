package de.wildwebmaster.avo.cal;

import android.graphics.Paint;

import java.util.Calendar;

/**
 * Created by vahldiek on 12/19/14.
 */
public class SimpleCalEvents implements Comparable<SimpleCalEvents>{

    private String title;
    private String location;
    private long color = 0;
    private long startTime = 0;
    private long endTime = 0;
    private Paint paint = null;

    private static final int SECOND = 1000;
    private static final int MINUTE = 60 * SECOND;
    private static final int HOUR = 60 * MINUTE;
    private static final int DAY = 24 * HOUR;

    public SimpleCalEvents(String title, String location, long color, long startTime, long endTime) {

        this.title = title;
        this.location = location;
        this.color = color;
        this.startTime = startTime;
        this.endTime = endTime;


        this.paint = new Paint();//mCalHighlightPaint; ;
        this.paint.setColor((int)this.color);
    }

    public int compareTo(SimpleCalEvents sce) {

        return Long.compare(this.startTime, sce.startTime);
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public long getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "[" + title + "," + location + "," + color + "," + hoursStart() + ":" + minutesStart() + " - " + hoursEnd() + ":" + minutesEnd() + "]";
    }

    public long hoursStart() {

        return getHourOfTS(startTime);
    }

    public long hoursEnd() {
        return getHourOfTS(endTime);
    }

    public long minutesStart() {
        return getMinutesOfTS(startTime);
    }

    public long minutesEnd() {
        return getMinutesOfTS(endTime);
    }

    private int getHourOfTS(long ts) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ts);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    private int getMinutesOfTS(long ts) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ts);
        return calendar.get(Calendar.MINUTE);
    }

    private int getDayOfTS(long ts) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ts);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public boolean stared() {
        return startedPastSeconds(0);
    }

    public boolean startedPastSeconds(long pastTime) {
        if(System.currentTimeMillis() > startTime+pastTime *1000)
            return true;
        else
            return false;
    }

    public Paint getPaint() {
        return paint;
    }
}
