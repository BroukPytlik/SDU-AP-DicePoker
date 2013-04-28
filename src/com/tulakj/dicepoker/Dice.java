package com.tulakj.dicepoker;

import java.util.BitSet;
import java.util.Random;

import android.widget.ImageView;

public class Dice {
	private int LOCKED_OPAQUE = 90;
	private int UNLOCKED_OPAQUE = 255;
	
	
	private int value;
	private boolean locked_system = true;
	private boolean locked_manual = false;
	private ImageView image;

	private static Random generator;

	public Dice(ImageView img){
		image = img;
	}


	/**
	 * Generate a random number between 0-5
	 * @return
	 */
	private int getRandomInt(){
		if(generator == null){
			generator = new Random();
		}

		return generator.nextInt(6);
	}

	/**
	 * 
	 * @return int
	 */
	public int getValue(){
		return value;
	}

	
	/**
	 * Shall be call after a player's throw.
	 * if the dice is manually locked , then it will do nothing
	 * @param val
	 */
	public void rolled(double val){
		if(locked_manual) return;
		
		int t = Math.abs(((int)val+this.getRandomInt()) % 6);
		
		// save the throw
		value = t;
		// set image
		setImage();
	}
	

	/**
	 * Set image of the dice according of set value and locked state
	 */
	private void setImage(){
		// set image
		image.setImageResource(getImageId(value));
		
		//set opacity
		
			if(locked_system){
				image.setAlpha(LOCKED_OPAQUE);
			}else{
				image.setAlpha(UNLOCKED_OPAQUE);
			}
		
		
	}
	
	

	/**
	 * Get image id for the dice side
	 * @param s int
	 * @return int
	 */
	private int getImageId(int s){

		if( locked_manual){
			switch (s) {
			case 0:
				return R.drawable.terning1_highlighted;
			case 1:
				return R.drawable.terning2_highlighted;
			case 2:
				return R.drawable.terning3_highlighted;
			case 3:
				return R.drawable.terning4_highlighted;
			case 4:
				return R.drawable.terning5_highlighted;
			case 5:
				return R.drawable.terning6_highlighted;
			}
		}else{
			switch (s) {
			case 0:
				return R.drawable.terning1;
			case 1:
				return R.drawable.terning2;
			case 2:
				return R.drawable.terning3;
			case 3:
				return R.drawable.terning4;
			case 4:
				return R.drawable.terning5;
			case 5:
				return R.drawable.terning6;

			}
		}

		return 0;
	}

	public void unlock(boolean system){
		if(system){
			locked_system = false;
		}else if(locked_system == false){
			// manual locking/unlocking can be done only if dice is not locked
			// by system
			locked_manual = false;
		}
		setImage();
	}

	public void lock(boolean system){
		if(system){
			locked_system = true;
		}else if(locked_system == false){
			// manual locking/unlocking can be done only if dice is not locked
			// by system
			locked_manual = true;
		}
		setImage();
	}
	
	/**
	 * Toggle the manual lock
	 */
	public void toggle(){
		if(locked_manual){
			unlock(false);
		}else{
			lock(false);
		}
	}
	
	/**
	 * Set the dice to default state
	 */
	public void reset(){
		locked_system = true;
		locked_manual = false;
		setImage();
	}
}
