package org.bitman.project.record;

import android.media.MediaRecorder;

public class VideoQuality {

    private static final String TAG = "VideoQuality";

    private int framerate = 8;
    private int resX = 176, resY = 144;
    private int bitrate = 100;
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

    public int getFramerate() { return framerate; }

    public void setFramerate(int framerate) { this.framerate = framerate; }

    public int getResX() { return resX; }

    public void setResX(int resX) { this.resX = resX; }

    public int getResY() { return resY; }

    public void setResY(int resY) { this.resY = resY; }

    public int getBitrate() { return bitrate; }

    public void setBitrate(int bitrate) { this.bitrate = bitrate; }
}
