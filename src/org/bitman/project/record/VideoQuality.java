package org.bitman.project.record;

import android.content.res.Resources;
import android.media.MediaRecorder;
import org.bitman.project.ProjectApplication;
import org.bitman.project.R;

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

    
    public String getDescription(int rtpPort) {

        String[] parameter = getParameter().split(",");

        StringBuffer sb = new StringBuffer();
        sb.append("m=video "+String.valueOf(rtpPort)+" RTP/AVP 96\r\n" +
                "a=rtpmap:96 H264/90000\r\n" +
                "a=fmtp:96 packetization-mode=1;");
        sb.append("profile-level-id="+parameter[0]);
        sb.append(";sprop-parameter-sets="+parameter[1]+","+parameter[2]+";\r\n");
        return sb.toString();
    }

    private String getParameter() {
        Resources resources = ProjectApplication.instance.getResources();
        if (framerate == 8)
        {
            switch (resX) {
                case 176:
                    return resources.getString(R.string.h8_176_144);
                case 320:
                    return resources.getString(R.string.h8_320_240);
                case 640:
                    return resources.getString(R.string.h8_640_480);
                default:
                    return null;
            }
        }
        else {
            switch (resX) {
                case 176:
                    return resources.getString(R.string.h10_176_144);
                case 320:
                    return resources.getString(R.string.h10_320_240);
                case 640:
                    return resources.getString(R.string.h10_640_480);
                default:
                    return null;
            }
        }
    }

}
