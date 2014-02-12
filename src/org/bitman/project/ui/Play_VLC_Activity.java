package org.bitman.project.ui;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.bitman.project.R;
import org.bitman.project.http.HttpClient;
import vlc.*;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * The VLC Player, I don't know that whether it can play the sdp-file.
 * @author ay27
 *
 */
public class Play_VLC_Activity extends Activity implements
        SurfaceHolder.Callback, View.OnClickListener {

    public final static String TAG = "DEBUG/Play_VLC_Activity";
    private static final int SURFACE_SIZE = 3;
    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private final Handler mHandler = new VideoPlayerHandler(this);
    private final Handler eventHandler = new VideoPlayerEventHandler(this);
    private SurfaceHolder surfaceHolder = null;
    private LibVLC mLibVLC = null;
    private int mVideoHeight;
    private int mVideoWidth;
    private int mSarDen;
    private int mSarNum;

    //private String[] mAudioTracks;
    private int mUiVisibility = -1;
    private int mCurrentSize = SURFACE_BEST_FIT;
    private SurfaceView surfaceView = null;
    private TextView mTextTitle;
    private TextView mTextTime;
    private ImageView btnSize;
    private TextView mTextShowInfo;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            int time = (int) mLibVLC.getTime();
            int length = (int) mLibVLC.getLength();
            showVideoTime(time, length);
            handler.sendEmptyMessageDelayed(0, 1000);
        }
    };

    /**
     * Convert time to a string
     *
     * @param millis
     *            e.g.time/length from file
     * @return formated string (hh:)mm:ss
     */
    public static String millisToString(long millis) {
        boolean negative = millis < 0;
        millis = java.lang.Math.abs(millis);

        millis /= 1000;
        int sec = (int) (millis % 60);
        millis /= 60;
        int min = (int) (millis % 60);
        millis /= 60;
        int hours = (int) millis;

        String time;
        DecimalFormat format = (DecimalFormat) NumberFormat
                .getInstance(Locale.US);
        format.applyPattern("00");
        if (millis > 0) {
            time = (negative ? "-" : "") + hours + ":" + format.format(min)
                    + ":" + format.format(sec);
        } else {
            time = (negative ? "-" : "") + min + ":" + format.format(sec);
        }
        return time;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_vlc_layout);
        setupView();

        if(Util.isICSOrLater())
            getWindow()
                    .getDecorView()
                    .findViewById(android.R.id.content)
                    .setOnSystemUiVisibilityChangeListener(
                            new View.OnSystemUiVisibilityChangeListener() {

                                @Override
                                public void onSystemUiVisibilityChange(
                                        int visibility) {
                                    if (visibility == mUiVisibility)
                                        return;
                                    setSurfaceSize(mVideoWidth, mVideoHeight,
                                            mSarNum, mSarDen);
                                    if (visibility == View.SYSTEM_UI_FLAG_VISIBLE) {
                                        Log.d(TAG, "onSystemUiVisibilityChange");
                                    }
                                    mUiVisibility = visibility;
                                }
                            });

        try {
            mLibVLC = LibVLC.getInstance();
            if (mLibVLC != null) {
                String pathUri=this.getIntent().getStringExtra("play_address");
                mLibVLC.readMedia(pathUri, false);
                handler.sendEmptyMessageDelayed(0, 1000);
            }
        } catch (LibVlcException e) {
            Log.e("play", e.toString());
        }

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnSize.getId()) {
            if (mCurrentSize < SURFACE_ORIGINAL) {
                mCurrentSize++;
            } else {
                mCurrentSize = 0;
            }
            changeSurfaceSize();
        }

    }

    private void setupView() {
        surfaceView = (SurfaceView) findViewById(R.id.main_surface);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.RGBX_8888);
        surfaceHolder.addCallback(this);
        mTextTitle = (TextView) findViewById(R.id.video_player_title);

        btnSize = (ImageView) findViewById(R.id.video_player_size);
        mTextTime = (TextView) findViewById(R.id.video_player_time);
        mTextShowInfo = (TextView) findViewById(R.id.video_player_showinfo);

        btnSize.setOnClickListener(this);

        mTextTitle.setText(getIntent().getStringExtra("name"));
    }

    private void showVideoTime(int t, int l) {
        mTextTime.setText(millisToString(t));
    }

        @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setSurfaceSize(mVideoWidth, mVideoHeight, mSarNum, mSarDen);
        super.onConfigurationChanged(newConfig);
    };

    public void setSurfaceSize(int width, int height, int sar_num, int sar_den) {
        // store video size
        mVideoHeight = height;
        mVideoWidth = width;
        mSarNum = sar_num;
        mSarDen = sar_den;
        Message msg = mHandler.obtainMessage(SURFACE_SIZE);
        mHandler.sendMessage(msg);
    }

    private void changeSurfaceSize() {
        // get screen size
        int dw = getWindow().getDecorView().getWidth();
        int dh = getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (dw > dh && isPortrait || dw < dh && !isPortrait) {
            int d = dw;
            dw = dh;
            dh = d;
        }
        if (dw * dh == 0)
            return;
        // compute the aspect ratio
        double ar, vw;
        double density = (double) mSarNum / (double) mSarDen;
        if (density == 1.0) {
					/* No indication about the density, assuming 1:1 */
            vw = mVideoWidth;
            ar = (double) mVideoWidth / (double) mVideoHeight;
        } else {
					/* Use the specified aspect ratio */
            vw = mVideoWidth * density;
            ar = vw / mVideoHeight;
        }

        // compute the display aspect ratio
        double dar = (double) dw / (double) dh;

//		// calculate aspect ratio
//		double ar = (double) mVideoWidth / (double) mVideoHeight;
//		// calculate display aspect ratio
//		double dar = (double) dw / (double) dh;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                mTextShowInfo.setText(R.string.video_player_best_fit);
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_FIT_HORIZONTAL:
                mTextShowInfo.setText(R.string.video_player_fit_horizontal);
                dh = (int) (dw / ar);
                break;
            case SURFACE_FIT_VERTICAL:
                mTextShowInfo.setText(R.string.video_player_fit_vertical);
                dw = (int) (dh * ar);
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                mTextShowInfo.setText(R.string.video_player_16x9);
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_4_3:
                mTextShowInfo.setText(R.string.video_player_4x3);
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = (int) (dw / ar);
                else
                    dw = (int) (dh * ar);
                break;
            case SURFACE_ORIGINAL:
                mTextShowInfo.setText(R.string.video_player_original);
                dh = mVideoHeight;
                dw = mVideoWidth;
                break;
        }

        surfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
        ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        lp.width = dw;
        lp.height = dh;
        surfaceView.setLayoutParams(lp);
        surfaceView.invalidate();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        mLibVLC.attachSurface(holder.getSurface(), Play_VLC_Activity.this,
                width, height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mLibVLC.detachSurface();
    }

