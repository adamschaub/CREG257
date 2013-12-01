package com.example.morebluetoothtesting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class FileManager {
	
	private Context context;
	
	public FileManager(Context c) {
		context = c;
	}
	
	/* File format: 32 bytes of key, then a byte of the length of passcode, then
	 * bytes of passcode, then byte of length of lock name, then bytes of lock name
	 */
	public void storeData(byte key[], String passcode, String lockName) {
		int i;
		byte buffer[] = new byte[key.length + passcode.getBytes().length + lockName.getBytes().length + 2];
		for (i=0; i<key.length; i++)
			buffer[i] = key[i];
		buffer[i] = (byte) passcode.getBytes().length;
		for (i=0; i<passcode.getBytes().length; i++)
			buffer[i+1+key.length] = passcode.getBytes()[i];
		buffer[i] = (byte) lockName.getBytes().length;
		for (i=0; i<lockName.getBytes().length; i++)
			buffer[i+2+key.length+passcode.getBytes().length] = lockName.getBytes()[i];
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

	/* Reads each stored key/pass/id file and creates a new
	 * KeyPassData object, returns an ArrayList of the objects. 
	 */
	public List<KeyPassData> readData() {
		File lockDir = new File(context.getFilesDir(),  "lockdir");
		lockDir.mkdirs();
		
		List<KeyPassData> keyPassList = new ArrayList<KeyPassData>();
		KeyPassData kpd;
		FileInputStream fileIn;
		byte buffer[] = new byte[100];	// 100 bytes should be enough, right?
		byte passcodeLen, lockIdLen;
		String passcode = "", lockId = "";
		
		for (File f : lockDir.listFiles()) {
			try {
				fileIn = new FileInputStream(f);
				fileIn.read(buffer);
				fileIn.close();
			} catch (Exception e) { Log.e("Error!", e.toString()); }
		
			passcodeLen = buffer[32];
			lockIdLen = buffer[33+passcodeLen];
			try {
				passcode = new String(buffer, 33, passcodeLen, "ASCII");
				lockId = new String(buffer, 34+passcodeLen, lockIdLen, "ASCII");
			} catch (Exception e) { Log.e("Error!", e.toString()); }
		
			kpd = new KeyPassData(buffer, passcode, lockId);
			keyPassList.add(kpd);
		
			Log.v("Found lock file: ", f.toString());
		}
		
		if (lockDir.listFiles().length == 0)
			Log.v("FileManager:", "No files!");
	
		return keyPassList;
	}
}
