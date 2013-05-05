package com.tulakj.dicepoker;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	

	private static final String TAG = "DICE POKER";
    private static final boolean D = true;

	private SharedPreferences appPref;

	Context context;

	/** Buttons
	 * 
	 */

	/*Button startButton;
	Button settingsButton;
	Button rulesButton;*/
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        if(D) Log.d(TAG, "+++ onCreate +++");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		

		// get shared preferenece
		appPref = PreferenceManager.getDefaultSharedPreferences(this);
		
		Button startButton = (Button) findViewById(R.id.button_start);
		startButton.setOnClickListener(mOnClickListener);
		Button rulesButton = (Button) findViewById(R.id.button_rules);
		rulesButton.setOnClickListener(mOnClickListener);
		Button settingsButton = (Button) findViewById(R.id.button_settings);
		settingsButton.setOnClickListener(mOnClickListener);
		Button historyButton = (Button) findViewById(R.id.button_history);
		historyButton.setOnClickListener(mOnClickListener);
		
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        if(D) Log.d(TAG, "onCreateOptionsMenu");
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		return false;
	}

	
	private void selectIntent(int id){
		Intent intent;
		// detect button
		switch (id) {

		case R.id.button_start:
			intent = new Intent(this, GameActivity.class);
			startActivity(intent);
			break;
		case R.id.button_settings:
			intent = new Intent(this, SettingsActivity.class); 
			startActivity(intent);
			break;
		case R.id.button_rules:
			intent = new Intent(this, RulesActivity.class); 
			startActivity(intent);
			break;
		case R.id.button_history:
			intent = new Intent(this, HistoryActivity.class); 
			startActivity(intent);
			break;

		}
	}
	

	/**
	 * OnClick listener
	 */

	OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
	        if(D) Log.d(TAG, "onClickListener");
	        selectIntent(v.getId());
		}
	};


	/**
	 * @param text
	 * @param duration
	 */
	private void toastShow(String text,int duration) {
		if (context == null) {
			context = getApplicationContext();
		}

		if(D)Log.v(TAG, "Toast: " + text);
		Toast.makeText(context, text, duration).show();
	}
	private void toastShow(String text){
		toastShow(text,Toast.LENGTH_SHORT);
	}

}
