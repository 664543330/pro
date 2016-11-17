package com.example.host.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends FragmentActivity {

	private TextView txt;

	private String msgStr;

	private DownloadView downloadView;
	private int mDownloadProgress = 0;

	public final static int MSG_UPDATE = 1;
	public final static int MSG_FINISHED = 2;

    private String url="http://wcong.top/download/plug.apk";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txt= (TextView) findViewById(R.id.txt);
		downloadView= (DownloadView) findViewById(R.id.dv);

		PlugProxyActivity.scan(new PCallback() {
			@Override
			public void scan(String str) {
				msgStr=str;
				handler.sendEmptyMessage(0);
			}
		});
	}

	private Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what){
				case 0:
					txt.setText(msgStr);
					break;
				case MSG_FINISHED:
					downloadView.setStatus(DownloadView.STATUS_FINISHED);
					break;
				case MSG_UPDATE:
					downloadView.setProgress(msg.arg1);
					break;
			}
			super.handleMessage(msg);
		}
	};

	public void loadActivity(View view) {

		if (new File(getFilesPath()+"/plug.apk").exists()){
			Intent intent = new Intent(this, PlugProxyActivity.class);
			intent.putExtra(PlugProxyActivity.KEY_APK,getFilesPath() + File.separator +"plug.apk");
			intent.putExtra(PlugProxyActivity.KEY_CLASS,"com.plug.scan.MipcaActivityCapture");
			startActivity(intent);

			getFilesPath();
		}else {
			dialog();
		}

	}

	public String getFilesPath() {
		File cacheDir;
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
			cacheDir = getExternalFilesDir("");
		else
			cacheDir = getFilesDir();
		if (!cacheDir.exists())
			cacheDir.mkdirs();
		return cacheDir.getAbsolutePath();
	}

	public void downFile(final String httpUrl,final Handler handler) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					File file = null;
					URL url = new URL(httpUrl);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("GET");
					connection.setConnectTimeout(5000);
					FileOutputStream fileOutputStream = null;
					InputStream inputStream;
					if (connection.getResponseCode() == 200) {
						inputStream = connection.getInputStream();

						if (inputStream != null) {
							file = new File(getFilesPath()+"/plug.apk");
							fileOutputStream = new FileOutputStream(file);
							byte[] buffer = new byte[1024];
							int length = 0;
							int total = 0;
							int max = connection.getContentLength();
							downloadView.setTotalProgress(max);
							while ((length = inputStream.read(buffer)) != -1) {
								fileOutputStream.write(buffer, 0, length);
								mDownloadProgress += length;
								Log.i("mDownloadProgress",mDownloadProgress+"");
								Log.i("max",max+"");
								Message msg = handler.obtainMessage();
								msg.what = MSG_UPDATE;
//                                msg.arg1 = (int)(total*100/max);
								msg.arg1 = mDownloadProgress;
								handler.sendMessage(msg);
//                                handler.sendEmptyMessage(MSG_UPDATE);
							}
							fileOutputStream.close();
							fileOutputStream.flush();
						}
						inputStream.close();
					}

					handler.sendEmptyMessage(MSG_FINISHED);
//                    Message message = handler.obtainMessage();
//                    message.what = LoadingActivity.DOWNLOAD_COMPLETE;
//                    handler.sendMessage(message);
//
//                    installApk(file);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void dialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage("无组件，是否现在下载");
		builder.setTitle("提示");
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				downloadView.setStatus(DownloadView.STATUS_DOWNLOADING);
				downFile(url,handler);}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}



}
