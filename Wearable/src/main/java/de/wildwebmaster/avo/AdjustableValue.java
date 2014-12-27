package de.wildwebmaster.avo;

import android.graphics.Paint;

/**
 * Created by vahldiek on 12/27/14.
 */
public class AdjustableValue<T extends Paintable> implements Adjustable{

    private T value;

    public void clear() {
        value = null;
    }

    public Paint getPaint() {
        return value.getPaint();
    }
}
