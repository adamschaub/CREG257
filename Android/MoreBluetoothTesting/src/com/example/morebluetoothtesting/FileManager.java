package com.example.morebluetoothtesting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;

public class FileManager {
	
	private int KEY_SIZE = 32;
	private int ADDR_SIZE = 17;	// it's in the format AA:BB:CC:DD:EE:FF

	private String CLASS_NAME = this.getClass().getSimpleName();
	
	private Context context;
	
	public FileManager(Context c) {
		context = c;
	}
	
	/* Takes in a byte array, which will have been read from
	 * a .phnky attachment file by KeyPassFileOpener.
	 * 
	 * Creates a new internal file for the data, copies the data in.
	 */
	public void storeData(byte data[]) {
		/* Create a File object for our directory, actually create the directory if it doesn't exist yet. */
		File lockDir = new File(context.getFilesDir(),  "lockdir");
		lockDir.mkdirs();
		
		/* Create the file, which is one plus the current number of files, with the .phnky extension */
		File file = new File(context.getFilesDir() + File.separator + "lockdir", (lockDir.listFiles().length + 1) + ".phnky");		
		FileOutputStream outputStream;

		/* Write the data to the file */
		try {
    		outputStream = new FileOutputStream(file);
    		outputStream.write(data);
    		outputStream.close();
		} catch (Exception e) { Log.e("Error!", e.toString()); }
	}

	/* Reads each stored key/addr file and builds a HashMap out of them. */
	public HashMap<String, String> getKeyAddrMap() {
		File lockDir = new File(context.getFilesDir(),  "lockdir");
		lockDir.mkdirs();
		
		HashMap<String, String> keyAddrMap = new HashMap<String, String>();
		FileInputStream fileIn;
		byte buffer[] = new byte[KEY_SIZE+ADDR_SIZE];
		String lockAddr = "";
		String key = "";
		
		for (File f : lockDir.listFiles()) {
			try {
				fileIn = new FileInputStream(f);
				fileIn.read(buffer);
				fileIn.close();
			} catch (Exception e) { Log.e("Error!", e.toString()); }
		
			try {
				key = new String(buffer, 0, KEY_SIZE, "ASCII");
				lockAddr = new String(buffer, KEY_SIZE, ADDR_SIZE, "ASCII");
			} catch (Exception e) { Log.e("Error!", e.toString()); }
		
			keyAddrMap.put(lockAddr, key);
		
			Log.v(CLASS_NAME, f.toString());
		}
		
		if (lockDir.listFiles().length == 0)
			Log.v(CLASS_NAME, "No files!");
	
		return keyAddrMap;
	}
}
