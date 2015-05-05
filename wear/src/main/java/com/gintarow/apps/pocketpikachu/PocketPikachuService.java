package com.gintarow.apps.pocketpikachu;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Calendar;


/**
 * 各種ステータスを管理するサービス
 * ・歩数
 * ・ワット数
 * ・仲良し度
 */
public class PocketPikachuService extends Service implements SensorEventListener{
	public PocketPikachuService() {
	}

	static final String TAG = "PocketPikachuService";

	SensorManager mSensorManager;
	Sensor sensorStepCounter;
	boolean sensorFlag =false;

	public FriendStatusManager friendStatusManager;
	private SharedPreferences sharedPreferences;

	static final String KEY_PREFERENCES = "PocketPikachu";
	static final String KEY_PREF_STEP_TODAY = "stepToday";
	static final String KEY_PREF_STEP_DATE = "stepDate";	//今日はいつなのか
	static final String KEY_PREF_STEP_TOTAL = "stepTotal";
	static final String KEY_PREF_STEP_YESTERDAY  = "stepYesterday";
	static final String KEY_PREF_FRIEND_POINT = "friendPoint";
	static final String KEY_PREF_WATT_POINT = "WattPoint";

	int stepToday = 0;
	int stepTotal = 0;
	int prevStep = -1;

	int watt = 0;
	int wattCount = 0;

	static final int MSG_SAVE_STATUS = 0;
//	static final int MSG_DAILY_RESET = 1;
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
							Calendar calendar = Calendar.getInstance();
							String newDate = calendar.get(Calendar.YEAR)+String.format("%02d",calendar.get(Calendar.MONTH)+1)+String.format("%02d",calendar.get(Calendar.DAY_OF_MONTH));
							sharedPreferences.edit()
									.putString(KEY_PREF_STEP_DATE, newDate)
									.putInt(KEY_PREF_STEP_YESTERDAY, stepToday)
									.apply();
							stepToday = 0;
							//todo 仲良しポイント減点
						}
						savePikachuStatus();
						StepCountUpdater.sendEmptyMessageDelayed(MSG_SAVE_STATUS, delayMs);
						break;
//					case MSG_DAILY_RESET:
//
//						break;
				}
//			}else{
//				return;
			}
		}
	};


	@Override
	public void onCreate(){
		super.onCreate();
		Log.d(TAG, "onCreate");
		Calendar calendar = Calendar.getInstance();
		String date = calendar.get(Calendar.YEAR)+String.format("%02d",calendar.get(Calendar.MONTH)+1)+String.format("%02d",calendar.get(Calendar.DAY_OF_MONTH));

		sharedPreferences = getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE);
		friendStatusManager = new FriendStatusManager(sharedPreferences.getInt(KEY_PREF_FRIEND_POINT, 0));
		stepTotal = sharedPreferences.getInt(KEY_PREF_STEP_TOTAL, 0);
		String tmpDate = sharedPreferences.getString(KEY_PREF_STEP_DATE,"");
		//日が変わってたら歩数をリセット
		if(date.equals(tmpDate)) {
			stepToday = sharedPreferences.getInt(KEY_PREF_STEP_TODAY, 0);
		}else{
			sharedPreferences.edit().putString(KEY_PREF_STEP_DATE, date).apply();
			stepToday = 0;
		}
		Log.d(TAG,"Date: "+date);

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

		//通知
		notificationUpdater();

		return START_NOT_STICKY;
	}


	public void savePikachuStatus(){
		sharedPreferences.edit()
				.putInt(KEY_PREF_FRIEND_POINT, friendStatusManager.getFriendPoint())
				.putInt(KEY_PREF_STEP_TODAY, stepToday)
				.putInt(KEY_PREF_STEP_TOTAL, stepTotal)
				.putInt(KEY_PREF_WATT_POINT, watt)
				.apply();
	}


	public void notificationUpdater(){
		Log.d(TAG,"notificationUpdater");
		Intent displayIntent = new Intent(getApplicationContext(), PikachuDisplayActivity.class);

//		Bitmap bg =

		Notification secondPage = new NotificationCompat.Builder(this)
				.setContentTitle(getString(R.string.step_total))
				.setContentText(String.format("%24d 歩", stepTotal))
//				.setStyle(new NotificationCompat.InboxStyle()
//						.addLine(String.format("Total:%12d歩", stepTotal))
//						.addLine(String.format("%20dワット",watt)))
				.build();

		Notification thirdPage = new NotificationCompat.Builder(this)
				.setContentTitle(getString(R.string.notif_title_friend))
				.setContentText("なみなカンジ Now!")
				.build();


		Notification notification = new Notification.Builder(getApplicationContext())
				.setPriority(1)
				.setSmallIcon(R.mipmap.pika_icon_a)
				.setColor(0x00000000)
				.setContentTitle(getString(R.string.step_today) + String.format("%12d 歩", stepToday))
				.setContentText(String.format("%21d Watt", watt))
				.extend(new Notification.WearableExtender()
						.addPage(secondPage))
//						.addPage(thirdPage))
//						.setDisplayIntent(PendingIntent.getActivity(getApplicationContext(), 0, displayIntent,
//								PendingIntent.FLAG_UPDATE_CURRENT)))
				.setColor(0xffff00)
				.addAction(R.mipmap.pika_icon_a,    //todo 白地アイコン作成
						getString(R.string.show_status),
						PendingIntent.getActivity(getApplicationContext(), 0, displayIntent, PendingIntent.FLAG_UPDATE_CURRENT))
				.build();
		((NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, notification);
	}


	//Watt数計算
	public int wattCounter(int step){
		int w = (wattCount + step) / 20;
		wattCount = (wattCount + step) % 20;
		return w;
	}



//	public void updateStepCount(){
//		tmpTimeMs =  System.currentTimeMillis();String.format("Total:%12d歩", stepTotal)
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
