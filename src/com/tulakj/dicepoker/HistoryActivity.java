package com.tulakj.dicepoker;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.Window;
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
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
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
			//long latestThrowCreatedAtTime = this.getData()
			//		.getLatestThrow();
			
			// get last 5 throws
			Date date;
			int winner;
			String A,B,comb_a, comb_b,result;
			
			Cursor cursor = this.getData().getThrows();
			while(cursor.moveToNext()){
				//list+=id+" ("+cursor.getString(cursor.getColumnIndex(DbHelper.C_T_VALUE))+") - ";
				winner = cursor.getInt(cursor.getColumnIndex(DbHelper.C_T_WINNER));
				A = cursor.getString(cursor.getColumnIndex(DbHelper.C_T_PLAYER_A));
				B = cursor.getString(cursor.getColumnIndex(DbHelper.C_T_PLAYER_B));
				comb_a = getResources().getString(cursor.getInt(cursor.getColumnIndex(DbHelper.C_T_COMB_A)));
				comb_b = getResources().getString(cursor.getInt(cursor.getColumnIndex(DbHelper.C_T_COMB_B)));
				date = new Date(cursor.getLong(cursor.getColumnIndex(DbHelper.C_T_CREATED_AT))*1000);
				if(winner == Game.PLAYER_A){
					result = getResources().getString(R.string.winner_is,
							A, 
							comb_a,
							B,
							comb_b
						);
				}else if (winner == Game.PLAYER_B){
					result = getResources().getString(R.string.winner_is,
							B, 
							comb_b,
							A,
							comb_a
						);
				}else{
					result = getResources().getString(R.string.draw_is,
							A, 
							B,
							comb_a
						);
				}
				
				list+=date.toString()+": "+result+"\n";
			}
		}catch(SQLException e){
			// silently ignore
			Log.d(TAG,e.getMessage());
		}
		// fill empty data
		if(list=="")list="No plays yet...";
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
		//getMenuInflater().inflate(R.menu.history, menu);
		return false;
	}

}
