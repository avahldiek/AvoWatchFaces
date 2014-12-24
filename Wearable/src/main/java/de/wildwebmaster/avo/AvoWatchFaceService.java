package de.wildwebmaster.avo;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.wearable.provider.WearableCalendarContract;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;

import de.wildwebmaster.avo.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import de.wildwebmaster.avo.cal.SimpleCalEvents;

/**
 * Avo WatchFace displaying the time with a continuous seconds finger & calendar details.
 */
public class AvoWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "AvoWatchFaceService";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        Paint mHourPaint;
        Paint mMinutePaint;
        Paint mSecondPaint;
        Paint mTickPaintCross;
        Paint mTickPaintRegular;
        Paint mBlackPaint;
        Paint mAlmostBlackPaint;
        Paint mCalPaint;
        Paint mCalHighlightPaint;
        Paint mCalHighlightABPaint;
        TextPaint mTextPaint;
        TextPaint mLogoPaint;
        boolean mMute;
        Time mTime;
        float curMinRot = -1.0f;

        private AsyncTask<Void, Void, Integer> mLoadMeetingsTask;
        static final int MSG_LOAD_MEETINGS = 0;
        int mNumMeetings;
        List<SimpleCalEvents> mCalEvents;

        /** Handler to load the meetings once a minute in interactive mode. */
        final Handler mLoadMeetingsHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_LOAD_MEETINGS:
                        cancelLoadMeetingTask();
                        mLoadMeetingsTask = new LoadMeetingsTask();
                        mLoadMeetingsTask.execute();
                        break;
                }
            }
        };

        private boolean mIsReceiverRegistered;

        private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_PROVIDER_CHANGED.equals(intent.getAction())
                        && WearableCalendarContract.CONTENT_URI.equals(intent.getData())) {
                    cancelLoadMeetingTask();
                    mLoadMeetingsHandler.sendEmptyMessage(MSG_LOAD_MEETINGS);
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundBitmapAB;
        Bitmap mBackgroundScaledBitmap;
        Bitmap mBackgroundScaledBitmapAB;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(AvoWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setViewProtection(WatchFaceStyle.PROTECT_WHOLE_SCREEN)
                    .setHotwordIndicatorGravity(Gravity.CENTER_VERTICAL)
                    .setStatusBarGravity(Gravity.CENTER_VERTICAL)
                    .setShowUnreadCountIndicator(true)
                    .build());

            Resources resources = AvoWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            resources = AvoWatchFaceService.this.getResources();
            backgroundDrawable = resources.getDrawable(R.drawable.bgab);
            mBackgroundBitmapAB = ((BitmapDrawable) backgroundDrawable).getBitmap();

            mHourPaint = new Paint();
            mHourPaint.setARGB(255, 255, 255, 255);
            mHourPaint.setStrokeWidth(6.5f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.SQUARE);

            mMinutePaint = new Paint();
            mMinutePaint.setARGB(255, 255, 255, 255);
            mMinutePaint.setStrokeWidth(5.f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.SQUARE);

            mSecondPaint = new Paint();
            mSecondPaint.setARGB(255, 0, 126, 255);
            mSecondPaint.setStrokeWidth(2.f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.SQUARE);

            mTickPaintRegular = new Paint();
            mTickPaintRegular.setARGB(255, 130, 130, 130);
            mTickPaintRegular.setStrokeWidth(2.f);
            mTickPaintRegular.setAntiAlias(true);
            mTickPaintRegular.setStrokeCap(Paint.Cap.SQUARE);

            mTickPaintCross = new Paint();
            mTickPaintCross.setARGB(255, 130, 130, 130);
            mTickPaintCross.setStrokeWidth(5.f);
            mTickPaintCross.setAntiAlias(true);
            mTickPaintCross.setStrokeCap(Paint.Cap.SQUARE);

            mCalPaint = new Paint();
            mCalPaint.setARGB(255, 220, 220, 220);
            mCalPaint.setStrokeWidth(3.f);
            mCalPaint.setAntiAlias(true);
            mCalPaint.setStrokeCap(Paint.Cap.SQUARE);

            mCalHighlightPaint = new Paint();
            mCalHighlightPaint.setARGB(255,  0, 126, 255);
            mCalHighlightPaint.setStrokeWidth(3.f);
            mCalHighlightPaint.setAntiAlias(true);
            mCalHighlightPaint.setStrokeCap(Paint.Cap.SQUARE);

            mCalHighlightABPaint = new Paint();
            mCalHighlightABPaint.setARGB(255,  100, 100, 100);
            mCalHighlightABPaint.setStrokeWidth(3.f);
            mCalHighlightABPaint.setAntiAlias(true);
            mCalHighlightABPaint.setStrokeCap(Paint.Cap.SQUARE);

            mBlackPaint = new Paint();
            mBlackPaint.setARGB(255, 0, 0, 0);
            mBlackPaint.setAntiAlias(true);

            mAlmostBlackPaint = new Paint();
            mAlmostBlackPaint.setARGB(255, 0, 0, 0);
            mAlmostBlackPaint.setAntiAlias(true);

            mTime = new Time();

            mTextPaint = new TextPaint();
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setTextSize(16);
            mTextPaint.setAntiAlias(true);

            mLogoPaint = new TextPaint();
            mLogoPaint.setColor(Color.WHITE);
            mLogoPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mLogoPaint.setTextSize(22);
            mLogoPaint.setAntiAlias(true);

            mLoadMeetingsHandler.sendEmptyMessage(MSG_LOAD_MEETINGS);

            mCalEvents = new LinkedList<>();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
                mTickPaintRegular.setAntiAlias(antiAlias);
                mTickPaintCross.setAntiAlias(antiAlias);
                mBlackPaint.setAntiAlias(antiAlias);
                mAlmostBlackPaint.setAntiAlias(antiAlias);
                mCalPaint.setAntiAlias(antiAlias);
                mCalHighlightABPaint.setAntiAlias(antiAlias);
                mTextPaint.setAntiAlias(antiAlias);
                mLogoPaint.setAntiAlias(antiAlias);
            }
            invalidate();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "onDraw");
            }

            long now = System.currentTimeMillis();
            mTime.set(now);
            int milliseconds = (int) (now % 1000);

            int width = bounds.width();
            int height = bounds.height();

            float centerX = width / 2f;
            float centerY = height / 2f;

