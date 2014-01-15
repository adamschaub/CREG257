package com.example.morebluetoothtesting;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

//XXX: Use services for persistant connections

public class MainActivity extends Activity {

	private String CLASS_NAME = this.getClass().getSimpleName();
	
	byte encryptedData [] = new byte[16];
	
	private byte encryptedPacket[] = {};
	
	Magnetometer magListener = null;
	List<KeyPassData> keyPassList = null;
	KeyPassData currentKpd = null;
	FileManager fm = null;
	
	private Intent mainIntent;
	private MainService mainService;
	
	private ServiceConnection con = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.v(CLASS_NAME, "Service connected");
			mainService = ((MainService.LocalBinder)service).getService();
		}
		
		public void onServiceDisconnected(ComponentName className) {
			mainService = null;
			Log.v(CLASS_NAME, "Service disconnected");
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mainIntent = new Intent(this, MainService.class);
		bindService(mainIntent, con, Context.BIND_AUTO_CREATE);
		startService(mainIntent);
		
		/* Load list of known keys/passes/ids */
		//fm = new FileManager(getBaseContext());
		//keyPassList = fm.readData();
		//if (keyPassList.size() > 0) {
		//	currentKpd = keyPassList.get(0);

//			sensorManager = (SensorManager)getBaseContext().getSystemService(Context.SENSOR_SERVICE);
//	        if ((mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) != null) {
//	        	Log.v(CLASS_NAME, "There is a magnetomter!");
//	        	//sensorManager.registerListener(magListener, mag, SensorManager.SENSOR_DELAY_FASTEST);
//	        	HandlerThread hThread = new HandlerThread("hThread");
//	        	hThread.start();
//	        	Handler h = new Handler(hThread.getLooper());
//	        	sensorManager.registerListener(magListener, mag, SensorManager.SENSOR_DELAY_FASTEST, h);
//	        } else {
//	        	Log.v(CLASS_NAME, "There is no magnetomter!");
//	        }
//			
//			Log.v(CLASS_NAME, currentKpd.passcode);
//			Log.v(CLASS_NAME, currentKpd.lockName);
//		}
//
//        Button initButton = (Button) findViewById(R.id.initButtonId);
//        initButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if (btConnection == null) {
//					btConnection = new BluetoothConnection(currentKpd.lockName);
//					btConnection.connect();
//				}
//				
//				/*byte initSeq[] = {'1', '2', '3', '4', '5', '6', '7', '8' };
//				btConnection.write(initSeq);
//				
//				btConnection.write32(currentKpd.passcode.getBytes());
//				btConnection.write(currentKpd.key);*/
//			}
//		});

		/* TODO: On lock/unlock buttons, need to message service and have that try to execute the command. */

        Button shareButton = (Button) findViewById(R.id.shareKeyButtonId);
        shareButton.setOnClickListener(new View.OnClickListener () {
        	public void onClick(View v) {
        		int i;
        		byte buffer[] = new byte[currentKpd.key.length + currentKpd.passcode.getBytes().length + currentKpd.lockName.getBytes().length + 2];
        		for (i=0; i<currentKpd.key.length; i++)
        			buffer[i] = currentKpd.key[i];
        		buffer[i] = (byte) currentKpd.passcode.getBytes().length;
        		for (i=0; i<currentKpd.passcode.getBytes().length; i++)
        			buffer[i+1+currentKpd.key.length] = currentKpd.passcode.getBytes()[i];
        		buffer[i+1+currentKpd.key.length] = (byte) currentKpd.lockName.getBytes().length;
        		for (i=0; i<currentKpd.lockName.getBytes().length; i++)
        			buffer[i+2+currentKpd.key.length+currentKpd.passcode.getBytes().length] = currentKpd.lockName.getBytes()[i];
        		
        		/* Create file in internal storage (i.e., it's private to our app) */
        		String filename = "tmpfile.phnky";
        		File file = new File(getBaseContext().getFilesDir(), filename);
        		FileOutputStream outputStream;

        		try {
	        		outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
	        		outputStream.write(buffer);
	        		outputStream.close();
        		} catch (Exception e) { Log.e("Error!", e.toString()); }
        		
        		/* Send email, with tmpfile.phnky attached */
        		Intent sendIntent;

        		sendIntent = new Intent(Intent.ACTION_SEND);
        		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Super secret email!");
        		sendIntent.putExtra(Intent.EXTRA_TEXT, "Super secret text!");
        		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://" + "com.example.morebluetoothtesting.provider" + "/" + file.getPath()));
        		sendIntent.setType("text/plain");

        		startActivity(Intent.createChooser(sendIntent, "Send Mail"));
        	}
        });
        
        Button deleteButton = (Button) findViewById(R.id.delButtonId);
        deleteButton.setOnClickListener(new View.OnClickListener () {
        	public void onClick(View v) {
        		File lockDir = new File(getBaseContext().getFilesDir(),  "lockdir");
        		lockDir.mkdirs();
        		
        		for (File f : lockDir.listFiles()) {
        			f.delete();
        		}
        	}
        });
        
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.v(CLASS_NAME, "onStart");
		//startService(btIntent);
	}

	@Override
	public void onResume() {
		super.onResume();
		
/*		if (keyPassList == null || currentKpd == null) {
			keyPassList = fm.readData();
			if (keyPassList.size() > 0) {
				currentKpd = keyPassList.get(0);
			
				Log.v(CLASS_NAME, currentKpd.passcode);
				Log.v(CLASS_NAME, currentKpd.lockName);
			}
		}*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
