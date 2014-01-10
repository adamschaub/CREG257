
package com.example.morebluetoothtesting;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

public class MainService extends Service {

	private String CLASS_NAME = this.getClass().getSimpleName();

	private BluetoothConnection btConnection = null;
	private Magnetometer magListener = null;
	
	private void listenForMIChallenge() {
		synchronized (magListener) {
			try {
				magListener.wait();
			} catch (Exception e) { Log.e("Exception!", e.toString()); }
		}
		byte micData[] = magListener.getData();
		try {
			Log.v(CLASS_NAME, new String(micData, 0, 8, "ASCII"));
		} catch (Exception e) { Log.e("Error!", e.toString()); }
		btConnection.write("dsadsa\r".getBytes());
	}
	/* Receives Bluetooth related messages (e.g. when BT is connected) */

	public final BroadcastReceiver btReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
				Log.v(CLASS_NAME, "Connected to BT");
				listenForMIChallenge();
			}
		}
	};
   
	public class LocalBinder extends Binder {
		MainService getService() {
			return MainService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.v(CLASS_NAME, "Created");

		btConnection = new BluetoothConnection();
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		registerReceiver(btReceiver, filter);

		magListener = new Magnetometer();
		SensorManager sensorManager;
		Sensor mag;

		sensorManager = (SensorManager)getBaseContext().getSystemService(Context.SENSOR_SERVICE);
		if ((mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) != null) {
			Log.v(CLASS_NAME, "There is a magnetomter!");
			HandlerThread hThread = new HandlerThread("hThread");
			hThread.start();
			Handler h = new Handler(hThread.getLooper());
			sensorManager.registerListener(magListener, mag, SensorManager.SENSOR_DELAY_FASTEST, h);
		} else {
			Log.v(CLASS_NAME, "There is no magnetomter!");
		}
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(CLASS_NAME, "Started");

		btConnection.start();

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
