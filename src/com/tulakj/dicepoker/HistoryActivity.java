package com.tulakj.dicepoker;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class HistoryActivity extends Activity {
	DbHelper dbHelper;
	SQLiteDatabase db;
	
	Context context;
	TextView lastGames;
	private static final String TAG = "History";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_history);
		
		lastGames = (TextView) findViewById(R.id.last_games);
		dbHelper = new DbHelper(this); 
		
			this.getHistory();
		
		
	}


	public synchronized void getHistory(){
		Log.d(TAG, "getHistory");
		String list="";
		try{
			// get latest time
			long latestThrowCreatedAtTime = this.getData()
					.getLatestThrow();
			
			// get last 5 throws
			Date date;
			String winner,looser;
			Cursor cursor = this.getData().getThrows();
			while(cursor.moveToNext()){
				//list+=id+" ("+cursor.getString(cursor.getColumnIndex(DbHelper.C_T_VALUE))+") - ";
				winner = cursor.getString(cursor.getColumnIndex(DbHelper.C_T_WINNER));
				looser = cursor.getString(cursor.getColumnIndex(DbHelper.C_T_LOOSER));
				date = new Date(cursor.getLong(cursor.getColumnIndex(DbHelper.C_T_CREATED_AT))*1000);
				list+=date.toString()+": winner is "+winner+", looser is "+looser+"\n";
			}
		}catch(SQLException e){
			// silently ignore
			Log.d(TAG,e.getMessage());
		}
		// fill empty data
		if(list=="")list="No games...";
		// display it
		lastGames.setText(list);
		//toastShow(list);
		
	}
	
	public DbHelper getData()
	{
		return dbHelper;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.history, menu);
		return true;
	}

}
