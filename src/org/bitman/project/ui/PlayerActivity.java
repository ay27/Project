package org.bitman.project.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.bitman.project.R;
import org.bitman.project.http.AsyncInetClient;
import org.bitman.project.networkmiscellaneous.RTSP_Client;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

public class PlayerActivity extends Activity{
	public static final String TAG = "PlayerActivity";

	private SurfaceView playerSurface;
	private SurfaceHolder playerHolder;
	private TextView timeText;
	private MediaPlayer player;

    private File file;
    private RTSP_Client client;

    private ProgressDialog pd;

    private boolean isPlaying = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.player);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

		playerSurface = (SurfaceView) findViewById(R.id.playerSurface);
		timeText = (TextView) findViewById(R.id.timeText);

		playerHolder = playerSurface.getHolder();
		playerHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		player = new MediaPlayer();

        final String path = getIntent().getStringExtra("play_address");

        pd = ProgressDialog.show(this, "waitting", "please wait until the video arrive...", true, true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                file = new File(getExternalCacheDir(), UUID.randomUUID().toString().substring(0, 5)+".sdp");
                try {
                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                    client = new RTSP_Client(path, randomAccessFile);
                    client.Play();
                    playHandler.sendEmptyMessage(1);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cancelHandler.sendEmptyMessage(1);
            }
        }).start();
	}

    private Handler cancelHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!isPlaying) {
                Toast.makeText(PlayerActivity.this, getString(R.string.cannotPlayTheVideo), Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        }
    };

    private Handler playHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                player.setDisplay(playerHolder);
                player.setDataSource(file.getAbsolutePath());
                new Thread(timeUpdate).start();
                player.prepareAsync();
                player.start();
            } catch (Exception e)
            {
                Log.e(TAG, e.toString());
            }

        }
    };
	
	private int playedTime = 0;
	private Runnable timeUpdate = new Runnable() {
		
		@Override
		public void run() {
			while (player.getVideoWidth() == 0)
			{
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
            isPlaying = true;
            pd.dismiss();
			changeSize.sendMessage(new Message());
			while (true)
			try {
				Thread.sleep(1000);
				playedTime++;
				changeTime.sendMessage(new Message());
			} catch (Exception e) {
				
			}
		}
	};
	
	// change the surface size
	private Handler changeSize = new Handler() {
		@Override
		public void handleMessage(Message msg)
		{
			playerHolder.setFixedSize(player.getVideoWidth(), player.getVideoHeight());
			RelativeLayout.LayoutParams lp = 
					new RelativeLayout.LayoutParams(player.getVideoWidth(), player.getVideoHeight());
			lp.width = player.getVideoWidth();
			lp.height = player.getVideoHeight();
			playerSurface.setLayoutParams(lp);
			playerSurface.invalidate();
		}
	};
	
	private Handler changeTime = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			
			String time = ""+(playedTime/60/60)+":"+(playedTime/60)+":"+(playedTime%60);
			timeText.setText(time);
			
		}
		
	};

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.onBackPressed();
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onDestroy();
    }

    @Override
    protected void onDestroy() {

        AsyncInetClient.getInstance().close(AsyncInetClient.Type.PlayFile, null);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        client.Teardown();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        if (player.isPlaying())
            player.stop();

        super.onDestroy();
    }

}
