
package com.example.morebluetoothtesting;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

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
	
	/* Receives Bluetooth related messages (e.g. when BT is connected) */
	public final BroadcastReceiver btReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
				Log.v(CLASS_NAME, "Connected to BT");
				doAuthSeq();
			}
		}
	};

	private byte[] listenForMIChallenge() {
		synchronized (magListener) {
			try {
				magListener.wait();
			} catch (Exception e) { Log.e("Exception!", e.toString()); }
		}
		byte micData[] = magListener.getData();
		try {
			Log.v(CLASS_NAME, new String(micData, 0, 8, "ASCII"));
		} catch (Exception e) { Log.e("Error!", e.toString()); }
		return micData;
	}

	private void doAuthSeq() {
		int response = BluetoothConnection.RESPONSE_NONE;

		boolean miDataOK = false;
		byte miData[] = new byte[8];

		/* Keep looping until we get an ACK back from door, meaning our response
		 * to the MI challenge was good.
		 */
		while (response != BluetoothConnection.RESPONSE_ACK) {

			/* Loop until we have a possibly valid MI challenge.
			 * Challenge comes from door, so it will be encrypted
			 */
			do {
				miDataOK = true;
				miData = listenForMIChallenge();
				for (int i=0; i<8; i++) {
					if (miData[i] > 'z' || miData[i] < 'A')	{	// Not A-Za-z, not valid
						miDataOK = false;
						break;
					}
				}
			} while (!miDataOK);

			/* Reply to the door with the challenge we received, and wait for an ACK or NAK. */
			try {
				btConnection.write(encryptData("12345678123456781234567812345678".getBytes("ASCII"), miData, new byte[16]));
			} catch (Exception e) { Log.e("Exception!", e.toString()); }
			synchronized (btConnection.mConnectedThread) {
				try {
					btConnection.mConnectedThread.wait();
				} catch (Exception e) { Log.e("Exception!", e.toString()); }
			}
			response = btConnection.getResponse();
		}

		/* MI challenge passed, so we're good to send our command: */
		Log.v(CLASS_NAME, "Sending unlock command");
		btConnection.write("dsadsa\r".getBytes());
	}

	private byte[] encryptData(byte[] key, byte[] data, byte[] encryptedData) {
		byte[] encrypted = {};
		byte[] paddedData = new byte [data.length + 16 - data.length%16];	// pad to 16 byte block size

		Cipher cipher = null;
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        System.arraycopy(data, 0, paddedData, 0, data.length);

        try {
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
		} catch (Exception e) { Log.e("Error!", e.toString()); }
		try {
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		} catch (Exception e) { Log.e("Error!", e.toString()); }
		try {
			encrypted = cipher.doFinal(paddedData);
		} catch (Exception e) { Log.e("Error!", e.toString()); }

		encryptedData = encrypted;

		return encrypted;
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

	public class LocalBinder extends Binder {
		MainService getService() {
			return MainService.this;
		}
	}
}
