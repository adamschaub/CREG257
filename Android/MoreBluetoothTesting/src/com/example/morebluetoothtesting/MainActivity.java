package com.example.morebluetoothtesting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

//XXX: Use services for persistant connections

public class MainActivity extends Activity {
	
	private byte encryptedPacket[] = {};
	
	List<KeyPassData> keyPassList = null;
	KeyPassData currentKpd = null;
	FileManager fm = null;
	
	private BluetoothConnection btConnection = null;
	 
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/* Load list of known keys/passes/ids */
		fm = new FileManager(getBaseContext());
		keyPassList = fm.readData();
		if (keyPassList.size() > 0) {
			currentKpd = keyPassList.get(0);
		
			Log.v("Passcode:", currentKpd.passcode);
			Log.v("lock name:", currentKpd.lockName);
		}

        Button initButton = (Button) findViewById(R.id.initButtonId);
        initButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (btConnection == null) {
					btConnection = new BluetoothConnection(currentKpd.lockName);
					btConnection.connect();
				}
				
				byte initSeq[] = {'1', '2', '3', '4', '5', '6', '7', '8' };
				btConnection.write(initSeq);
				
				btConnection.write32(currentKpd.passcode.getBytes());
				btConnection.write(currentKpd.key);
			}
		});
        
        Button lockButton = (Button) findViewById(R.id.lockButtonId);
        lockButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (btConnection == null) {
					btConnection = new BluetoothConnection(currentKpd.lockName);
					btConnection.connect();
				}

				/* Send the encrypted passcode */
				try {
					encryptedPacket = encryptData(currentKpd.key, currentKpd.passcode.getBytes("ASCII"), encryptedPacket);
				} catch (Exception e) { Log.e("Error!", e.toString()); }
				
				btConnection.write(encryptedPacket);
				
				/* Send the encrypted lock command */
				try {
					encryptedPacket = encryptData(currentKpd.key, "asdasd".getBytes("ASCII"), encryptedPacket);
				} catch (Exception e) { Log.e("Error!", e.toString()); }
				
				btConnection.write(encryptedPacket);
			}
		});
    
        Button unlockButton = (Button) findViewById(R.id.unlockButtonId);
        unlockButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (btConnection == null) {
					btConnection = new BluetoothConnection(currentKpd.lockName);
					btConnection.connect();
				}
				
				/* Send the encrypted passcode */
				try {
					encryptedPacket = encryptData(currentKpd.key, currentKpd.passcode.getBytes("ASCII"), encryptedPacket);
				} catch (Exception e) { Log.e("Error!", e.toString()); }
				
				btConnection.write(encryptedPacket);
				
				/* Send the encrypted unlock command */
				try {
					encryptedPacket = encryptData(currentKpd.key, "dsadsa".getBytes("ASCII"), encryptedPacket);
				} catch (Exception e) { Log.e("Error!", e.toString()); }
				
				btConnection.write(encryptedPacket);
			}
		});
        
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
	
	public void onResume() {
		super.onResume();
		
		if (keyPassList == null || currentKpd == null) {
			keyPassList = fm.readData();
			if (keyPassList.size() > 0) {
				currentKpd = keyPassList.get(0);
			
				Log.v("Passcode:", currentKpd.passcode);
				Log.v("lock name:", currentKpd.lockName);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
