package com.example.morebluetoothtesting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
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
	private String knownLocks[] = { "RNBT-A70E" };
	private String passCodes[] = { "yoopenthedoor" };
	
	private byte keys[][] =
		{{
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		}};
	
	private byte encryptedPacket[] = {};
	 
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
	        
	        try {
		        //Log.v("Raw: ", new String(data, "ASCII"));
		        //Log.v("Encrypted: ", new String(encrypted, "ASCII"));
	        } catch (Exception e) { Log.e("Error!", e.toString()); }
	        
	        encryptedData = encrypted;
	       
	        return encrypted;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button loggingButton = (Button) findViewById(R.id.button1);
        loggingButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BluetoothConnection btConnection = new BluetoothConnection(knownLocks[0]);
				btConnection.connect();
				
				try {
					encryptedPacket = encryptData(keys[0], passCodes[0].getBytes("ASCII"), encryptedPacket);
				} catch (Exception e) { Log.e("Error!", e.toString()); }
				
				Log.v("Encrypted:", new BigInteger(1, encryptedPacket).toString(16));
				btConnection.write(encryptedPacket);
				
				/*IntentFilter filter = new IntentFilter();
				filter.addAction(BluetoothDevice.ACTION_FOUND);
				filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
				filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
				registerReceiver(btConnection.mReceiver, filter);*/
			}
		});
       
        Button initButton = (Button) findViewById(R.id.button2);
        initButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BluetoothConnection btConnection = new BluetoothConnection(knownLocks[0]);
				btConnection.connect();
				
				byte initSeq[] = {'1', '2', '3', '4', '5', '6', '7', '8' };
				btConnection.write(initSeq);
				btConnection.write32(passCodes[0].getBytes());
				btConnection.write(keys[0]);
				
				try {
					encryptedPacket = encryptData(keys[0], passCodes[0].getBytes("ASCII"), encryptedPacket);
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
