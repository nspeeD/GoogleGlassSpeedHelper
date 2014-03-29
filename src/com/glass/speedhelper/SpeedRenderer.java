package com.glass.speedhelper;

import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;

import com.glass.speedhelper.gui.SpeedView;
import com.glass.speedhelper.managers.StateLocationManager;

/**
 * The surface callback that provides the rendering logic for the speed live card. This callback
 * also manages the lifetime of the sensor and location event listeners (through
 * {@link StateLocationManager}) so that tracking only occurs when the card is visible.
 */
public class SpeedRenderer implements SurfaceHolder.Callback {

    private static final String TAG = SpeedRenderer.class.getSimpleName();

    /** The refresh rate, in frames per second, of the compass. */
    private static final int REFRESH_RATE_FPS = 45;

    /** The duration, in milliseconds, of one frame. */
    private static final long FRAME_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1) / REFRESH_RATE_FPS;

    private SurfaceHolder mHolder;
    private RenderThread mRenderThread;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private final FrameLayout mLayout;
    private final SpeedView mView;
    private final StateLocationManager mStateLocationManager;

    private final StateLocationManager.OnChangedListener mSpeedListener =
            new StateLocationManager.OnChangedListener() {

        @Override
        public void onLocationChanged(StateLocationManager stateLocationManager) {
            mView.setSpeed(stateLocationManager.getLocation().getSpeed());
        }
    };

    /**
     * Creates a new instance of the {@code SpeedHudRenderer} with the specified context
     * and orientation manager.
     */
    public SpeedRenderer(Context context, StateLocationManager stateLocationManager) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mLayout = (FrameLayout) inflater.inflate(R.layout.speed_content_lay, null);
        mLayout.setWillNotDraw(false);

        mView = (SpeedView) mLayout.findViewById(R.id.speedView);

        mStateLocationManager = stateLocationManager;

        mView.setStateLocationManager(mStateLocationManager);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        doLayout();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;

        mStateLocationManager.addOnChangedListener(mSpeedListener);
        mStateLocationManager.start();

        mRenderThread = new RenderThread();
        mRenderThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mRenderThread.quit();

        mStateLocationManager.removeOnChangedListener(mSpeedListener);
        mStateLocationManager.stop();
    }

    /**
     * Requests that the views redo their layout. This must be called manually every time the
     * tips view's text is updated because this layout doesn't exist in a GUI thread where those
     * requests will be enqueued automatically.
     */
    private void doLayout() {
        // Measure and update the layout so that it will take up the entire surface space
        // when it is drawn.
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(mSurfaceWidth,
                View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(mSurfaceHeight,
                View.MeasureSpec.EXACTLY);

        mLayout.measure(measuredWidth, measuredHeight);
        mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
    }

    /**
     * Repaints the speed HUD.
     */
    private synchronized void repaint() {
        Canvas canvas = null;

        try {
            canvas = mHolder.lockCanvas();
        } catch (RuntimeException e) {
            Log.d(TAG, "lockCanvas failed", e);
        }

        if (canvas != null) {
            mLayout.draw(canvas);

            try {
                mHolder.unlockCanvasAndPost(canvas);
            } catch (RuntimeException e) {
                Log.d(TAG, "unlockCanvasAndPost failed", e);
            }
        }
    }

    public void setUom(int uom) {
    	mView.setUom(uom);
    }

    public int getUom() {
        return mView.getUom();
    }

    /**
     * Redraws the s[eed HUD in the background.
     */
    private class RenderThread extends Thread {
        private boolean mShouldRun;

        /**
         * Initializes the background rendering thread.
         */
        public RenderThread() {
            mShouldRun = true;
        }

        /**
         * Returns true if the rendering thread should continue to run.
         *
         * @return true if the rendering thread should continue to run
         */
        private synchronized boolean shouldRun() {
            return mShouldRun;
        }

        /**
         * Requests that the rendering thread exit at the next opportunity.
         */
        public synchronized void quit() {
            mShouldRun = false;
        }

        @Override
        public void run() {
            while (shouldRun()) {
                long frameStart = SystemClock.elapsedRealtime();
                repaint();
                long frameLength = SystemClock.elapsedRealtime() - frameStart;

                long sleepTime = FRAME_TIME_MILLIS - frameLength;
                if (sleepTime > 0) {
                    SystemClock.sleep(sleepTime);
                }
            }
        }
    }
}
