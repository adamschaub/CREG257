package com.example.morebluetoothtesting;

public class KeyPassData {
	public byte [] key;
	public String lockAddr;	// MAC address of lock, in String format
	
	public KeyPassData() {}
	
	/* Instead of passing in the key, just pass in the raw buffer read in from
	 * the file.  Then, copy the first 32 bytes of that to the key here.
	 */
	public KeyPassData(byte buffer[], String lockAddr) {
		key = new byte[32];
		for (int i=0; i<32; i++)
			key[i] = buffer[i];
		this.lockAddr = lockAddr;
	}
	
	public void generateNewCombo(String lockAddr) {
		key = new byte[32];
		for (int i=0; i<32; i++)
			key[i] = (byte) i;
		this.lockAddr = lockAddr;
	}
}