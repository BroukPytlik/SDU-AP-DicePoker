package com.tulakj.dicepoker;

import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class Player {

	private static int DICES_COUNT = 5;
	
	public static final int SCORE_NOTHING=0;
	public static final int SCORE_PAIR=10;
	public static final int SCORE_TWO_PAIRS=20;
	public static final int SCORE_THREE_OF_A_KIND=30;
	public static final int SCORE_FIVE_HIGH=40;
	public static final int SCORE_SIX_HIGH=50;
	public static final int SCORE_FULL_HOUSE=60;
	public static final int SCORE_FOUR_OF_A_KIND=70;
	public static final int SCORE_FIVE_OF_A_KIND=80;
	
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
	
	public void lockDices(){
		for(Dice dice:dices){
			dice.lock(true);
		}
	}
	
	public void setInactive(){
		 setInactive(false);
	}

	public void setActive(){
		 setActive(false);
	}
	public void setInactive(boolean remote){
		activity.setImageResource(R.drawable.player_inactive);
		// also lock dices
		if(remote == false)
			lockDices();
	}
	public void setActive(boolean remote){
		activity.setImageResource(R.drawable.player_active);
		// also unlock dices

		if(remote == false){
			if(!firstRound){
				for(Dice dice:dices){
					dice.unlock(true);
				}
			}
		}
	}
	
	public void reset(){
		firstRound = true;
		for(Dice dice:dices){
			dice.reset();
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
	 * Pair - two dices with same value
	 * @param counts
	 * @return
	 */
	private int testPair(int[] counts){
		
		for(int i=0; i<6;i++){
			if(counts[i]==2) return SCORE_PAIR+i;
		}
		
		
		return -1;
	}
	/**
	 * Two pairs 
	 * @param counts
	 * @return
	 */
	private int testTwoPairs(int[] counts){
		
		int pairs = 0; 
		int first=0;
		int second=0;
		for(int i=0; i<6;i++){
			if(counts[i]==2) {
				if((pairs++)==0){
					first = i;
				}else{
					second=i;
				}
			}
		}
		
		if(pairs == 2){
			return SCORE_TWO_PAIRS+((first+second)/2);
		}
		
		return -1;
	}
	/**
	 * Three of a kind - three dices with the same value
	 * @param counts
	 * @return
	 */
	private int testThreeOfAKind(int[] counts){
		

		for(int i=0; i<6;i++){
			if(counts[i]==3) return SCORE_THREE_OF_A_KIND+i;
		}
		
		return -1;
	}
	/**
	 * Five High - dices from 1 to 5
	 * @param counts
	 * @return
	 */
	private int testFiveHigh(int[] counts){
		
			if(counts[0]>0)
				if(counts[1]>0)
					if(counts[2]>0)
						if(counts[3]>0)
							if(counts[4]>0)
								return SCORE_FIVE_HIGH;
		
		return -1;
	}
	/**
	 * Six high - dices from 2 to 6
	 * @param counts
	 * @return
	 */
	private int testSixHigh(int[] counts){

		
		if(counts[1]>0)
			if(counts[2]>0)
				if(counts[3]>0)
					if(counts[4]>0)
						if(counts[5]>0)
							return SCORE_SIX_HIGH;
		return -1;
	}
	/**
	 * Full house - one pair and one three of a kind
	 * @param counts
	 * @return
	 */
	private int testFullHouse(int[] counts){
		
		// at first find a pair
		int pair=-1;
		for(int i=0; i<6;i++){
			if(counts[i]==2){
				pair = i;
				break;
			}
		}
		// then if pair exits find a three
		if(pair > -1){
			int three=-1;
			for(int i=0; i<6;i++){
				if(counts[i]==3){
					three = i;
					break;
				}
			}
			// if three also exists
			if(three > -1){
				return SCORE_FULL_HOUSE+((pair+three)/2);
			}
				
		}
		
		return -1;
	}
	/**
	 * Four of a kind - all four dices has to have the same value
	 * @param counts
	 * @return
	 */
	private int testFourOfKind(int[] counts){
		for(int i=0; i<6;i++){
			if(counts[i]==4) return SCORE_FOUR_OF_A_KIND+i;
		}
		return -1;
	}
	/**
	 * Five of a kind - all five dices has to have the same value
	 * @param counts
	 * @return
	 */
	private int testFiveOfKind(int[] counts){
		for(int i=0; i<6;i++){
			if(counts[i]==5) return SCORE_FIVE_OF_A_KIND+i;
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
		
		int score;
		if( (score=testFiveOfKind(counts)) > -1) return score;
		else if( (score=testFourOfKind(counts)) > -1) return score;
		else if( (score=testFullHouse(counts)) > -1) return score;
		else if( (score=testSixHigh(counts)) > -1) return score;
		else if( (score=testFiveHigh(counts)) > -1) return score;
		else if( (score=testThreeOfAKind(counts)) > -1) return score;
		else if( (score=testTwoPairs(counts)) > -1) return score;
		else if( (score=testPair(counts)) > -1) return score;
		
		else return SCORE_NOTHING;
	}
	
	/**
	 * Will serialize player's state - dice values, selected dices
	 * @return String
	 */
	public String serialize(){
		String str = "";
		// at first get dices values
		for(int i=0; i<dices.length; i++){
			if(i>0)str+=",";
			str+=dices[i].getValue();
		}
		
		// then selected
		str+=";";
		String locks="";
		for(int i=0; i<dices.length; i++){
			if(dices[i].isLocked()){
				if(locks.length() > 0)locks+=",";
				locks+=i;
			}
		}
		str+=locks;
		
		return str;
	}
	
	/**
	 * Reversion of serialize
	 * @param msg
	 */
	public void unserialize(String msg){

		try {
			// get dice values and locked dices
			String[] values = msg.split(";");
			// values
			String[] diceValues = values[0].split(",");
	
			// locked dices
			String[] lockedDices = null;
			if(values.length>1){
				lockedDices = values[1].split(",");
			}
			for(int i=0; i<dices.length; i++){
				dices[i].unlock(true);
				// set value
				dices[i].setValue(Integer.parseInt(diceValues[i]));
				dices[i].unlock(false);
				
				// and possibly lock - look if the index of current dice is in the list
				if(lockedDices != null){
					if(java.util.Arrays.asList(lockedDices).indexOf(""+i) > -1){
						// unlock system, lock manually, lock system
						dices[i].lock(false);
					}
				}
				dices[i].lock(true);
			}
		}catch (NumberFormatException e) {
			// do nothing.. bad syntax
		}
		
		
	}
}
