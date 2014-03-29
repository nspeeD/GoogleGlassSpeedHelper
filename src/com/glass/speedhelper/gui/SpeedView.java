package com.glass.speedhelper.gui;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.glass.speedhelper.R;
import com.glass.speedhelper.managers.StateLocationManager;

/**
 * Draws a stylized compass, with text labels at the cardinal and ordinal directions, and tick
 * marks at the half-winds. The red "needles" in the display mark the current heading.
 */
public class SpeedView extends View {
    
    public static final int KMH = 0;
    public static final int MPH = 1;
    public static final int DEFAULT = KMH;
    
    private static final double KMH_IN_MPS = 0.277777778;
    private static final double MPH_IN_MPS = 0.44704;

    private StateLocationManager mLocation;

    private final Typeface mSpeedTypeface;
    private final NumberFormat mDistanceFormat;
    private int uom = DEFAULT;
    
    private TextView mSpeed;
    private TextView mUom;

    public SpeedView(Context context) {
        this(context, null, 0);
    }

    public SpeedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpeedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        LayoutInflater.from(context).inflate(R.layout.speed_view_lay, null, false);

        mSpeedTypeface = Typeface.createFromFile(new File("/system/glass_fonts",
                                                    "Roboto-Thin.ttf"));
        
        mSpeed = (TextView) findViewById(R.id.tvSpeed);
        mUom = (TextView) findViewById(R.id.tvUom);
        
        mSpeed.setTypeface(mSpeedTypeface);
        mUom.setTypeface(mSpeedTypeface);
        
        mDistanceFormat = NumberFormat.getNumberInstance();
        mDistanceFormat.setMinimumFractionDigits(0);
        mDistanceFormat.setMaximumFractionDigits(1);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        setVisibility(VISIBLE);
    }


    /**
     * Sets the instance of {@link StateLocationManager} that this view will use to get the current
     * heading and location.
     *
     * @param stateLocationManager the instance of {@code StateLocationManager} that this view will use
     */
    public void setStateLocationManager(StateLocationManager stateLocationManager) {
    	mLocation = stateLocationManager;
    }

    /**
     * Sets the current speed in m/s and redraws the HUD.
     *
     * @param degrees the current heading
     */
    public void setSpeed(float speed) {
        invalidate();
    }
    
    /**
     * Set the unit of measurement.
     * 
     * @param uom the new  unit of measurement.
     */
    public void setUom(int uom) {
        switch (uom) {
        case KMH: this.uom = KMH; break;
        case MPH: this.uom = MPH; break;
        default:      this.uom = DEFAULT;
        }
        
        invalidate();
    }
    
    public int getUom() {
        return uom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // now the speed
        drawSpeed(canvas);
    }

    /**
     * Draws the speed
     *
     * @param canvas the {@link Canvas} upon which to draw
     */
    private void drawSpeed(Canvas canvas) {
        
        int speed = (int) mLocation.getLocation().getSpeed();
        String uomStr;
        switch (uom) {
        case KMH:
            speed /= KMH_IN_MPS;
            uomStr = " km/h";
            break;
        case MPH:
            speed /= MPH_IN_MPS;
            uomStr = " mph";
            break;
        default:
            uomStr = " m/s";
        }
        if (speed > 1000) {
            speed = 999;
        }
        
        final DecimalFormat smallNumberFormat = new DecimalFormat("0.0");
        
        String speedStr = speed < 10 ? smallNumberFormat.format(speed)
                        : Integer.toString(speed);
        
        mSpeed.setText(speedStr);
        mUom.setText(uomStr);
    }
}
