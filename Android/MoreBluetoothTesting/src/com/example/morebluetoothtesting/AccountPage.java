package com.example.morebluetoothtesting;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AccountPage extends Activity {

	CookieStore sessionCookie = null;
	boolean closed = false, disabled = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accountpage_activity_layout);

		final Button lockUnlockButton = ((Button)findViewById(R.id.lockUnlockButton));
		final Button enableDisableButton = ((Button)findViewById(R.id.enableDisableButton));

		Bundle extras = getIntent().getExtras();
		String[] cookieNames = extras.getStringArray("cookieNames");
		String[] cookieValues = extras.getStringArray("cookieValues");
		String[] cookieDomains = extras.getStringArray("cookieDomains");
		sessionCookie = new DefaultHttpClient(new BasicHttpParams()).getCookieStore();
		for (int i=0; i<cookieNames.length; i++) {
			BasicClientCookie cookie = new BasicClientCookie(cookieNames[i], cookieValues[i]);
			cookie.setDomain(cookieDomains[i]);
			sessionCookie.addCookie(cookie);
		}

		updateActivity();

		enableDisableButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String action = "";
				if (disabled)
					action = "enable_lock";
				else
					action = "disable_lock";
				try {
					String res = new WebRequest(sessionCookie).execute("http://phone-key-website.herokuapp.com/" + action).get();
					Log.v("enabledisalbe", res);
					JSONObject jsonRes = new JSONObject(res);
					if (jsonRes.has("success") && !(jsonRes.getBoolean("success")))
						Log.e("Error!", "Error with request" + action);
					updateActivity();
				} catch (Exception e) { e.printStackTrace(); }
			}
		});

		lockUnlockButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String action = "";
				if (!disabled && closed)
					action = "open_lock";
				else if (!disabled && !closed)
					action = "close_lock";
				else	// lock disabled, so don't do anything
					return;
				try {
					String res = new WebRequest(sessionCookie).execute("http://phone-key-website.herokuapp.com/" + action).get();
					Log.v("lockunlock", res);
					JSONObject jsonRes = new JSONObject(res);
					if (jsonRes.has("msg"))
						Log.e("Error!", "Error with request" + action + ": " + jsonRes.get("msg"));
					updateActivity();
				} catch (Exception e) { e.printStackTrace(); }
			}
		});
	}

	private void updateActivity() {
		try {
			String statusMsg = "";

			final Button lockUnlockButton = ((Button)findViewById(R.id.lockUnlockButton));
			final Button enableDisableButton = ((Button)findViewById(R.id.enableDisableButton));

			String lockStatusRes = new WebRequest(sessionCookie).execute("http://phone-key-website.herokuapp.com/lock_status").get();

			JSONObject jsonLockStatus = new JSONObject(lockStatusRes);
			if (jsonLockStatus.has("closed"))
				closed =  jsonLockStatus.getBoolean("closed");
			if (jsonLockStatus.has("disabled"))
				disabled = jsonLockStatus.getBoolean("disabled");

			if (disabled) {
				statusMsg = "Lock disabled";
				((Button)findViewById(R.id.enableDisableButton)).setText("Enable Lock");
			}
			else {
				enableDisableButton.setText("Disable Lock");
			}
			if (!disabled && closed) {
				statusMsg = "Lock closed";
				lockUnlockButton.setText("Open Lock");
			}
			else if (!disabled && !closed) {
				statusMsg = "Lock open";
				lockUnlockButton.setText("Close Lock");
			}

			((TextView)findViewById(R.id.lockStatusText)).setText(statusMsg);
		} catch (Exception e) { e.printStackTrace(); }
	}

}
