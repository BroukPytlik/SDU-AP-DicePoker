package com.tulakj.dicepoker;

import java.util.BitSet;

import android.widget.ImageView;
import android.widget.TextView;

public class Player {

	private static int DICES_COUNT = 5;
	
	public boolean firstRound = true;
	
	private Dice[] dices;
	private TextView nameField;
	private ImageView activity;
	private String name;
	
	public  Player(TextView name, ImageView activity, ImageView img1,ImageView img2,ImageView img3,ImageView img4,ImageView img5){
		
			this.nameField = name;
			this.activity = activity;
			dices = new Dice[5];
			dices[0]=new Dice(img1);
			dices[1]=new Dice(img2);
			dices[2]=new Dice(img3);
			dices[3]=new Dice(img4);
			dices[4]=new Dice(img5);
			
			
			this.setInactive();
			
	}
	
	public void setName(String n){
		nameField.setText(n);
		name = n;
	}
	public String getName(){
		return name;
	}
	
	
	public void rolled(double val){
		for(Dice dice:dices){
			dice.rolled(val);
		}
	}
	
	public void setInactive(){
		activity.setImageResource(R.drawable.player_inactive);
		// also lock dices
		for(Dice dice:dices){
			dice.lock(true);
		}
	}
	public void setActive(){
		activity.setImageResource(R.drawable.player_active);
		// also unlock dices
		if(!firstRound){
			for(Dice dice:dices){
				dice.unlock(true);
			}
		}
	}
	/**
	 * Click on given dice - lock/unlock it
	 * @param dice
	 */
	public void clickDice(int dice){
		dices[dice].toggle();
	}
	
	
	/* ***************************
	 * Getting of score
	 */
	/**
	 * Get count of 
	 * @param val
	 * @return
	 */
	private int getCountOfValues(int val){
		int count = 0;
		
		
		for(Dice dice : dices){
			
			if(dice.getValue()==val){
				count++;
			}
		}
		return count;
	}
	/* ******
	 * combination tests - return the 
	 */
	
	/**
	 * Five of a kind - all five dices has to have the same value
	 * @param counts
	 * @return
	 */
	private int testFiveOfKind(int[] counts){
		for(int i=0; i<6;i++){
			if(counts[i]>0) return i;
		}
		return -1;
	}
	
	
	private int getSum(){
		int sum = 0;
		for(Dice dice : dices){
		
			sum += dice.getValue()+1;
		}
		return sum;
	}
	/**
	 * 
	 * @return
	 */
	public int getScore(){
		int[] counts = new int[6];
		counts[0]= getCountOfValues(0);
		counts[1] = getCountOfValues(1);
		counts[2] = getCountOfValues(2);
		counts[3] = getCountOfValues(3);
		counts[4] = getCountOfValues(4);
		counts[5] = getCountOfValues(5);
		
		
		
		return getSum();
	}
}
