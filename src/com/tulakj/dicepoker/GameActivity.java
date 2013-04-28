package com.tulakj.dicepoker;




import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity extends Activity  implements
SensorEventListener {


	private static final String TAG = "Game";
	private static final boolean D = true;
	private Context context;

	private SensorManager sensorManager;
	private double ax, ay, az, sum, oldsum; // these are the acceleration in x, y and z axis
	private int sensitivity;
	private Vibrator vibrator;
	private int VIBRATION_LENGTH = 500;


	private SharedPreferences appPref;
	public Game game;

	private static long WASNT_SHAKED = -1;
	private long lastShakeTime = WASNT_SHAKED;
	private long roundStarted = WASNT_SHAKED;
	// in miliseconds - max time before ending shaking
	private static long MAX_SHAKE_DELAY = 500;
	// generate random number if player do not shake the device
	private static long MAX_SHAKE_TIMEOUT = 5000;


	// Reference to an instance of our MyThread object
	private MyThread mMyThread = null;
	private Handler mHandler;
	
	// database
	DbHelper dbHelper;
	SQLiteDatabase db;
	
	public static int STOP_SHAKING_THREAD = 1;

	/* ***************************************************************************
	 * 
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.activity_game);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);


		dbHelper = new DbHelper(this); 

		// get shared preferenece
		appPref = PreferenceManager.getDefaultSharedPreferences(this);
		sensitivity = Integer.parseInt(appPref.getString("pref_sensitivity", "1"));
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		/*************
		 * Creating dices and players
		 */
		// select dice images for first player
		ImageView d1,d2,d3,d4,d5;
		d1= (ImageView)findViewById(R.id.dice_A_1);
		d1.setOnClickListener(mOnClickListener);
		d2= (ImageView)findViewById(R.id.dice_A_2);
		d2.setOnClickListener(mOnClickListener);
		d3= (ImageView)findViewById(R.id.dice_A_3);
		d3.setOnClickListener(mOnClickListener);
		d4= (ImageView)findViewById(R.id.dice_A_4);
		d4.setOnClickListener(mOnClickListener);
		d5= (ImageView)findViewById(R.id.dice_A_5);
		d5.setOnClickListener(mOnClickListener);
		// create player A
		Player playerA = new Player(
				(TextView)findViewById(R.id.player_A_name),
				(ImageView)findViewById(R.id.player_A_activity),
				d1,d2,d3,d4,d5);
		playerA.setName(appPref.getString("pref_player_name", getResources().getString(R.string.playerA)));

		// select dice images for second player
		d1= (ImageView)findViewById(R.id.dice_B_1);
		d1.setOnClickListener(mOnClickListener);
		d2= (ImageView)findViewById(R.id.dice_B_2);
		d2.setOnClickListener(mOnClickListener);
		d3= (ImageView)findViewById(R.id.dice_B_3);
		d3.setOnClickListener(mOnClickListener);
		d4= (ImageView)findViewById(R.id.dice_B_4);
		d4.setOnClickListener(mOnClickListener);
		d5= (ImageView)findViewById(R.id.dice_B_5);
		d5.setOnClickListener(mOnClickListener);
		//Create player A
		Player playerB = new Player(
				(TextView)findViewById(R.id.player_B_name),
				(ImageView)findViewById(R.id.player_B_activity),
				d1,d2,d3,d4,d5);
		playerB.setName(getResources().getString(R.string.playerB));
		
		/*********
		 * Creating game
		 */
		
		game = new Game(playerA, playerB, (TextView)findViewById(R.id.game_tooltip));

		/*********
		 * mapping Throw button
		 */
		Button playButton = (Button) findViewById(R.id.button_throw);
		playButton.setOnClickListener(mOnClickListener);
		// set handler for messages
		mHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            mHandleMessage(msg);
	            //call setText here
	        }
		};

		
		// start the game
		//startRound();
	}
	
	private void saveGameResult(String winner,String looser){
		//  save
		try {
			//dbOpen the database for writing
			db = dbHelper.getWritableDatabase();//

			ContentValues values = new ContentValues(); 
			// Insert into database
			values.clear(); //
			values.put(DbHelper.C_T_CREATED_AT, System.currentTimeMillis()/1000);
			values.put(DbHelper.C_T_WINNER, winner);
			values.put(DbHelper.C_T_LOOSER, looser);
			db.insertOrThrow(DbHelper.TABLE_GAMES, null, values); 
			// Close the database
			db.close(); //
		}catch(SQLException e){
			// ignore
			toastShow("Saving to database failed!");
		}
	}

	/* ***************************************************************************
	 * 
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game, menu);
		return true;
	}

	/* ***************************************************************************
	 * 
	 */


	/* ***************************************************************************
	 * 
	 */
	// @Override
	public void onSensorChanged(SensorEvent event) {
		// check if the game is waiting for roll, if not, do nothing
		if(game.getState() == Game.WAITING_FOR_ROLL ){

			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {




				ax = event.values[0];
				ay = event.values[1];
				az = event.values[2];
				sum = (ax + ay + az);

				int diff = (int) Math.abs(sum - oldsum);

				//textView.setText("Dif: "+diff+" Progress: "+sensitivity);

				if(diff > sensitivity){
					if(!(oldsum == 0 && lastShakeTime == WASNT_SHAKED)){
						// on starting the game, the oldsum is zero and thus it is causing
						// a false detection. This condition prevents it.
					
						if(D) Log.v(TAG, "Shaked "+diff+"/"+sensitivity);
						
						//	imageView.setImageResource(getImageId(sum));
						//game.roll(sum);
						vibrator.vibrate(VIBRATION_LENGTH);
						lastShakeTime = System.currentTimeMillis();
					}
					oldsum = sum;


				}

			}
		}

	}

	// --- Nested/Inner class that extends Thread ---
	/**
	 * Thread for measuring the time during shaking
	 * 
	 *
	 */
	class MyThread extends Thread {

		// Logging tag
		private static final String TAG = "MyThread";

		// Bundle key
		public static final String MSG_KEY = "mykey";

		private void stopMsg(){
			Message msg = new Message();
			msg.obj = STOP_SHAKING_THREAD;
			mHandler.sendMessage(msg);
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			if(D)Log.v(TAG, "Thread started.");

			try {
				while(true){
					sleep(100); 
					if(System.currentTimeMillis() - lastShakeTime > MAX_SHAKE_DELAY && lastShakeTime != WASNT_SHAKED){
						// do the roll once the player stops shaking
						if(D)Log.v(TAG, "Stops after shaking. Game: selecting dices.");
						stopMsg();

					}else if(roundStarted == WASNT_SHAKED && System.currentTimeMillis() - roundStarted >MAX_SHAKE_TIMEOUT){
						// skip shaking, maybe the sensors are not working
						if(D)Log.v(TAG, "Stops after timeout Game: selecting dices.");
						stopMsg();
					}
				}

			} catch (InterruptedException e) {
				if(D)Log.v(TAG, "Thread interrupted, stopping ");
			}

		}

	}


	/* ***************************************************************************
	 * 
	 */
	/**
	 * OnClick listener
	 */

	OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

			// detect button
			switch (v.getId()) {

			case R.id.button_throw:
				if(game.getState()==Game.SELECTING_DICES|| game.getState() == Game.NEW_GAME){
					startRound();
				}
				break;
			case R.id.dice_A_1:
			case R.id.dice_B_1:
				game.clickDice(0);
				break;
			case R.id.dice_A_2:
			case R.id.dice_B_2:
				game.clickDice(1);
				break;
			case R.id.dice_A_3:
			case R.id.dice_B_3:
				game.clickDice(2);
				break;
			case R.id.dice_A_4:
			case R.id.dice_B_4:
				game.clickDice(3);
				break;
			case R.id.dice_A_5:
			case R.id.dice_B_5:
				game.clickDice(4);
				break;


			}

		}
	};

	/* ***************************************************************************
	 * 
	 */

	private void startRound(){
		lastShakeTime = WASNT_SHAKED;
		oldsum = 0;
		if (mMyThread == null) {
			mMyThread = new MyThread();
			mMyThread.start();
		}
		roundStarted = System.currentTimeMillis();
		game.waitForRoll();
		if(D)Log.v(TAG, "Game: waiting for a roll");
	}


	/* ***************************************************************************
	 * 
	 */
	/**
	 * Called after end of each round
	 */
	private void endRound(){
		game.roll(sum);
		lastShakeTime = roundStarted = WASNT_SHAKED;
		// stop thread
		if(mMyThread != null){
			mMyThread.interrupt();
			mMyThread = null;
		}
		
		/* ***** Toasts info */
		int w = game.getWinner();
		String winner="Both ("+game.getPlayerA().getName()+", "+game.getPlayerB().getName()+")";
		String looser="";
		if(w == Game.WINNER_A){
			winner=game.getPlayerA().getName();
			looser=game.getPlayerB().getName();
		}
		else if(w == Game.WINNER_B){
			winner=game.getPlayerB().getName();
			looser=game.getPlayerA().getName();
		}
		
		if(!game.nextRound()){
			// if no other round is possible
			endGame(winner,looser);
			game.resetGame();
		}else{
			//toastShow("Winner of this round is: "+winner+", score is: "+game.getPlayerA().getName()+" - "+game.getPlayerA().getScore()+", "+game.getPlayerB().getName()+" - "+game.getPlayerB().getScore(),Toast.LENGTH_LONG);
		}
	}

	/* ****************************************************************************
	 * 
	 */
	/**
	 * Called after end of a game
	 * @param winner
	 */
	private void endGame(String winner,String looser){
		AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
		// set message
		dlgAlert.setMessage("Winner is: "+winner
				+", score is: "+game.getPlayerA().getName()
				+" - "+game.getPlayerA().getScore()
				+", "+game.getPlayerB().getName()
				+" - "+game.getPlayerB().getScore());
		//set title
		dlgAlert.setTitle("End of the game");
		dlgAlert.setPositiveButton("Ok",
			    new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			          //dismiss the dialog  
			        }
			    });
		dlgAlert.setCancelable(true);
		dlgAlert.create().show();
		// save to DB
		saveGameResult(winner, looser);
	}
	/* ***************************************************************************
	 * 
	 */
	public void mHandleMessage (Message msg){
		if((Integer)msg.obj == STOP_SHAKING_THREAD){
			endRound();
		}
	}
	/* ***************************************************************************
	 * 
	 */

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

	/* ***************************************************************************
	 * 
	 */
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}
}
