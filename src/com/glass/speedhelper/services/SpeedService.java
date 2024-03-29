package com.glass.speedhelper.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import com.glass.speedhelper.MenuActivity;
import com.glass.speedhelper.SpeedRenderer;
import com.glass.speedhelper.gui.SpeedView;
import com.glass.speedhelper.managers.StateLocationManager;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

/**
 * The main application service that manages the lifetime of the speed helper live card.
 */
public class SpeedService extends Service {

    private static final String LIVE_CARD_ID = "speed_helper";
    
    private static final String PREFERENCES_NAME = SpeedService.class.toString();
    private static final String PREFS_UOM_KEY = "key_uom";

    /**
     * A binder that gives other components access to the speech capabilities provided by the
     * service.
     */
    public class SpeedBinder extends Binder {
        /**
         * Read the current heading aloud using the text-to-speech engine.
         */
        public void readMaxSpeed() {

//            Resources res = getResources();
//            String[] spokenDirections = res.getStringArray(R.array.spoken_directions);
//            String directionName = spokenDirections[MathUtils.getHalfWindIndex(heading)];
//
//            int roundedHeading = Math.round(heading);
//            int headingFormat;
//            if (roundedHeading == 1) {
//                headingFormat = R.string.spoken_heading_format_one;
//            } else {
//                headingFormat = R.string.spoken_heading_format;
//            }
//
//            String maxSpeedText = res.getString(headingFormat, roundedHeading, directionName);
//            mSpeech.speak(maxSpeedText, TextToSpeech.QUEUE_FLUSH, null);
        }
        
        public SpeedService getSpeedService() {
            return SpeedService.this;
        }
    }

    private final SpeedBinder mBinder = new SpeedBinder();

    private StateLocationManager mStateLocationManager;
    private TextToSpeech mSpeech;

    private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;
    private SpeedRenderer mRenderer;
    
    @Override
    public void onCreate() {
        super.onCreate();

        mTimelineManager = TimelineManager.from(this);

        // Even though the text-to-speech engine is only used in response to a menu action, we
        // initialize it when the application starts so that we avoid delays that could occur
        // if we waited until it was needed to start it up.
        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            }
        });

        SensorManager sensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE); //Enabled to use sensors
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mStateLocationManager = new StateLocationManager(sensorManager, locationManager);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_ID);
            mRenderer = new SpeedRenderer(this, mStateLocationManager);

            LiveCard direct = mLiveCard.setDirectRenderingEnabled(true);
            direct.getSurfaceHolder().addCallback(mRenderer);
            direct.getSurfaceHolder().setKeepScreenOn(true);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            
            mLiveCard.publish(PublishMode.REVEAL);
        }
        
        SharedPreferences prefs =
                        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        int uom = prefs.getInt(PREFS_UOM_KEY, SpeedView.DEFAULT);
        mRenderer.setUom(uom);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        SharedPreferences prefs =
                        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(PREFS_UOM_KEY, mRenderer.getUom());
        edit.commit();

        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard.getSurfaceHolder().removeCallback(mRenderer);
            mLiveCard = null;
        }

        mSpeech.shutdown();

        mSpeech = null;
        mStateLocationManager = null;

        super.onDestroy();
    }
    
    public void setUom(int uom) {
        mRenderer.setUom(uom);
    }
}
