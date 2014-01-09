
package com.example.morebluetoothtesting;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MainService extends Service {
	
	private String CLASS_NAME = this.getClass().getSimpleName();

	private BluetoothConnection btConnection = null;
	
	/* Receives Bluetooth related messages (e.g. when BT is connected) */
	public final BroadcastReceiver btReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
				Log.v(CLASS_NAME, "Connected to BT");
				btConnection.write32("helloooo".getBytes());
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
