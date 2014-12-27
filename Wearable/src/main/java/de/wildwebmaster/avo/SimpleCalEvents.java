package de.wildwebmaster.avo;

import android.graphics.Paint;

import java.util.Calendar;

/**
 * Created by vahldiek on 12/19/14.
 */
public class SimpleCalEvents implements Comparable<SimpleCalEvents>, Paintable{

    private String title;
    private String location;
    private long color = 0;
    private long startTime = 0;
    private long endTime = 0;
    private Paint paint = null;

    private TimeHarmonizer startH;
    private TimeHarmonizer endH;

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

        startH = new TimeHarmonizer(startTime);
        endH = new TimeHarmonizer(endTime);

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

        return startH.getHours();
    }

    public long hoursEnd() {
        return endH.getHours();
    }

    public long minutesStart() {
        return startH.getMinutes();
    }

    public long minutesEnd() {
        return endH.getMinutes();
    }

    public int dayStart() {
        return startH.getDay();
    }

    public int dayEnd() {
        return endH.getDay();
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getHarmoziedStart(int numTicks) {
        return startH.getTick(numTicks);
    }

    public int getHarmoziedEnd(int numTicks) {
        return endH.getTick(numTicks);
    }

    public TimeHarmonizer getStartH() {
        return startH;
    }

    public TimeHarmonizer getEndH() {
        return endH;
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

    public boolean endsBefore(long timeInMs) {
        if(timeInMs > endTime)
            return true;
        else
            return false;
    }

    public Paint getPaint() {
        return paint;
    }

    public boolean equals(Object o) {
        if(o instanceof SimpleCalEvents) {
            SimpleCalEvents snd = (SimpleCalEvents) o;
            if(snd.startTime == this.startTime && snd.endTime == this.endTime
                    && snd.color == this.color && snd.title.equals(this.title)
                    && snd.location.equals(this.location)) {
                return true;
            }
        }

        return false;
    }
}
