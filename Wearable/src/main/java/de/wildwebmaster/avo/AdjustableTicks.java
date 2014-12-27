package de.wildwebmaster.avo;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import android.util.Log;
import android.util.Pair;

/**
 * Created by vahldiek on 12/26/14.
 */
public class AdjustableTicks<T extends Paintable> {


    private static final String TAG = "AdjustableTicks";

    private int numTicks = 0;
    private float spaceTillBorder = 0;
    private float tickDepth = 0;
    private float tickLength = 0; // 360 / 48
    private float space = 0.3f;

    private Paint tickColor = null;
    private Paint tickHighlightColor = null;
    private Paint getTickHighlightColorAmbient = null;
    private Paint blackPaint = null;
    
    private Map<Integer, T> entries;
    private float tickAngle[];
    private float tickSpace[];

    /**
     * Setting defaults
     */
    {
        tickColor = new Paint();
        tickColor.setARGB(255, 220, 220, 220);
        tickColor.setStrokeWidth(3.f);
        tickColor.setAntiAlias(true);
        tickColor.setStrokeCap(Paint.Cap.SQUARE);

        tickHighlightColor = new Paint();
        tickHighlightColor.setARGB(255,  0, 126, 255);
        tickHighlightColor.setStrokeWidth(3.f);
        tickHighlightColor.setAntiAlias(true);
        tickHighlightColor.setStrokeCap(Paint.Cap.SQUARE);

        getTickHighlightColorAmbient = new Paint();
        getTickHighlightColorAmbient.setARGB(255,  100, 100, 100);
        getTickHighlightColorAmbient.setStrokeWidth(3.f);
        getTickHighlightColorAmbient.setAntiAlias(true);
        getTickHighlightColorAmbient.setStrokeCap(Paint.Cap.SQUARE);

        blackPaint = new Paint(Color.BLACK);
        blackPaint.setAntiAlias(true);
        blackPaint.setStrokeCap(Paint.Cap.SQUARE);
    }


    private void init() {

        this.entries = new HashMap<>(numTicks);
        clear();


//        Log.v(TAG, "after clear");

        // two for loops to reorder (since angle 0 - 90)
        this.tickAngle = new float[numTicks];
        this.tickSpace = new float[numTicks];
        Arrays.fill(tickAngle, 0.f);
        Arrays.fill(tickSpace, 0.f);
        float bumpBottom = 0;
        int intermedTicks = numTicks/12;
        for(int ticks = 0; ticks < numTicks; ticks++) {

//            Log.v(TAG, "at tick " + ticks);

            if(ticks < numTicks/4) {
                tickAngle[ticks] = (ticks * tickLength + space)+270.f;
            } else {
                tickAngle[ticks] = (ticks * tickLength + space) - 90.f;
            }

            if(ticks == numTicks/2 - intermedTicks && numTicks <= numTicks/2) {
                bumpBottom += 3;
            }
            tickSpace[ticks] = spaceTillBorder + bumpBottom;
        }
    }
    
    /**
     * Creates a ring of 60 ticks close to the border
     */
    public AdjustableTicks() {
        this.numTicks = 60;
        this.spaceTillBorder = 0;
        this.tickDepth = 12;
        this.tickLength = 360.f / this.numTicks;

        init();
    }

    /**
     * Creates a ring of numTicks ticks on the screen of size [width, height] at
     * spaceTillBorder pixels away from the border with a depth of tickDepth
     * @param numTicks number of ticks on the ring of ticks
     * @param spaceTillBorder space till border in px
     * @param tickDepth number of pixels from outer border of ticks to inner border
     */
    public AdjustableTicks(int numTicks, float spaceTillBorder, float tickDepth) {

        this.numTicks = numTicks;
        this.spaceTillBorder = spaceTillBorder;
        this.tickDepth = spaceTillBorder + tickDepth;
        tickLength =  360.f / numTicks;

//        Log.v(TAG, "before init");

        init();
    }

    public AdjustableTicks setTickPaint(Paint tickPaint) {
        this.tickColor = tickPaint;
        return this;
    }

    public AdjustableTicks<T> setHighlightPaint(Paint highlightPaint) {
        this.tickHighlightColor = highlightPaint;
        return this;
    }

    public AdjustableTicks<T> setHighlightAmbientPaint(Paint highlightAmbientPaint) {
        this.getTickHighlightColorAmbient = highlightAmbientPaint;
        return this;
    }

    public AdjustableTicks<T> setSpace(float space) {
        this.space = space;
        return this;
    }

    public void clear() {
        if(entries.size() == 0)
            return;

        entries.clear();
    }

    public int getNumTicks() {
        return numTicks;
    }

    public void setTick(int tick, T value) {
        entries.put(tick, value);
    }

    public boolean isTickSet(int tick) {

        boolean ret = entries.get(tick) != null;
//        Log.v(TAG, "isTickSet "+ tick+ "," + ret);
        return ret;
    }

    public T getTickValue(int tick) {
        return entries.get(tick);
    }

    public Pair<Integer, T> findNextSetTick(int tick) {

        for(int it = 0; it < numTicks; it++) {
            int curTick = (tick+it)%numTicks;
            if(isTickSet(curTick)) {
                return new Pair<>(curTick, getTickValue(curTick));
            }
        }
        return null;
    }

    public Pair<Integer,T> findNextSetTickDiffOf(Pair<Integer, T> first) {
        int tick = first.first;
        T firstValue = first.second;

        for(int it = 0; it < numTicks; it++) {
            int curTick = (tick+it)%numTicks;
            if(isTickSet(curTick) && !getTickValue(curTick).equals(firstValue)) {
                return new Pair(curTick, getTickValue(curTick));
            }
        }
        return null;
    }


    public void draw(Canvas canvas, float width, float height) {

        if(canvas == null || entries == null)
            return;

        for (int ticks = 0; ticks < numTicks; ticks ++) {

            Paint p = tickColor;

            T v = entries.get(ticks);
            if(v != null) {
                p = v.getPaint();
            }

            float calcSpace = tickSpace[ticks];
            canvas.drawArc(calcSpace, calcSpace, width - calcSpace, height - calcSpace,
                    tickAngle[ticks], tickLength-space*2, true, p);
        }


        float bump = 15;
        canvas.drawArc(tickDepth, tickDepth, width - tickDepth, height-tickDepth, 0, 360/6 - 360/48, true, blackPaint);
        canvas.drawRect(tickDepth+bump+44, height/2, width - (tickDepth+bump+44), height-(tickDepth+bump+16), blackPaint);
        canvas.drawArc(tickDepth, tickDepth, width - tickDepth, height-tickDepth, 360/4 + 360/12 + 360/48, 360 - (360/4 + 360/12 + 360/48), true, blackPaint);
    }
}
