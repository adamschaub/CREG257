package com.example.morebluetoothtesting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

//XXX: Use services for persistant connections

public class MainActivity extends Activity {
	
	
	/* XXX: Create a datastructure for storing this lock info, eventually load from file/DB */
	String knownLocks[] = { "RNBT-A70E" };
	String passCodes[] = { "yoopenthedoor" };
	
	byte keys[][] =
		{{
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		}};
	 
	private byte[] encryptData(byte[] key, byte[] data, byte[] encryptedData) {
			byte[] encrypted = {};
			Cipher cipher = null;
	        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
	        
	        try {
	        	cipher = Cipher.getInstance("AES/ECB/NoPadding");
	        } catch (Exception e) { Log.e("Error!", e.toString()); }
	        try {
	        	cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			} catch (Exception e) { Log.e("Error!", e.toString()); }
	        try {
	        	encrypted = cipher.doFinal(data);
			} catch (Exception e) { Log.e("Error!", e.toString()); }
	        
	        try {
		        Log.v("Raw: ", new String(data, "ASCII"));
		        Log.v("Encrypted: ", new String(encrypted, "ASCII"));
	        } catch (Exception e) { Log.e("Error!", e.toString()); }
	        
	        encryptedData = encrypted;
	        
	        return encrypted;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final byte encryptedPacket[] = {};

		Button loggingButton = (Button) findViewById(R.id.button1);
        loggingButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BluetoothConnection btConnection = new BluetoothConnection(knownLocks[0]);
				btConnection.connect();
				try {
					encryptData(keys[0], passCodes[0].getBytes("ASCII"), encryptedPacket);
				} catch (Exception e) { Log.e("Error!", e.toString()); }
				btConnection.write(encryptedPacket);
				/*IntentFilter filter = new IntentFilter();
				filter.addAction(BluetoothDevice.ACTION_FOUND);
				filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
				filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

				registerReceiver(btConnection.mReceiver, filter);*/
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
