package com.tulakj.dicepoker;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.Window;
import android.webkit.WebView;

public class RulesActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_rules);
		
		/*TextView tv = (TextView)findViewById(R.id.textView1);
		tv.setText(Html.fromHtml(getResources().getString(R.string.rules_text)));*/
		WebView wv = (WebView)findViewById(R.id.webView1);
		wv.loadDataWithBaseURL(null,
				"<head>" +
					"<style>" +
						"* {margin:0;padding:0;font-size:15; text-align:justify;background-color:transparent;}" +
						"body {margin:5;}"+
						"p {margin-bottom: 5;}"+
						"li {margin-left:20;}"+
						"h1 {font-size: 200%; text-decoration: underline;}"+
					"</style>" +
				"</head>"
		+getResources().getString(R.string.rules_text),
		"text/html","UTF-8","about:blank");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.rules, menu);
		return false;
	}

}
