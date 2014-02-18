package com.example.magnetometer;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	private Sensor mag;
	private double prevRMS = 0;
	private int prevLogicalVal = 0;
	private double prevTS;
	private boolean logging = false;
	private int currentPeriod = 20000;
	private float freqSum = 0;
	private int numSamples = 0;
	private float jitSum = 0;
	
	private TextView binaryVal, freqText, avgFreqText, avgJitText, periodText;
	
	private int state = 0;
	private int bitsRecieved = 0;
	private int inByte = 0;
	private int numInTriplet = 0;
	private int triplet[] = new int [3];
	
	private int prevVal = -1;
	private int errors = 0;
	private int noterrors = 0;
	private int prevnoterrors = 0;
	private int preverrors = 0;
	private int currentByte;
	private byte string[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H' };
	
	private int bitcount = 0;
	
	byte key[] =
		{
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		    0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
		};
	
	byte encryptedData [] = new byte[16];
	byte decryptedData [];
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        if ((mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) != null) {
        	System.out.println("Success! There is a magnetometer!");
        	sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_FASTEST);
        	currentPeriod = 100;
        	//sensorManager.registerListener(this, mag, currentPeriod);
        } else {
        	System.out.println("Failure! There is no magnetometer!");
        }
        
        binaryVal = (TextView)findViewById(R.id.binaryValText);
		freqText = (TextView)findViewById(R.id.freqText);
		avgFreqText = (TextView)findViewById(R.id.avgFreqText);
		periodText = (TextView)findViewById(R.id.periodText);
		avgJitText = (TextView)findViewById(R.id.avgJitText);
        
        Button loggingButton = (Button) findViewById(R.id.loggingButton);
        loggingButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				logging = !logging;
			}
		});
    }
    
    int z = 0;

	@Override
	public void onSensorChanged(SensorEvent event) {
		int logicalVal;
		double rms, freq, avgFreq;
		float avgJit = 0;
		
		if (!logging)
			return;
		
		/* Calculate frequency, average frequency, average frequency jitter */
		freq = 1000000000/(event.timestamp - prevTS);
		prevTS = event.timestamp;
		freqSum += freq;
		numSamples++;
		avgFreq = freqSum/numSamples;
		if (numSamples != 1) {
			jitSum += Math.abs(avgFreq - freq);
			avgJit = jitSum/(numSamples-1);
		}
		
		/* Calculate RMS of the values on the three magnetometer axes */
		rms = Math.sqrt((double)((event.values[0]*event.values[0])
							+ (event.values[1]*event.values[1])
							+ (event.values[2]*event.values[2])));
		
		/* Determine what the logical value is.
		 * XXX: There's got to be a better way to do this.
		 */
		if (rms < (prevRMS-2))	//XXX was prevRMS-2 (should be +???)
			logicalVal = 0;
		else if ((rms-2) > prevRMS)	//XXX was rms-2
			logicalVal = 1;
		else
			logicalVal = prevLogicalVal;	// no significant change, stay at current logical level
		
		prevLogicalVal = logicalVal;
		prevRMS = rms;
		
		/* Data transfer starts with preamble of 10101010.  When the preamble
		 * has been completely received, we expect the next 32 bits
		 * will constitute four bytes (lowest bit first).  Then, go back to
		 * state=0 and wait for another preamble.  */
		
		//z++;
		if (z>34*25) {
			binaryVal.setText("");
			z = 0;
		}
		//binaryVal.setText(binaryVal.getText() + "" + logicalVal);
		if (state == 16) {
				inByte >>= 1;
				inByte |= (logicalVal << 7);
				bitsRecieved++;
				
				int expectedVal = (string[(bitsRecieved-1)/8] >> ((bitsRecieved-1)%8)) & 0x1;
				if (expectedVal != logicalVal)
					errors++;
				else
					noterrors++;
				
				if ((bitsRecieved % 8) == 0) {
					//binaryVal.setText(binaryVal.getText() + "" + String.format("%c", (char) inByte));
					//binaryVal.setText(binaryVal.getText() + "" + (char) inByte);
					encryptedData[(bitsRecieved/8)-1] = (byte) inByte;
					//binaryVal.setText(binaryVal.getText() + "" + (char) inByte);
					inByte = 0;
					if (bitsRecieved == (8*8)) {
						bitsRecieved = 0;
						state = 0;
						/*try {
							decryptedData = decrypt(key, encryptedData);
						} catch (Exception e) { e.printStackTrace(); }*/
						if (binaryVal.getText().length() > 34*25)
							binaryVal.setText("");
						binaryVal.setText(binaryVal.getText() + " ");
						try {
					//		binaryVal.setText(binaryVal.getText() + new String(encryptedData, 0, 16, "ASCII"));
						} catch (Exception e) { Log.e("Error!", e.toString()); }
						
						for (int i=0; i<8; i++) {
							//binaryVal.setText(binaryVal.getText() + "" + (char) decryptedData[i]);
							binaryVal.setText(binaryVal.getText() + "" + String.format("%c", (encryptedData[i] & 0xff)));
						}
						try {
							Log.v("Errors:", errors+":"+noterrors);
							//Log.v("Here!", new String(encryptedData, 0, 16, "ASCII"));
						} catch (Exception e) { Log.e("Error!", e.toString()); }
					}
				}
			//}
		} else {
			if (((state % 2) == 0 && logicalVal == 1) || ((state % 2) == 1 && logicalVal == 0)) {
				state++;
				if (state == 16) {
					preverrors = errors;
					prevnoterrors = noterrors;
					errors = 0;
					noterrors = 0;
				}
			}
			else
				state = 0;
		}
		
		/* Update all the textboxes */
		freqText.setText("Freq (Hz):          " + freq);
		avgJitText.setText("Avg Jitter(Hz):  " + avgJit);
		avgFreqText.setText("Avg Freq (Hz):  " + avgFreq);
		//periodText.setText("Period (us):      " + currentPeriod);
		//periodText.setText("Errors      " + errors + " Good bits: " + noterrors);
		periodText.setText(errors + " " + noterrors + " PrevE: " + preverrors + " PrevNE: " + prevnoterrors);
	}
	
	private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {}
}
