package com.example.magnetometer;

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
	private int numBits = 0;
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
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        if ((mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) != null) {
        	System.out.println("Success! There is a magnetometer!");
        	sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_FASTEST);
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
		if (state == 8) {
			inByte >>= 1;
			inByte |= (logicalVal << 7);
			bitsRecieved++;
			//binaryVal.setText(binaryVal.getText() + "" + logicalVal);
			if ((bitsRecieved % 8) == 0) {
				binaryVal.setText(binaryVal.getText() + "" + (char) inByte);
				inByte = 0;
				if (bitsRecieved == (8*4)) {
					bitsRecieved = 0;
					state = 0;
				}
			}
		} else {
			if (((state % 2) == 0 && logicalVal == 1) || ((state % 2) == 1 && logicalVal == 0))
				state++;
			else
				state = 0;
		}
		
		/* Update all the textboxes */
		freqText.setText("Freq (Hz):          " + freq);
		avgJitText.setText("Avg Jitter(Hz):  " + avgJit);
		avgFreqText.setText("Avg Freq (Hz):  " + avgFreq);
		periodText.setText("Period (us):      " + currentPeriod);
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
