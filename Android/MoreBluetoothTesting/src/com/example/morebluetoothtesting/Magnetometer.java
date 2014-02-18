package com.example.morebluetoothtesting;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class Magnetometer extends HandlerThread implements SensorEventListener {

	private double prevRMS = 0;
	private int prevLogicalVal = 0;

	private int state = 0;
	private int bitsRecieved = 0;
	private int inByte = 0;
	
	private boolean ready = false;
	
	byte encryptedData [] = new byte[8];
	
	Handler handler = null;
	
	public Magnetometer() {
		super("MagnetometerHandler");
		start();
		handler = new Handler(getLooper());
	}
	
	public Handler getHandler() {
		return handler;
	}
	
    public synchronized boolean getReady() {
        return ready;
    }
    
    public synchronized void setReady() {
    	ready = true;
    }
    
    public synchronized void clearReady() {
    	ready = false;
    }
    
    public synchronized byte[] getData() {
    	return encryptedData;
    }

	@Override
	public void onSensorChanged(SensorEvent event) {
		int logicalVal;
		double rms;
		
		/* Calculate RMS of the values on the three magnetometer axes */
		rms = Math.sqrt((double)((event.values[0]*event.values[0])
							+ (event.values[1]*event.values[1])
							+ (event.values[2]*event.values[2])));
		
		/* Determine what the logical value is.
		 * XXX: There's got to be a better way to do this. */
		if (rms < (prevRMS-2))
			logicalVal = 0;
		else if ((rms-2) > prevRMS)
			logicalVal = 1;
		else
			logicalVal = prevLogicalVal;	// no significant change, stay at current logical level
		
		prevLogicalVal = logicalVal;
		prevRMS = rms;
	
		/* Data transfer starts with preamble of 10101010.  When the preamble
		 * has been completely received, we expect the next 32 bits
		 * will constitute four bytes (lowest bit first).  Then, go back to
		 * state=0 and wait for another preamble.  */
	
		if (state == 16) {
			inByte >>= 1;
			inByte |= (logicalVal << 7);
			bitsRecieved++;
			
			if ((bitsRecieved % 8) == 0) {
				//Log.v(CLASS_NAME, (char) inByte));
				encryptedData[(bitsRecieved/8)-1] = (byte) inByte;
				inByte = 0;
				if (bitsRecieved == (8*8)) {
					try {
						//Log.v(CLASS_NAME, new String(encryptedData, 0, 8, "ASCII"));
					} catch (Exception e) { Log.e ("Error!", e.toString()); }
					bitsRecieved = 0;
					state = 0;
					synchronized(this) {
						this.notify();
					}
				}
			}
		} else {
			if (((state % 2) == 0 && logicalVal == 1) || ((state % 2) == 1 && logicalVal == 0)) {
				state++;
			}
			else
				state = 0;
		}	
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
}
