package com.example.morebluetoothtesting;

public class KeyPassData {
	public byte [] key;
	public String passcode;
	public String lockName;
	
	public KeyPassData() {
		
	}
	
	/* Instead of passing in the key, just pass in the raw buffer read in from
	 * the file.  Then, copy the first 32 bytes of that to the key here.
	 */
	public KeyPassData(byte buffer[], String passcode, String lockName) {
		key = new byte[32];
		for (int i=0; i<32; i++)
			key[i] = buffer[i];
		this.passcode = passcode;
		this.lockName = lockName;
	}
	
	public void generateNewCombo(String lockName) {
		key = new byte[32];
		for (int i=0; i<32; i++)
			key[i] = (byte) i;
		passcode = "yoopenthedoor";
		this.lockName = lockName;
	}
}