package com.example.morebluetoothtesting;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final Activity thisActivity = this;
		
		Button loggingButton = (Button) findViewById(R.id.button1);
        loggingButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent bluetoothIntent = new Intent(thisActivity, BluetoothDiscover.class);
	            startActivityForResult(bluetoothIntent, 0);
			}
		});
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			Log.v("Selected:", data.getExtras().getString("address"));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
