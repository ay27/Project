package org.bitman.project.record;

import android.media.MediaRecorder;

public class VideoQuality {

    private static final String TAG = "VideoQuality";

    public int framerate = 8;
    public int resX = 176, resY = 144;
    public int bitrate = 100;
    public final int encoder = MediaRecorder.VideoEncoder.H264;
    public final int orientation = 90;

    private static VideoQuality instance;

    /**
     * The default quality.
     */
    private VideoQuality() {
        this.framerate = 8;
        this.resX = 176;
        this.resY = 144;
        this.bitrate = 100;
    }

    public static VideoQuality getInstance()
    {
        if (instance == null)
            return (instance = new VideoQuality());
        else return instance;
    }

    private static final String s640_10 = "";
    private static final String s640_8 = "";
    private static final String s320_10 = "";
    private static final String s320_8 = "";
    private static final String s176_10 = "";
    private static final String s176_8 = "";

    // TODO finish it
    public String getDescription() {
        return null;
    }
}
