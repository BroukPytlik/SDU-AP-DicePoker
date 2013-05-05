package com.tulakj.dicepoker;




import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.Window;
import android.widget.ArrayAdapter;
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


	public SharedPreferences appPref;
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
	private Handler mThreadHandler;
	
	// database
	DbHelper dbHelper;
	SQLiteDatabase db;
	
	public static final int STOP_SHAKING_THREAD = 1;
	public static final int RETURN_TO_MAIN_MENU = 2;

    // Intent request codes
    private static final int REQUEST_CANCELED = 0;
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private GameBTService mChatService = null;
    

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String TOAST = "toast";

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    private boolean isMpServer = false;
    
    private Dialog waitForClientDlg;

	/* ***************************************************************************
	 * 
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
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
		mThreadHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            mHandleMessage(msg);
	            //call setText here
	        }
		};

		
		// is it bt multiplayer?
		if(appPref.getBoolean("bt_game_checkbox",false)){
			game.multiplayer = true;
	        // Get local Bluetooth adapter
	        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	        // If the adapter is null, then Bluetooth is not supported
	        if (mBluetoothAdapter == null) {
	            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
	            finish();
	            return;
	        }
			game.setRemotePlayer(Game.PLAYER_B);
			createMPDialog();
		}else{
			game.paused = false;
		}
		
		
		/*
		 * TESTING OF VICTORY CONDITIONS
		 */
		//test_victory_conditions();
	}
	

	/* ***************************************************************************
	 * 
	 */
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == GameBTService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }


	/* ***************************************************************************
	 * 
	 */
    private void waitForClient(){
		isMpServer = true;
		AlertDialog.Builder dialog =  new AlertDialog.Builder(this);
		// set message
		dialog.setMessage(getResources().getString(R.string.waiting_for_client));
		//set title
		dialog.setTitle(getResources().getString(R.string.waiting));
		// button for cancelation
		dialog.setPositiveButton("Storno",
			    new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			        	// show the selection
			        	// show again the dialog
			        	createMPDialog();	
			        	
			        }
			    });
		// on dismiss/cancelation
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				/*Message msg = new Message();
				msg.obj = RETURN_TO_MAIN_MENU;
				mThreadHandler.sendMessage(msg);*/

	        	createMPDialog();	
			}
		});
		dialog.setCancelable(true);
		waitForClientDlg = dialog.create();
		waitForClientDlg.show();
    }

	/* ***************************************************************************
	 * 
	 */
	public void createMPDialog(){
		AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
		// set message
		dlgAlert.setMessage(getResources().getString(R.string.server_or_client));
		//set title
		dlgAlert.setTitle("Bluetooth multiplayer");
		// button for "be client"
		dlgAlert.setPositiveButton(getResources().getString(R.string.be_client),
			    new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			        	// show the selection
			        	callMPActivity();
			        }
			    });
		// button for "be server
		dlgAlert.setNegativeButton(getResources().getString(R.string.be_server),
			    new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int which) {
			          //dismiss the dialog  
			        	waitForClient();
			        }
			    });
		// on dismiss/cancelation
		dlgAlert.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				Message msg = new Message();
				msg.obj = RETURN_TO_MAIN_MENU;
				mThreadHandler.sendMessage(msg);
			}
		});
		dlgAlert.setCancelable(true);
		dlgAlert.create().show();
	}

	/* ***************************************************************************
	 * 
	 */
	public void callMPActivity(){
		isMpServer = false;
		Intent intent = new Intent(this, MultiplayerActivity.class);
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
	}
	/* ***************************************************************************
	 * 
	 */
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // if multiplayer
		if(appPref.getBoolean("bt_game_checkbox",false)){
	        // If BT is not on, request that it be enabled.
	        // setupChat() will then be called during onActivityResult
	        if (!mBluetoothAdapter.isEnabled()) {
	            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	        // Otherwise, setup the chat session
	        } else {
	            if (mChatService == null) setupChat();
	        }
		}
    }

	/* ***************************************************************************
	 * 
	 */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

	/* ***************************************************************************
	 * 
	 */
    private void setupChat() {
    	if(D) Log.d(TAG, "setupChat()");

       
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new GameBTService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }



    /* ***************************************************************************
	 * 
	 */
    private void setupMP(){
    	if(D) Log.d(TAG, "Setup Multiplayer");
    	
    	/**
    	 * Communication schema:
    	 * 
    	 * Client = A, Server = B
    	 * 
    	 * A->B: SYN
    	 * B->A: ACK
    	 * -- now the game is unpaused -- 
    	 * B->A: STATE
    	 * A->B: STATE
    	 * ...
    	 * 
    	 */
    	if(isMpServer){
	    	waitForClientDlg.dismiss();
    	}else{
	    	sendMsg("SYN "+appPref.getString("pref_player_name", getResources().getString(R.string.playerA)));
    	}
    	
    }
	/* ***************************************************************************
	 * 
	 */
    private void sendMsg(String msg){
    	 // Check that we're actually connected before trying anything
        if (mChatService.getState() != GameBTService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (msg.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = msg.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    	
    }

    /* ***************************************************************************
	 * 
	 */
    private String createStateMsg(){
    	if(D) Log.d(TAG, "Create state msg ");
    	
    	String msg = "STATE "+game.getActualPlayer().serialize();
    	
    	return msg;
    }

    /* ***************************************************************************
	 * 
	 */
    /**
     * This will join name with more words splitted by split. :)
     * @param parts
     * @return
     */
    private String joinName(String [] parts){
    	String name = "";
    	for(int i=1; i<parts.length; i++){
    		if(i>1)name+=" ";
    		name+=parts[i];
    	}
    	return name;
    }
    
    /**
     * 
     * @param msg
     */
    private void parseMsg(String msg){
    	//if(D) toastShow("RECIEVED MSG: " + msg);
    	
    	// the message has two parts splited by a space - identificator of action and values
    	String[] parts = msg.split(" ");
    	//Log.d(TAG,"action: '"+parts[0]+"'");
    	if(parts[0].equals("STATE")){
    		// this will set remote player's dices
        	Log.d(TAG,"new state: '"+parts[1]+"'");
    		game.getRemotePlayer().unserialize(parts[1]);
			vibrator.vibrate(VIBRATION_LENGTH);
			endRound();
    		
    	}else if(parts[0].equals("SYN")){
    		// A client is trying to connect - save his name and reply
    		// and unpause
        	Log.d(TAG,"SYN '"+parts[1]+"'");
        	//game.setRemotePlayer(Game.PLAYER_B);
    		game.getRemotePlayer().setName(joinName(parts));
    		game.getPlayerA().setName(appPref.getString("pref_player_name", getResources().getString(R.string.playerA)));
        	//sendMsg("PLAYER "+appPref.getString("pref_player_name", getResources().getString(R.string.playerA)));
        	sendMsg("ACK "+appPref.getString("pref_player_name", getResources().getString(R.string.playerA)));
        	game.resetGame();
    		game.paused = false;
    		
    		
    	}else if(parts[0].equals("ACK")){
    		// A server replied to our SYN - save his name
    		// switch players (server begins) and unpause
        	Log.d(TAG,"ACK  '"+parts[1]+"'");
        	game.setRemotePlayer(Game.PLAYER_A);
    		game.getRemotePlayer().setName(joinName(parts));
    		game.getPlayerB().setName(appPref.getString("pref_player_name", getResources().getString(R.string.playerA)));
        	game.resetGame();
    		game.paused = false;
        	
    		
    	}else{
    		toastShow("UNKNOWN MESSAGE: " + msg);
    	}
    	
    }
	/* ***************************************************************************
	 * 
	 */
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	//toastShow("BT message recieved: "+msg.arg1);
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                //if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case GameBTService.STATE_CONNECTED:
                	toastShow(getResources().getString(R.string.title_connected_to)+mConnectedDeviceName, Toast.LENGTH_LONG);
                	setupMP();
                    break;
                case GameBTService.STATE_CONNECTING:
                	toastShow(getResources().getString(R.string.title_connecting), Toast.LENGTH_LONG);
                	game.getRemotePlayer().setName(getResources().getString(R.string.title_connecting));
                    break;
                case GameBTService.STATE_LISTEN:
                case GameBTService.STATE_NONE:
                	toastShow(getResources().getString(R.string.title_not_connected), Toast.LENGTH_LONG);
                	game.getRemotePlayer().setName("Offline");
                	game.paused = true;
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                parseMsg(readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
	/* ***************************************************************************
	 * 
	 */
	public void initMultiplayer(){
		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
		
	}


	/* ***************************************************************************
	 * 
	 */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        if(resultCode == REQUEST_CANCELED){
       // 	if(D) Log.d(TAG, "onActivityResult " + resultCode +" - canceled");
        	// show again the dialog
        	createMPDialog();	
        }

        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(MultiplayerActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

	/* ***************************************************************************
	 * 
	 */
	private void saveGameResult(String A,String B, int winner, int comb_a, int comb_b){
		//  save
		try {
			//dbOpen the database for writing
			db = dbHelper.getWritableDatabase();//

			ContentValues values = new ContentValues(); 
			// Insert into database
			values.clear(); //
			values.put(DbHelper.C_T_CREATED_AT, System.currentTimeMillis()/1000);
			values.put(DbHelper.C_T_WINNER, winner);
			values.put(DbHelper.C_T_PLAYER_A, A);
			values.put(DbHelper.C_T_PLAYER_B, B);
			values.put(DbHelper.C_T_COMB_A, comb_a);
			values.put(DbHelper.C_T_COMB_B, comb_b);
			db.insertOrThrow(DbHelper.TABLE_GAMES, null, values); 
			// Close the database
			db.close(); //
		}catch(SQLException e){
			// ignore
			toastShow("Saving to database failed! ("+A+","+B+","+winner+","+comb_a+","+comb_b+")");
		}
	}

	/* ***************************************************************************
	 * 
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.game, menu);
		return false;
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
			mThreadHandler.sendMessage(msg);
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			//if(D)Log.v(TAG, "Thread started.");

			try {
				while(true){
					sleep(100); 
					if(System.currentTimeMillis() - lastShakeTime > MAX_SHAKE_DELAY && lastShakeTime != WASNT_SHAKED){
						// do the roll once the player stops shaking
						if(D)Log.v(TAG, "Stops after shaking. Game: selecting dices.");
						stopMsg();

					}else if(lastShakeTime == WASNT_SHAKED && System.currentTimeMillis() - roundStarted >MAX_SHAKE_TIMEOUT){
						// skip shaking, maybe the sensors are not working
						if(D)Log.v(TAG, "Stops after timeout Game: selecting dices.");
						vibrator.vibrate(VIBRATION_LENGTH);
						stopMsg();
					}
				}

			} catch (InterruptedException e) {
			//	if(D)Log.v(TAG, "Thread interrupted, stopping ");
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
				if(game.paused == false){
					if(game.getState()==Game.SELECTING_DICES|| game.getState() == Game.NEW_GAME){
						waitForShake();
					}
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

	private void waitForShake(){
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
	 * Called on end of each round, when player shaked his phone
	 */
	private void endRound(){
		lastShakeTime = roundStarted = WASNT_SHAKED;
		// stop thread
		if(mMyThread != null){
			mMyThread.interrupt();
			mMyThread = null;
		}
		if(game.multiplayer && game.getActualPlayer() != game.getRemotePlayer()){
			// send actual state to the remote player if it is multiplayer and it is not remote's draw
			sendMsg(createStateMsg());
		}
		
		/* ***** Toasts info */
		
				//"Both ("+game.getPlayerA().getName()+", "+game.getPlayerB().getName()+")";
		
		if(!game.nextRound()){
			game.resetGame();
			// if no other round is possible
			endGame();
			game.paused = false;
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
	private void endGame(){
		AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
		// set message
		int w = game.getWinner();
		String result= "";
		if(w == Game.PLAYER_A){
			result= getResources().getString(R.string.winner_is,
					game.getPlayerA().getName(), 
					getResources().getString(getCombinationString(game.getPlayerA().getScore())),
					game.getPlayerB().getName(),
					getResources().getString(getCombinationString(game.getPlayerB().getScore()))
				);
			
		}else if(w == Game.PLAYER_B){
			result= getResources().getString(R.string.winner_is,
					game.getPlayerB().getName(), 
					getResources().getString(getCombinationString(game.getPlayerB().getScore())),
					game.getPlayerA().getName(),
					getResources().getString(getCombinationString(game.getPlayerA().getScore()))
				);
			
		}else if(w == Game.WINNER_DRAW){
			result = getResources().getString(R.string.draw_is,
					game.getPlayerA().getName(),
					game.getPlayerB().getName(),
					getResources().getString(getCombinationString(game.getPlayerA().getScore()))
				);
		}
		
		dlgAlert.setMessage(result);
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
		saveGameResult(
				game.getPlayerA().getName(),
				game.getPlayerB().getName(),
				w,
				getCombinationString(game.getPlayerA().getScore()),
				getCombinationString(game.getPlayerB().getScore())
			);
	}
	/* ***************************************************************************
	 * 
	 */
	public void mHandleMessage (Message msg){
		int code = (Integer)msg.obj;
		switch(code){
		case STOP_SHAKING_THREAD:
			game.roll(sum);
			endRound();
			break;

		case RETURN_TO_MAIN_MENU:
			if(D) Log.v(TAG,"Return to main menu.");
			finish();
			break;
			
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

	/* ***************************************************************************
	 * 
	 */
	/**
	 * return string id for the score of the player
	 * @param score
	 * @return
	 */
	private int getCombinationString(int score){
		if(score >= Player.SCORE_FIVE_OF_A_KIND) 	return R.string.score_five;
		if(score >= Player.SCORE_FOUR_OF_A_KIND) 	return R.string.score_four;
		if(score >= Player.SCORE_FULL_HOUSE) 		return R.string.score_full_house;
		if(score >= Player.SCORE_SIX_HIGH) 			return R.string.score_six_high;
		if(score >= Player.SCORE_FIVE_HIGH) 		return R.string.score_five_high;
		if(score >= Player.SCORE_THREE_OF_A_KIND) 	return R.string.score_three;
		if(score >= Player.SCORE_TWO_PAIRS) 		return R.string.score_two_pairs;
		if(score >= Player.SCORE_PAIR) 				return R.string.score_pair;
		
		// nothing was found...
		return R.string.score_nothing;
	}

	/* ***************************************************************************
	 * 
	 */
	/* ***********************************************
	 * TESTING
	 */
	
	private void test_victory_conditions(){
		String [] games = {
			"0,1,2,4,5;", // NOTHING
			"0,0,2,3,4;", // PAIR lower
			"1,1,0,3,4;", // PAIR higher
			"1,1,0,0,4;", // TWO PAIR lower
			"1,1,2,2,4;", // TWO PAIR higher
			"1,1,1,0,4;", // THREE lower
			"1,4,2,2,2;", // THREE higher
			"4,3,2,1,0;", // FIVE HIGH
			"1,2,3,4,5;", // SIX HIGH
			"1,1,1,2,2;", // FULL HOUSE lower
			"1,1,1,3,3;", // FULL HOUSE higher
			"4,4,4,4,0;", // FOUR OF lower
			"5,5,5,5,0;", // FOUR OF higher
			"2,2,2,2,2;", // FIVE OF lower
			"3,3,3,3,3;", // FIVE OF higher
		};
		String [] gamesNames = {
			"NOTHING", // NOTHING
			"PAIR lower", // PAIR lower
			"PAIR higher", // PAIR higher
			"TWO PAIRS lower", // TWO PAIR
			"TWO PAIRS higher", // TWO PAIR
			"THREE OF A KIND lower", // THREE
			"THREE OF A KIND higher", // THREE
			"FIVE HIGH", // FIVE HIGHT
			"SIX HIGH", // SIX HIGHT
			"FULL HOUSE lower", // FULL HOUSE
			"FULL HOUSE higher", // FULL HOUSE
			"FOUR OF A KIND lower", // FOUR OF
			"FOUR OF A KIND higher", // FOUR OF
			"FIVE OF A KIND lower", // FIVE OF
			"FIVE OF A KIND higher", // FIVE OF
		};
		
		for(int a=0; a<games.length;a++){
			for(int b=0; b<games.length; b++){
				if(test_game(games, gamesNames, a, b) == false){
					return;
				}
			}
		}
		
		Log.i(TAG,"ALL TESTS OK!");
		
	}
	
	private boolean test_game(String [] games, String [] names, int A, int B){
		game.getPlayerA().unserialize(games[A]);
		game.getPlayerB().unserialize(games[B]);
		
		int result = game.getWinner();
		
		int correctResult = Game.WINNER_DRAW;
		if(A>B)correctResult = Game.PLAYER_A;
		else if(A<B)correctResult = Game.PLAYER_B;
		
		if(result == correctResult){
			
			Log.i(TAG,"OK (A:"+getResources().getString(getCombinationString(game.getPlayerA().getScore()))+" vs B:"+getResources().getString(getCombinationString(game.getPlayerB().getScore()))+")");
			return true;
		}else{
			Log.e(TAG,"ERROR (A:"+getResources().getString(getCombinationString(game.getPlayerA().getScore()))+" vs B:"+getResources().getString(getCombinationString(game.getPlayerB().getScore()))+") - result is "+result+" instead of "+correctResult);
			return false;
		}
		
	}
	
}
