package com.example.morebluetoothtesting;

import java.io.File;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	private String CLASS_NAME = this.getClass().getSimpleName();
	
	Magnetometer magListener = null;
	
	private Intent mainIntent;
	private MainService mainService;
	private Activity thisActivity = this;
	
	private ServiceConnection con = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.v(CLASS_NAME, "Service connected");
			mainService = ((MainService.LocalBinder)service).getService();
		}
		
		public void onServiceDisconnected(ComponentName className) {
			mainService = null;
			Log.v(CLASS_NAME, "Service disconnected");
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mainIntent = new Intent(this, MainService.class);
		//bindService(mainIntent, con, Context.BIND_AUTO_CREATE);
		startService(mainIntent);
		
		Button initButton = (Button) findViewById(R.id.initButtonId);
		initButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BluetoothConnection btConnection = BluetoothConnection.getInstance();

				/* Stop the service, otherwise it'll see the BT connect and try starting the auth sequence */
				stopService(mainIntent);

				btConnection.connect();
				btConnection.write("ABCDEFGH".getBytes());
				btConnection.write("12345678123456781234567812345678\r".getBytes());

				startService(mainIntent);
			}
		});

		Button lockButton = (Button) findViewById(R.id.lockButton);
		lockButton.setOnClickListener(new View.OnClickListener () {
			public void onClick(View v) {
				try {
					String res = new WebRequest().execute("lock request").get();
				} catch (Exception e) { Log.e("Exception!", e.toString()); }
			}
		});

		Button unlockButton = (Button) findViewById(R.id.unlockButton);
		unlockButton.setOnClickListener(new View.OnClickListener () {
			public void onClick(View v) {
				try {
					String res = new WebRequest().execute("unlock request").get();
				} catch (Exception e) { Log.e("Exception!", e.toString()); }
			}
		});

		Button shareButton = (Button) findViewById(R.id.shareKeyButtonId);
		shareButton.setOnClickListener(new View.OnClickListener () {
			public void onClick(View v) {
				try {
					String res = new WebRequest().execute("share request").get();
				} catch (Exception e) { Log.e("Exception!", e.toString()); }
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
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.v(CLASS_NAME, "onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
