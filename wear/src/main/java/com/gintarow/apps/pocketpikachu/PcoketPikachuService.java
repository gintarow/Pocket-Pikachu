package com.gintarow.apps.pocketpikachu;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * 各種ステータスを管理するサービス
 * ・歩数
 * ・ワット数
 * ・仲良し度
 */
public class PcoketPikachuService extends Service implements SensorEventListener{
	public PcoketPikachuService() {
	}

	static final String TAG = "PocketPikachuService";

	SensorManager mSensorManager;
	Sensor sensorStepCounter;
	boolean sensorFlag =false;

	FriendStatusManager friendStatusManager;
	private SharedPreferences sharedPreferences;
	static final String KEY_PREFERENCES = "PocketPikachu";
	static final String KEY_PREF_STEP_TODAY = "stepToday";
	static final String KEY_PREF_STEP_TOTAL = "stepTotal";
	static final String KEY_PREF_STEP_YESTERDAY  = "stepYesterday";
	static final String KEY_PREF_FRIEND_POINT = "friendPoint";
	static final String KEY_PREF_FRIEND_WATT = "WattPoint";

	static int stepToday = 0;
	static int stepTotal = 0;
	static int prevStep = -1;

	static int watt = 0;
	static int wattCount = 0;

	static final int MSG_SAVE_STATUS = 0;
	static final int MSG_DAILY_RESET = 1;
	static final long StatusSaveIntervalMs = 60 * 60 * 1000;


	//	private SQLiteDatabase db;

	//	private WearLifeLogSQLiteOpenHelper helper;
//
//
//	Time time;
//	static String date = "";
//	long tmpTimeMs;
//
//
	final Handler StepCountUpdater = new Handler(){
		@Override
		public void handleMessage(Message message){
			if(message!=null) {
				switch (message.what){
					//60分毎にsharedPreference書き込み
					case MSG_SAVE_STATUS:
						long timeMs = System.currentTimeMillis();
						long delayMs = StatusSaveIntervalMs - (timeMs % StatusSaveIntervalMs);
						//0時にリセット
						if((timeMs/(1000*60*60)%24)==0){
							sharedPreferences.edit().putInt(KEY_PREF_STEP_YESTERDAY, stepToday).apply();
							stepToday = 0;
						}
						savePikachuStatus();
						StepCountUpdater.sendEmptyMessageDelayed(MSG_SAVE_STATUS, delayMs);
						break;
//					case MSG_DAILY_RESET:
//
//						break;
				}
			}else{
				return;
			}
		}
	};


	@Override
	public void onCreate(){
		super.onCreate();
		Log.d(TAG,"onCreate");

		sharedPreferences = getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE);
		stepToday = sharedPreferences.getInt(KEY_PREF_STEP_TODAY, 0);
		stepTotal = sharedPreferences.getInt(KEY_PREF_STEP_TOTAL, 0);
//		prevStep = sharedPreferences.getInt(KEY_PREF_PREV_STEP, 0);		//電源切らずにアプリを落としただけの場合残ってるはず
		friendStatusManager = new FriendStatusManager(sharedPreferences.getInt(KEY_PREF_FRIEND_POINT, 0));

		//センサー初期化
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		sensorStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

//		time = new Time("Asia/Tokyo");
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		Log.d(TAG,"onDestroy");
		if(sensorFlag) {
			mSensorManager.unregisterListener(this);
			sensorFlag = false;
		}
		mSensorManager = null;
		sensorStepCounter = null;

		//保存
		savePikachuStatus();

		//タイマー解除
		StepCountUpdater.removeMessages(MSG_SAVE_STATUS);
//		StepCountUpdater.removeMessages(MSG_DAILY_RESET);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		Log.d(TAG, "onStartCommand");

//		time.setToNow();
//		date = time.year+String.format("%02d",time.month+1)+String.format("%02d",time.monthDay);
//		prevStepCount = sharedPreferences.getInt(KEY_PREF_STEP_COUNT_SINCE_YESTERDAY,0);

		//タイマー開始
		StepCountUpdater.removeMessages(MSG_SAVE_STATUS);
		StepCountUpdater.sendEmptyMessage(MSG_SAVE_STATUS);
//		StepCountUpdater.removeMessages(MSG_DAILY_RESET);
//		StepCountUpdater.sendEmptyMessage(MSG_DAILY_RESET);

		//センサー開始
		if(!sensorFlag){
			mSensorManager.registerListener(this, sensorStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
			sensorFlag = true;
		}

		return START_STICKY;		//再起動しない
	}


	public void savePikachuStatus(){
		sharedPreferences.edit()
				.putInt(KEY_PREF_FRIEND_POINT, friendStatusManager.getFriendPoint())
				.putInt(KEY_PREF_STEP_TODAY, stepToday)
				.putInt(KEY_PREF_STEP_TOTAL, stepTotal)
				.apply();
	}


	public void notificationUpdater(){

	}


	//Watt数計算
	public int wattCounter(int step){
		int w = (wattCount + step) / 20;
		wattCount = (wattCount + step) % 20;
		return w;
	}



//	public void updateStepCount(){
//		tmpTimeMs =  System.currentTimeMillis();
//		mSensorManager.registerListener(this, sensorStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
//		sensorState=true;
//	}
//
//	//1日分の歩数をDBに記録
//	public void saveDailyStepCount(){
//		SharedPreferences sp = getSharedPreferences(KEY_PREFERENCES,Context.MODE_PRIVATE);
//		int today = sp.getInt(KEY_PREF_TODAY_STEP_COUNT, 0);
////		db.execSQL("insert into "+WearLifeLogSQLiteOpenHelper.TABLE_NAME
////				+"(date, step_count) values('"+date+"', "+today+");");
////		db.execSQL("insert into startAppList(name,pkg_name,conf) values ('アプリ１', 'com.pioneer...', 1);");
//		//新しい日にち
//		time.setToNow();
//		date = time.year+String.format("%02d",time.month+1)+String.format("%02d",time.monthDay);
//	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
			Log.d(TAG, "stepCounter:" + event.values[0]);
			if(sensorFlag) {
//				mSensorManager.unregisterListener(this, sensorStepCounter);
//				sensorFlag = false;

				if(prevStep==-1){	//初回起動時
					prevStep = (int)event.values[0];
				}else{
					int diff = (int)event.values[0] - prevStep;
					stepToday += diff;
					stepTotal += diff;
					watt += wattCounter(diff);	//ワット加算
					prevStep = (int)event.values[0];
				}




//				//SharedPreferenceに記録
//				SharedPreferences.Editor editor = sharedPreferences.edit();
//				editor.putInt(KEY_PREF_TODAY_STEP_COUNT, (int) todayStepCount);
//				editor.apply();

//				//午前2時にリセット
//				int TimeMin = (int)tmpTimeMs/(1000*60);
//				int dayAmari = TimeMin%(60*24);
//				if(dayAmari==120){	//2:00:00~2:00:59
////				if(dayAmari==0){	//0:00:00~0:00:59
//					todayStepCount = 0;
//					editor.putInt(KEY_PREF_STEP_COUNT_SINCE_YESTERDAY,(int)event.values[0]).apply();
//					saveDailyStepCount();	//DBに記録
//					Log.d("Pika","AM2:00 reset");
//				}
//				sensorState=false;
//				Log.d("Pika","Sensor unregistered [sensorChanged] today's step:"+todayStepCount);
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
