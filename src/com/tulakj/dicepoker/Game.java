package com.tulakj.dicepoker;

import android.util.Log;
import android.widget.TextView;

public class Game {
	private Player playerA;
	private Player playerB;
	private Player actualPlayer;
	private TextView tooltip;
	
	private int round = 0;
	public static int MAX_ROUNDS = 3; 

	public static int NEW_GAME = 0;
	public static int SELECTING_DICES = 1;
	public static int WAITING_FOR_ROLL = 2;
	
	public static int WINNER_A = 1;
	public static int WINNER_B = 2;
	public static int WINNER_DRAW = 3;
	
	private int state = NEW_GAME;
	
	
	public Game(Player A,Player B, TextView tooltip){
		playerA = A;
		playerB = B;
		this.tooltip = tooltip;
		switchPlayers();
	}
	
	public Player getPlayerA(){
		return playerA;
	}
	public Player getPlayerB(){
		return playerB;
	}
	
	public int getState(){
			return state;
	}
	public boolean isFirstRound(){
		return actualPlayer.firstRound;
	}
	
	public void waitForRoll(){
			tooltip.setText(R.string.game_do_shake );
		
		state = WAITING_FOR_ROLL;
	}
	public void waitForDiceSelect(){
		// we want different text for the first round
		if(actualPlayer.firstRound){
			state = NEW_GAME;
			tooltip.setText(R.string.game_do_shake_for_start);
		}else{
			tooltip.setText(R.string.game_select_dices);
			state = SELECTING_DICES;
		}
	}
	
	public void switchPlayers(){
		if(actualPlayer != null){
			actualPlayer.setInactive();
		}
		
		if(actualPlayer == playerA){
			actualPlayer = playerB;
		}else{
			actualPlayer = playerA;
		}
		actualPlayer.setActive();
			
	}
	
	public void roll(double val){
		actualPlayer.rolled(val);
		actualPlayer.firstRound = false;
	}

	/**
	 * Click on given dice of actual player - lock/unlock it
	 * @param dice
	 */
	public void clickDice(int dice){
		actualPlayer.clickDice(dice);
	}
	
	
	public int getWinner(){
		int a=playerA.getScore();
		int b=playerB.getScore();
		
		if(a>b) return WINNER_A;
		else if(b>a) return WINNER_B;
		
		return WINNER_DRAW;
	}
	
	/**
	 * Increment the round counter and return true if possible.
	 * If not (MAX_ROUNDS reached), return false;
	 * 
	 * Will increment only if player A is the actual player (i.e. will not
	 * increment in middle of round)
	 * @return
	 */
	public boolean nextRound(){
		switchPlayers();
		waitForDiceSelect();
		if(playerA == actualPlayer){
			round++;
			if(round < MAX_ROUNDS){
				Log.v("Game class", "Incrementing round to "+round);
				return true;
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Reset the game to the initial state
	 */
	public void resetGame(){
		round=0;
		state=NEW_GAME;
		playerA.firstRound = true;
		playerB.firstRound = true;
	}
}
