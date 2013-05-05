package com.tulakj.dicepoker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {
	static final String TAG = "DbHelper";
	static final String DB_NAME = "dice_poker.db";
	static final int DB_VERSION = 5;
	static final String TABLE_GAMES = "games";
	static final String C_T_ID = "_id";
	static final String C_T_CREATED_AT = "created_at";
	static final String C_T_PLAYER_A = "player_a";
	static final String C_T_PLAYER_B = "player_b";
	static final String C_T_WINNER = "winner";
	static final String C_T_COMB_A = "combination_a";
	static final String C_T_COMB_B = "combination_b";
	
	private static final String GET_ALL_ORDER_BY = C_T_CREATED_AT + " DESC";
	private static final String[] SELECT_LAST_GAMES = { "max("
			+ DbHelper.C_T_CREATED_AT + ")" };

	
	Context context;

	// Constructor
	public DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}

	// Called only once, first time the DB is created
	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = context.getString(R.string.sql_games);

		Log.d(TAG, "onCreated sql: " + sql);

		db.execSQL(sql);
	}

	// Called whenever newVersion != oldVersion
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Typically do ALTER TABLE statements, but...we're just in development,
		// so:

		db.execSQL("drop table if exists " + TABLE_GAMES); // blow the old database
		//db.execSQL("create table " + TABLE_GAMES); // blow the old database
		// away
		Log.d(TAG, "onUpdated");
		onCreate(db); // run onCreate to get new database
	}

	/**
	 *
	 * @return Timestamp of the latest status we have it the database
	 */
	public long getLatestThrow() { //
		SQLiteDatabase db = this.getReadableDatabase();
		try {
			Cursor cursor = db.query(TABLE_GAMES, SELECT_LAST_GAMES, null, null,null, null,null);
			try {
				return cursor.moveToNext() ? cursor.getLong(0) : Long.MIN_VALUE;
			} finally {
				cursor.close();
			}
		} finally {
			db.close();
		}
	}

	/**
	*
	* @returnCursor where the columns are _id, created_at, value
	*/
	public Cursor getThrows() { //
		SQLiteDatabase db = this.getReadableDatabase();
		return db.query(TABLE_GAMES, null, null, null, null, null, GET_ALL_ORDER_BY+" LIMIT 5");
	}



}
