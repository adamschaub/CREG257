package com.example.morebluetoothtesting;

import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

public class RegisterActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.register_activity_layout);
	    
	    final Button registerButton = (Button)findViewById(R.id.registerButton);
	    registerButton.setOnClickListener(new View.OnClickListener () {
			public void onClick(View v) {
				try {
					String res = new WebRequest().execute("http://phone-key-website.herokuapp.com/mobile-register",
							"username", ((TextView)findViewById(R.id.username)).getText().toString(),
							"password", ((TextView)findViewById(R.id.password1)).getText().toString(),
							"firstname", ((TextView)findViewById(R.id.firstName)).getText().toString(),
							"lastname", ((TextView)findViewById(R.id.lastName)).getText().toString(),
							"email", ((TextView)findViewById(R.id.email)).getText().toString(),
							"phone", "1234567890",
							"MAC", BluetoothAdapter.getDefaultAdapter().getAddress()).get();
					
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
					imm.hideSoftInputFromWindow(registerButton.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
				} catch (Exception e) { Log.e("Exception!", e.toString()); }
			}
		});
	}
}