//            if(canvas.getSaveCount() == 0) {

            canvas.drawRect(0,0,width, height, new Paint(Color.BLACK));

                // create save
            drawCalTicks(canvas, width, height);
            drawTicks(canvas, centerX, centerY);

            if (!isInAmbientMode()) {
                // Draw the background, scaled to fit.

                if (mBackgroundScaledBitmap == null
                        || mBackgroundScaledBitmap.getWidth() != width
                        || mBackgroundScaledBitmap.getHeight() != height) {
                    mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                            width, height, true /* filter */);
                }
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            }

            canvas.drawText("avo", width/2-18, 65, mLogoPaint);
            drawNextEvent(canvas, width, height);


            drawFingers(canvas, width, height, centerX, centerY, milliseconds);

            // Draw every frame as long as we're visible and in interactive mode.
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        private void drawFingers(Canvas canvas, long width, long center, float centerX, float centerY, long milliseconds) {
            float seconds = mTime.second + milliseconds / 1000f;
            float secRot = seconds / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = (minutes) / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f ) * (float) Math.PI;

            float secLength = centerX - 20;
            float innerSpace = 13.f;
            float minLength = centerX - 30;
            float hrLength = centerX - 50;

            if (!isInAmbientMode()) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                float secXinner = (float) Math.sin(secRot) * (-innerSpace);
                float secYinner = (float) -Math.cos(secRot) * (-innerSpace);
                canvas.drawLine(centerX + secXinner, centerY + secYinner, centerX + secX, centerY + secY, mSecondPaint);
            }

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            float hrXinner = (float) Math.sin(hrRot);
            float hrYinner = (float) -Math.cos(hrRot);
            canvas.drawLine(centerX + hrXinner, centerY + hrYinner, centerX + hrX, centerY + hrY, mHourPaint);


            if (!isInAmbientMode()) {
                minRot += (seconds/60f) / 30f * (float) Math.PI;
                curMinRot = minRot;
            }
            // protection for switch to ambient mode and minute is jumping back
            if(minRot < curMinRot)
                minRot = curMinRot;
            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            float minXinner = (float) Math.sin(minRot);
            float minYinner = (float) -Math.cos(minRot);
            canvas.drawLine(centerX + minXinner, centerY + minYinner, centerX + minX, centerY + minY, mMinutePaint);

            float innerCircleRadius = 6;
            canvas.drawArc(centerX-innerCircleRadius, centerY-innerCircleRadius, centerX+innerCircleRadius, centerY+innerCircleRadius, 0, 360, true, mMinutePaint);

            if(isInAmbientMode())
                canvas.drawArc(centerX-2, centerY-2, centerX+2, centerY+2, 0, 360, true, mBlackPaint);
            else
                canvas.drawArc(centerX-2, centerY-2, centerX+2, centerY+2, 0, 360, true, mSecondPaint);
        }

        private void drawTicks(Canvas canvas, float centerX, float centerY) {
            // Draw the ticks.
            float innerTickRadius = centerX - 25;
            float outerTickRadius = centerX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                Paint p;
                float shorter = 0;
                switch(tickIndex % 3) {
                    case 0:
                        p = mTickPaintCross;
                        shorter = 0;
                        break;
                    default:
                        p = mTickPaintRegular;
                        shorter = 0;
                        break;
                }

                int extend6 = 0;
                if(tickIndex == 6) {
                    extend6 = 25;
                }
                int extend57 = 0;
                if(tickIndex == 5 || tickIndex == 7)
                    extend57 = 10;

                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * (innerTickRadius - shorter - extend6 - extend57);
                float innerY = (float) -Math.cos(tickRot) * (innerTickRadius - shorter - extend6- extend57);
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;

                canvas.drawLine(centerX + innerX, centerY + innerY,
                        centerX + outerX, centerY + outerY, p);
            }
        }

        private void drawCalTicks(Canvas canvas, float width, float height) {

            float tillBorder = 0;
            float depth = tillBorder + 12;
            float intermedTicks = 4;
            float numTicks = 12 * intermedTicks; // 12 * 4
            float tickLength = 360.f / numTicks; // 360 / 48
            float space = 0.3f;
            float bumpBottom = 0;

            for (int ticks = 0; ticks < numTicks; ticks++) {

                if(ticks == numTicks/2 - intermedTicks && numTicks <= numTicks/2) {
                    bumpBottom += 3;
                }

                float degreeStart = ticks * tickLength + space;

                //Log.v(TAG, "degreestart " + Float.toString(degreeStart));

                float calcSpace = tillBorder + bumpBottom;
                canvas.drawArc(calcSpace, calcSpace, width - calcSpace, height - calcSpace,
                        degreeStart, tickLength-space*2, true, mCalPaint);
            }


            Paint p = mCalHighlightABPaint;
            for (SimpleCalEvents ev : mCalEvents) {

                if(!isInAmbientMode()) {
                    p = ev.getPaint();
                    Log.v(TAG, "not ambient paint set to " + p);
                    if(p == null) {
                        p = mCalHighlightPaint;
                        Log.v(TAG, "no paint set");
                    }
                }

                long hS = ev.hoursStart();
                if(hS > 12)
                    hS -= 12; // get it on a 12 hour scale
                hS -= 3; // rotate by 90 degrees

                long mS = ev.minutesStart();
                int group = 0;
                for (int i = 1; i <= intermedTicks; i++) {
                    if(((float)mS) < i * (60/intermedTicks)) {
                        group = i - 1;
                        break;
                    }
                }

                long hE = ev.hoursEnd();
                if(hE > 12)
                    hE -= 12; // get it on a 12 hour scale
                hE -= 3; // rotate by 90 degrees
                long mE = ev.minutesEnd();
                int groupE = 0;
                for (int i = 1; i <= intermedTicks; i++) {
                    if(((float)mE) < i * (60/intermedTicks)) {
                        groupE = i - 1;
                        break;
                    }
                }

                long ticksTillEnd = 0;
                if(hE == hS) {
                    ticksTillEnd += groupE - group;
                } else {
                    // ticks from current hour
                    ticksTillEnd += intermedTicks - group;
                    // tricks from end hour
                    ticksTillEnd += groupE;
                    // hour ticks
                    ticksTillEnd += (ev.hoursEnd() - (ev.hoursStart() + 1)) * intermedTicks;
                }
                // ticks in the middle


                Log.v(TAG, "paint " + ev + " with " + ticksTillEnd + " ticks");

                for (int ticks = 0; ticks < ticksTillEnd; ticks++) {

                    float startAngle = hS * 4 * tickLength + group * tickLength + ticks * tickLength + space;

//                    Log.v(TAG, hS + "," + ticks + "," + group + "," + groupE + "," +startAngle);

                    canvas.drawArc(tillBorder, tillBorder, width - tillBorder, height - tillBorder,
                            startAngle, tickLength - space * 2, true, p);
                }


            }


            float bump = 15;
//            canvas.drawArc(depth-2, depth-2, width - depth + 2, height-depth + 2, 0, 360, true, mAlmostBlackPaint);
            canvas.drawArc(depth, depth, width - depth, height-depth, 0, 360/6 - 360/48, true, mBlackPaint);
            canvas.drawRect(depth+bump+44, height/2, width - (depth+bump+44), height-(depth+bump+16), mBlackPaint);
            canvas.drawArc(depth, depth, width - depth, height-depth, 360/4 + 360/12 + 360/48, 360 - (360/4 + 360/12 + 360/48), true, mBlackPaint);
        }

        private void drawNextEvent(Canvas canvas, int width, int height) {
            if(mCalEvents != null && mCalEvents.size() > 0) {
                final int maxTextLen = 22;
                SimpleCalEvents first = null;
                for (SimpleCalEvents sce : mCalEvents) {
                    if(!sce.startedPastSeconds(60*15)) {
                        first = sce;
                        break;
                    }

                    Log.v(TAG, "pass " + sce);
                }

                Log.v(TAG, "first " + first);

                if(first != null) {
                    String title = first.getTitle();
                    String location = first.getLocation();
                    canvas.drawText(title, 0, (title.length() > maxTextLen) ? maxTextLen : title.length(), 70, height - 90, mTextPaint);
                    canvas.drawLine(70, height - 85, width - 70, height - 85, mTickPaintRegular);
                    canvas.drawText(location, 0, (location.length() > maxTextLen) ? maxTextLen : location.length(), 70, height - 70, mTextPaint);
                }
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiverTimeZone();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();

                registerReceiverCalendar();

                invalidate();
            } else {

                unregisterReceiverTimeZone();
                unregisterReceiverCalendar();

            }

        }

        private void registerReceiverTimeZone() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            AvoWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void registerReceiverCalendar() {
            IntentFilter filter = new IntentFilter(Intent.ACTION_PROVIDER_CHANGED);
            filter.addDataScheme("content");
            filter.addDataAuthority(WearableCalendarContract.AUTHORITY, null);
            registerReceiver(mBroadcastReceiver, filter);
            mIsReceiverRegistered = true;

            mLoadMeetingsHandler.sendEmptyMessage(MSG_LOAD_MEETINGS);
        }

        private void unregisterReceiverTimeZone() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            AvoWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void unregisterReceiverCalendar() {
            if (mIsReceiverRegistered) {
                unregisterReceiver(mBroadcastReceiver);
                mIsReceiverRegistered = false;
            }
            mLoadMeetingsHandler.removeMessages(MSG_LOAD_MEETINGS);
        }


        private void onMeetingsLoaded(Integer result) {
            if (result != null) {
                mNumMeetings = result;
                invalidate();
            }
        }

        private void cancelLoadMeetingTask() {
            if (mLoadMeetingsTask != null) {
                mLoadMeetingsTask.cancel(true);
            }
        }

        /**
         * Asynchronous task to load the meetings from the content provider and report the number of
         * meetings back via {@link #onMeetingsLoaded}.
         */
        private class LoadMeetingsTask extends AsyncTask<Void, Void, Integer> {
            private PowerManager.WakeLock mWakeLock;

            @Override
            protected Integer doInBackground(Void... voids) {
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                mWakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK, "CalendarWatchFaceWakeLock");
                mWakeLock.acquire();

                long begin = System.currentTimeMillis() - 1000 * 60 * 60;
                Uri.Builder builder =
                    WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
                ContentUris.appendId(builder, begin);
                ContentUris.appendId(builder, begin + DateUtils.HOUR_IN_MILLIS * 12);
//                String []projection = {CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND};
                final Cursor cursor = getContentResolver().query(builder.build(),
                        null, null, null, null);
                int numMeetings = cursor.getCount();
                cursor.moveToFirst();

//                Log.v(TAG, Arrays.toString(cursor.getColumnNames()));
                if(numMeetings > 0) {
                    mCalEvents.clear();
                    int startTimeId =  cursor.getColumnIndex("dtStart");
                    int endTimeId =  cursor.getColumnIndex("dtEnd");
                    int colorId = cursor.getColumnIndex("calendar_color");
                    int displayColorId = cursor.getColumnIndex("displayColor");
                    int eventColorId = cursor.getColumnIndex("eventColor");
                    int allDayId = cursor.getColumnIndex("allDay");
                    int titleId = cursor.getColumnIndex("title");
                    int locationId = cursor.getColumnIndex("eventLocation");


//                    Log.v(TAG, "[" + startTimeId +", "+endTimeId+", "+colorId+", "+allDayId+", "+titleId+", "+locationId + "]" );

                    if(startTimeId != -1 && endTimeId != -1) {
                        while (cursor.moveToNext()) {
                            try {
                                long startTime = Long.parseLong(cursor.getString(startTimeId));
                                long endTime = Long.parseLong(cursor.getString(endTimeId));
                                int allDay = Integer.parseInt(cursor.getString(allDayId));

                                long color = Long.parseLong(cursor.getString(colorId));
                                long displayColor = Long.parseLong(cursor.getString(displayColorId));
                                String title = cursor.getString(titleId);
                                String location = cursor.getString(locationId);

                                if(allDay == 0) {

                                    Log.v(TAG, "added: [" + startTime + ", " + endTime + ", " + color + "," + displayColor + ", " + allDay + ", " + title + ", " + location + "]");
                                    mCalEvents.add(new SimpleCalEvents(title, location, color, startTime, endTime));
                                } else {
//                                    Log.v(TAG, "removed due to all day: [" + startTime + ", " + endTime + ", " + color + ", " + allDay + ", " + title + ", " + location + "]");
                                }

                            } catch (Exception e) {

                            }
                        }
                    }
                }

                cursor.close();

                Collections.sort(mCalEvents);

//                Log.v(TAG, mCalEvents.toString());

                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, Arrays.toString(cursor.getColumnNames()));
                    Log.v(TAG, "Num meetings: " + numMeetings);
                }
                return numMeetings;
            }

            @Override
            protected void onPostExecute(Integer result) {
                releaseWakeLock();
                onMeetingsLoaded(result);
            }

            @Override
            protected void onCancelled() {
                releaseWakeLock();
            }

            private void releaseWakeLock() {
                if (mWakeLock != null) {
                    mWakeLock.release();
                    mWakeLock = null;
                }
            }
        }

    }
}