@Override
    protected void onDestroy() {
        if (mLibVLC != null) {
            mLibVLC.stop();
        }

        EventManager em = EventManager.getInstance();
        em.removeHandler(eventHandler);

        super.onDestroy();

    // stop HttpClient
    HttpClient.getInstance().send(HttpClient.Options.Close, "");
    }

    private static class VideoPlayerHandler extends
            WeakHandler<Play_VLC_Activity> {
        public VideoPlayerHandler(Play_VLC_Activity owner) {
            super(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            Play_VLC_Activity activity = getOwner();
            if (activity == null) // WeakReference could be GC'ed early
                return;

            switch (msg.what) {
                case SURFACE_SIZE:
                    activity.changeSurfaceSize();
                    break;
            }
        }
    };

    private static class VideoPlayerEventHandler extends
            WeakHandler<Play_VLC_Activity> {
        public VideoPlayerEventHandler(Play_VLC_Activity owner) {
            super(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            Play_VLC_Activity activity = getOwner();
            if (activity == null)
                return;

            switch (msg.getData().getInt("event")) {
                case EventManager.MediaPlayerPlaying:
                    Log.i(TAG, "MediaPlayerPlaying");
                    break;
                case EventManager.MediaPlayerPaused:
                    Log.i(TAG, "MediaPlayerPaused");
                    break;
                case EventManager.MediaPlayerStopped:
                    Log.i(TAG, "MediaPlayerStopped");
                    break;
                case EventManager.MediaPlayerEndReached:
                    Log.i(TAG, "MediaPlayerEndReached");
                    activity.finish();
                    break;
                case EventManager.MediaPlayerVout:
                    activity.finish();
                    break;
                default:
                    Log.e(TAG, "Event not handled");
                    break;
            }
            // activity.updateOverlayPausePlay();
        }
    }



}
