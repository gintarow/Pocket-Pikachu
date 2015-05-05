package com.gintarow.apps.pocketpikachu;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Kikuchi on 15/05/05.
 */
public class PikachuStatusManager {

	static PikachuStatusManager pikachuStatusManager;
	FriendStatusManager friendStatusManager;
	SharedPreferences sp;

	int stepToday;
	int stepTotal;
	int stepYesterday;
	int watt;
	int wattCount;


	public PikachuStatusManager(Context context){
//		sp = context.getSharedPreferences()
//		friendStatusManager = new FriendStatusManager();
	}

	static PikachuStatusManager getInstance(Context context){
		//todo
		return pikachuStatusManager;
	}

	public void addStep(int step){

	}






}
