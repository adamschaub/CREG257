package com.example.morebluetoothtesting;

import java.io.File;
import org.apache.http.client.CookieStore;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

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
		
		findViewById(R.id.registerLink).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent (thisActivity, RegisterActivity.class));
			}
		});

		final Button loginButton = (Button)findViewById(R.id.loginButton);
		loginButton.setOnClickListener(new View.OnClickListener () {
			public void onClick(View v) {
				try {
					WebRequest loginReq = new WebRequest();
					CookieStore sessionCookie;

					/* Todo: progress bar like thing */
					String res = loginReq.execute("http://phone-key-website.herokuapp.com/mobile-login",
							"username", ((TextView)findViewById(R.id.usernameField)).getText().toString(),
							"password", ((TextView)findViewById(R.id.passwordField)).getText().toString()).get(10000, TimeUnit.MILLISECONDS);

					sessionCookie = loginReq.getCookieStore();

					JSONObject jsonRes = new JSONObject(res);
					if (jsonRes.has("loggedIn"))
						Log.v("Res loggedIn", jsonRes.getBoolean("loggedIn") + "");
					if (jsonRes.has("message"))
						Log.v("Res message", jsonRes.getString("message"));
					if (jsonRes.has("username"))
						Log.v("Res username", jsonRes.getString("username"));
					if (jsonRes.has("id"))
						Log.v("Res id", jsonRes.getString("id"));

					/* Make sure the keyboard goes away after displaying results */
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(loginButton.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);

					if (jsonRes.has("loggedIn") && jsonRes.getBoolean("loggedIn")) {
						Log.v("dsa", "Logged in");
						if (jsonRes.has("update") && jsonRes.getBoolean("update")) {	// need to issue update with MAC addr
							String updateRes = new WebRequest(sessionCookie).execute("http://phone-key-website.herokuapp.com/update/" + jsonRes.getString("username"),
									"MAC", BluetoothAdapter.getDefaultAdapter().getAddress()).get();
							Log.v("asd", updateRes);
						}
					}
					else {
						((TextView)findViewById(R.id.usernameField)).setText("");
						((TextView)findViewById(R.id.passwordField)).setText("");
						findViewById(R.id.invalidLabel).setVisibility(View.VISIBLE);
					}
				} catch (Exception e) { e.printStackTrace(); }
			}
		});

		/*Button initButton = (Button) findViewById(R.id.initButtonId);
		initButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BluetoothConnection btConnection = BluetoothConnection.getInstance();

				/* Stop the service, otherwise it'll see the BT connect and try starting the auth sequence *
				stopService(mainIntent);

				btConnection.connect();
				btConnection.write("ABCDEFGH".getBytes());
				btConnection.write("12345678123456781234567812345678\r".getBytes());

				startService(mainIntent);
			}
		});*

		Button lockButton = (Button) findViewById(R.id.lockButton);
		lockButton.setOnClickListener(new View.OnClickListener () {
			public void onClick(View v) {
				try {
					//String res = new WebRequest().execute("lock request").get();
					String res = new WebRequest().
							execute("http://phone-key-website.herokuapp.com/mobile-login", "username", "mam21", "password", "asd").get();
					JSONObject jsonRes = new JSONObject(res);
					if (jsonRes.has("loggedIn"))
						Log.v("Res loggedIn", jsonRes.getBoolean("loggedIn") + "");
					if (jsonRes.has("message"))
						Log.v("Res message", jsonRes.getString("message"));
					if (jsonRes.has("username"))
						Log.v("Res username", jsonRes.getString("username"));
					if (jsonRes.has("id"))
						Log.v("Res id", jsonRes.getString("id"));
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

		Button deleteButton = (Button) findViewById(R.id.delButtonId);
		deleteButton.setOnClickListener(new View.OnClickListener () {
			public void onClick(View v) {
				File lockDir = new File(getBaseContext().getFilesDir(),  "lockdir");
				lockDir.mkdirs();

				for (File f : lockDir.listFiles()) {
					f.delete();
				}
			}
		});*/
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
