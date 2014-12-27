package de.wildwebmaster.avo;

import java.util.List;

/**
 * Created by vahldiek on 12/26/14.
 */
public class CalendarHarmonizer {


    private static final String TAG = "CalendarHarmozier";

    private AdjustableTicks<SimpleCalEvents> adjTicks;
    private int numTicks = 0;

    CalendarHarmonizer(int numTicks, AdjustableTicks<SimpleCalEvents> adjTicks) {

        this.numTicks = numTicks;
        this.adjTicks = adjTicks;


    }

    public void update(List<SimpleCalEvents> newCalEvents) {

        adjTicks.clear();
        TimeHarmonizer cur = new TimeHarmonizer(System.currentTimeMillis());

        for(SimpleCalEvents event : newCalEvents) {


//            Log.v(TAG, "found event " + event);

            int startTick = event.getHarmoziedStart(numTicks);
            int endTick = event.getHarmoziedEnd(numTicks);
            int length;


//            Log.v(TAG, "event start tick " + startTick + " endTick " + endTick);

            if(startTick < endTick) {
                // regular case (no day boundary)
                length = endTick - startTick;
            } else {
                // ticks from start to max ticks + ticks after noon boundary
                length = numTicks - startTick + endTick;
            }


//            Log.v(TAG, "event length " + length);

            int set = 0;
            for(int tick = 0; tick < length; tick++) {

                int curTick = (tick+startTick)%numTicks;
                if(adjTicks.isTickSet(curTick)) {
//                    Log.v(TAG, "continue " + curTick);
                    continue;
                }
                if(curTick == cur.getTick(numTicks)) {
//                    Log.v(TAG, "break at "+ curTick+ "," + adjTicks.isTickSet(curTick) + "," +cur.getTick(numTicks));
                    break;
                }

//                Log.v(TAG, "added tick " + (tick + startTick) );
                adjTicks.setTick((tick + startTick)%numTicks, event);
                set = 1;
            }
        }
    }


}
