package com.example.morebluetoothtesting;

import java.io.InputStream;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class KeyPassFileOpener extends Activity {
	
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		
		FileManager fm = new FileManager(getBaseContext());
		InputStream fileData;
		int bytesRead;
		byte buffer[] = new byte[100];
		
		Intent intent = getIntent ();
		try {
			fileData = getContentResolver().openInputStream(intent.getData());
			bytesRead = fileData.read(buffer);
			fm.storeData(buffer);
			Log.v("File contents:", new String(buffer, 0, bytesRead, "ASCII"));
		} catch (Exception e) { Log.e("Error!", e.toString()); }
	}
}