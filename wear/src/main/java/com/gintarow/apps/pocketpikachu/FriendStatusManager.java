package com.gintarow.apps.pocketpikachu;

/**
 * Created by Kikuchi on 15/05/04.
 * 他のクラスからも見たいからSingletonにしたい?
 */
public class FriendStatusManager {

	static FriendStatusManager friendStatusManager = null;

	int friendStatus;
	int friendPoint;

	static final int STATUS_FRIEND_DONZOKO	= 1;
	static final int STATUS_FRIEND_BAD		= 2;
	static final int STATUS_FRIEND_SOSO		= 3;
	static final int STATUS_FRIEND_GOOD		= 4;
	static final int STATUS_FRIEND_FRIENDLY	= 5;

	public FriendStatusManager(int point){
		friendPoint = point;
		updateFriendStatus();
	}


	private void updateFriendStatus(){
		if(friendPoint<=-30){
			friendPoint=-30;
			friendStatus = STATUS_FRIEND_DONZOKO;
		}else if(friendPoint<=-15){
			friendStatus = STATUS_FRIEND_BAD;
		}else if(friendPoint <= 14){
			friendStatus = STATUS_FRIEND_SOSO;
		}else if(friendPoint <= 29){
			friendStatus = STATUS_FRIEND_GOOD;
		}else{
			if(friendPoint>35){
				friendPoint=35;
			}
			friendStatus = STATUS_FRIEND_FRIENDLY;
		}
	}

	public int getFriendStatus(){
		updateFriendStatus();
		return  friendStatus;
	}

	public int getFriendPoint(){
		return friendPoint;
	}

	public void addPoint(int point){
		friendPoint += point;
		updateFriendStatus();
	}

	public void reducePoint(int point){
		friendPoint -= point;
		updateFriendStatus();
	}
}
