package org.bitman.project.ui;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.bitman.project.R;
import org.bitman.project.networkmiscellaneous.RTSP_Client;

import java.io.File;
import java.io.RandomAccessFile;

public class PlayerActivity extends Activity{
	public static final String TAG = "PlayerActivity";

	private SurfaceView playerSurface;
	private SurfaceHolder playerHolder;
	private TextView timeText;
	private MediaPlayer player;
	private String playAddr;
	private Button startButton;
	
	private Context context;

    private File file;
    private RTSP_Client client;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setContentView(R.layout.player);
		
		playerSurface = (SurfaceView) findViewById(R.id.playerSurface);
		timeText = (TextView) findViewById(R.id.timeText);
		startButton = (Button) findViewById(R.id.startButton);
		
		playerHolder = playerSurface.getHolder();
		playerHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		player = new MediaPlayer();

        playAddr = getIntent().getStringExtra("play_address");
		
		//playAddr = this.getIntent().getStringExtra("rtspAddr");
//		playAddr = "rtsp://121.49.83.42:8554/";
		
		// open the rtsp client.
		//TODO check it .
		/*try {
			testFile = new File(Environment.getExternalStorageDirectory()+"/test.sdp");
			sdpFile = new RandomAccessFile(testFile, "rw");
			
			Log.i("sdpfile", "ok");
			
			client = new RTSP_Client(playAddr, sdpFile);
			
			Log.i("client", "ok");
			
			client.Play();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
			*/


        final String path = getIntent().getStringExtra("play_address");

        new Thread(new Runnable() {
            @Override
            public void run() {
                file = new File(getExternalCacheDir(), "sdpfile.sdp");
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
		
//		startButton.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View arg0) {
//
//				try {
//					player.setDisplay(playerHolder);
//					//player.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.sdp");
//					player.setDataSource(playAddr);
//					//player.setDataSource(Environment.getExternalStorageDirectory()+"/test.sdp");
//					//player.prepare();
////					player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
////
////						@Override
////						public void onPrepared(MediaPlayer mp) {
////
////							playedTime = 0;
////							//new Thread(timeUpdate).start();
////
////							Toast.makeText(context, "...", Toast.LENGTH_LONG).show();
////							Log.i("Player", "start to Play");
////							player.start();
////						}
////					});
//					player.prepare();
//                    player.start();
//				} catch (Exception e)
//				{
//					Log.e(TAG, e.toString());
//				}
//
//			}
//		});
		
	}

    private Handler playHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                player.setDisplay(playerHolder);
                player.setDataSource(file.getAbsolutePath());
                //player.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.sdp");
//                player.setDataSource(playAddr);
                //player.setDataSource(Environment.getExternalStorageDirectory()+"/test.sdp");
                //player.prepare();
//					player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//
//						@Override
//						public void onPrepared(MediaPlayer mp) {
//
//							playedTime = 0;
//							//new Thread(timeUpdate).start();
//
//							Toast.makeText(context, "...", Toast.LENGTH_LONG).show();
//							Log.i("Player", "start to Play");
//							player.start();
//						}
//					});
                player.prepare();
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
			changeSize.sendMessage(new Message());
			disableButton.sendMessage(new Message());
			while (true)
			try {
				Thread.sleep(1000);
				playedTime++;
				changeTime.sendMessage(new Message());
			} catch (Exception e) {
				
			}
		}
	};
	
	private Handler disableButton = new Handler() {
		@Override
		public void handleMessage(Message msg)
		{
			startButton.setEnabled(false);
			startButton.setVisibility(Button.INVISIBLE);
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
	protected void onDestroy() {
		
		if (player.isPlaying())
			player.stop();
		
		// TODO open it
		// delete the dummy file.
		/*try {
			sdpFile.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		testFile.delete();
		*/

		Log.i(TAG, "kill myself");
		Process.killProcess(Process.myPid());
		
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		
		onDestroy();
	}
	
	

}
