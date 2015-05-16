package com.gintarow.apps.pocketpikachu;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.lang.ref.WeakReference;
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
	static final String KEY_PREF_WATT_POINT = "wattPoint";
	static final String KEY_PREF_WATT_COUNT = "wattCount";

	static final int MSG_SAVE_STATUS = 0;
	static final int MSG_UPDATE_NOTIF_15 = 15;
	static final int MSG_UPDATE_NOTIF_30 = 30;
	static final int MSG_UPDATE_NOTIF_45 = 45;
	static final int MSG_SET_UPDATE = 1;
	static final long StatusSaveIntervalMs = 60 * 60 * 1000;
	static final long NotificationUpdateIntervalMs = 15 * 60 * 1000;

	int stepToday = 0;
	int stepTotal = 0;
	int prevStep = -1;
	int watt = 0;
	int wattCount = 0;
	int currentMsgNotifNum = MSG_SET_UPDATE;

	static final int NOTIFICATION_ID = 0;
	NotificationManager notificationManager;
	Notification secondPage;
	Notification thirdPage;
	Bitmap bgNotif;
	Intent displayIntent;
	String today;


	//	private SQLiteDatabase db;

	//	private WearLifeLogSQLiteOpenHelper helper;
//
//
//	Time time;
//	static String date = "";
//	long tmpTimeMs;
//
//

	private static class UpdateHandler extends Handler{
		private final WeakReference<PocketPikachuService> service;
		UpdateHandler(PocketPikachuService s){
			service = new WeakReference<PocketPikachuService>(s);
		}

		@Override
		public void handleMessage(Message message){
			PocketPikachuService s = this.service.get();
			if(message!=null && s!=null) {
				long timeMs = System.currentTimeMillis();
				long delayMs;// = StatusSaveIntervalMs - (timeMs % StatusSaveIntervalMs);
				Calendar calendar;
				switch (message.what){
					//60分毎にsharedPreference書き込み
					case MSG_SAVE_STATUS:
//						long timeMs = System.currentTimeMillis();
						Log.d(TAG,"update:00");
						delayMs = StatusSaveIntervalMs - (timeMs % StatusSaveIntervalMs);
						calendar = Calendar.getInstance();
						String date = calendar.get(Calendar.YEAR)+String.format("%02d",calendar.get(Calendar.MONTH)+1)+String.format("%02d",calendar.get(Calendar.DAY_OF_MONTH));
						//0時にリセット
						if(!s.today.equals(date)){	//日が変わってたら
							Log.d(TAG,"Daily Update");
							s.today = date;
							s.sharedPreferences.edit()
									.putString(KEY_PREF_STEP_DATE, s.today)
									.putInt(KEY_PREF_STEP_YESTERDAY, s.stepToday)
									.apply();
							s.stepToday = 0;
							//todo 仲良しポイント減点
						}
						s.savePikachuStatus();
						s.notificationUpdater();
						s.currentMsgNotifNum = MSG_UPDATE_NOTIF_15;
						s.StepCountUpdater.sendEmptyMessageDelayed(s.currentMsgNotifNum, delayMs);
						break;
					case MSG_SET_UPDATE:	//最初だけ
						calendar = Calendar.getInstance();
						int min = calendar.get(Calendar.MINUTE);
						if(min<15){
							s.currentMsgNotifNum = MSG_UPDATE_NOTIF_15;
						}else if(min<30){
							s.currentMsgNotifNum = MSG_UPDATE_NOTIF_30;
						}else if(min<45){
							s.currentMsgNotifNum = MSG_UPDATE_NOTIF_45;
						}else{
							s.currentMsgNotifNum = MSG_SAVE_STATUS;
						}
						delayMs = NotificationUpdateIntervalMs - (timeMs % NotificationUpdateIntervalMs);
//						notificationUpdater();
						s.StepCountUpdater.sendEmptyMessageDelayed(s.currentMsgNotifNum, delayMs);
						break;
					case MSG_UPDATE_NOTIF_15:	//15分
						Log.d(TAG,"update:15");
						delayMs = NotificationUpdateIntervalMs - (timeMs % NotificationUpdateIntervalMs);
						s.savePikachuStatus();
						s.notificationUpdater();
						s.currentMsgNotifNum = MSG_UPDATE_NOTIF_30;
						s.StepCountUpdater.sendEmptyMessageDelayed(s.currentMsgNotifNum, delayMs);
						break;
					case MSG_UPDATE_NOTIF_30:	//30分
						Log.d(TAG,"update:30");
						delayMs = NotificationUpdateIntervalMs - (timeMs % NotificationUpdateIntervalMs);
						s.savePikachuStatus();
						s.notificationUpdater();
						s.currentMsgNotifNum = MSG_UPDATE_NOTIF_45;
						s.StepCountUpdater.sendEmptyMessageDelayed(s.currentMsgNotifNum, delayMs);
						break;
					case MSG_UPDATE_NOTIF_45:	//45分
						Log.d(TAG,"update:45");
						delayMs = NotificationUpdateIntervalMs - (timeMs % NotificationUpdateIntervalMs);
						s.savePikachuStatus();
						s.notificationUpdater();
						s.currentMsgNotifNum = MSG_SAVE_STATUS;
						s.StepCountUpdater.sendEmptyMessageDelayed(s.currentMsgNotifNum, delayMs);
						break;
				}
			}
		}

	}

	final UpdateHandler StepCountUpdater = new UpdateHandler(this);

//	final Handler StepCountUpdater = new Handler(){
//		@Override
//		public void handleMessage(Message message){
//			if(message!=null) {
//				long timeMs = System.currentTimeMillis();
//				long delayMs;// = StatusSaveIntervalMs - (timeMs % StatusSaveIntervalMs);
//				Calendar calendar;
//				switch (message.what){
//					//60分毎にsharedPreference書き込み
//					case MSG_SAVE_STATUS:
////						long timeMs = System.currentTimeMillis();
//						Log.d(TAG,"update:00");
//						delayMs = StatusSaveIntervalMs - (timeMs % StatusSaveIntervalMs);
//						calendar = Calendar.getInstance();
//						String date = calendar.get(Calendar.YEAR)+String.format("%02d",calendar.get(Calendar.MONTH)+1)+String.format("%02d",calendar.get(Calendar.DAY_OF_MONTH));
//						//0時にリセット
//						if(!today.equals(date)){	//日が変わってたら
//							Log.d(TAG,"Daily Update");
//							today = date;
//							sharedPreferences.edit()
//									.putString(KEY_PREF_STEP_DATE, today)
//									.putInt(KEY_PREF_STEP_YESTERDAY, stepToday)
//									.apply();
//							stepToday = 0;
//							//todo 仲良しポイント減点
//						}
//						savePikachuStatus();
//						notificationUpdater();
//						currentMsgNotifNum = MSG_UPDATE_NOTIF_15;
//						StepCountUpdater.sendEmptyMessageDelayed(currentMsgNotifNum, delayMs);
//						break;
//					case MSG_SET_UPDATE:	//最初だけ
//						calendar = Calendar.getInstance();
//						int min = calendar.get(Calendar.MINUTE);
//						if(min<15){
//							currentMsgNotifNum = MSG_UPDATE_NOTIF_15;
//						}else if(min<30){
//							currentMsgNotifNum = MSG_UPDATE_NOTIF_30;
//						}else if(min<45){
//							currentMsgNotifNum = MSG_UPDATE_NOTIF_45;
//						}else{
//							currentMsgNotifNum = MSG_SAVE_STATUS;
//						}
//						delayMs = NotificationUpdateIntervalMs - (timeMs % NotificationUpdateIntervalMs);
////						notificationUpdater();
//						StepCountUpdater.sendEmptyMessageDelayed(currentMsgNotifNum, delayMs);
//						break;
//					case MSG_UPDATE_NOTIF_15:	//15分
//						Log.d(TAG,"update:15");
//						delayMs = NotificationUpdateIntervalMs - (timeMs % NotificationUpdateIntervalMs);
//						savePikachuStatus();
//						notificationUpdater();
//						currentMsgNotifNum = MSG_UPDATE_NOTIF_30;
//						StepCountUpdater.sendEmptyMessageDelayed(currentMsgNotifNum, delayMs);
//						break;
//					case MSG_UPDATE_NOTIF_30:	//30分
//						Log.d(TAG,"update:30");
//						delayMs = NotificationUpdateIntervalMs - (timeMs % NotificationUpdateIntervalMs);
//						savePikachuStatus();
//						notificationUpdater();
//						currentMsgNotifNum = MSG_UPDATE_NOTIF_45;
//						StepCountUpdater.sendEmptyMessageDelayed(currentMsgNotifNum, delayMs);
//						break;
//					case MSG_UPDATE_NOTIF_45:	//45分
//						Log.d(TAG,"update:45");
//						delayMs = NotificationUpdateIntervalMs - (timeMs % NotificationUpdateIntervalMs);
//						savePikachuStatus();
//						notificationUpdater();
//						currentMsgNotifNum = MSG_SAVE_STATUS;
//						StepCountUpdater.sendEmptyMessageDelayed(currentMsgNotifNum, delayMs);
//						break;
//				}
//			}
//		}
//	};


	@Override
	public void onCreate(){
		super.onCreate();
		Log.d(TAG, "onCreate");
		Calendar calendar = Calendar.getInstance();
		String date = calendar.get(Calendar.YEAR)+String.format("%02d",calendar.get(Calendar.MONTH)+1)+String.format("%02d",calendar.get(Calendar.DAY_OF_MONTH));

		sharedPreferences = getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE);
		watt = sharedPreferences.getInt(KEY_PREF_WATT_POINT, 0);
		wattCount = sharedPreferences.getInt(KEY_PREF_WATT_COUNT, 0);
		friendStatusManager = new FriendStatusManager(sharedPreferences.getInt(KEY_PREF_FRIEND_POINT, 0));
		stepTotal = sharedPreferences.getInt(KEY_PREF_STEP_TOTAL, 0);
		today = sharedPreferences.getString(KEY_PREF_STEP_DATE, "");
		//日が変わってたら歩数をリセット
		if(date.equals(today)) {
			stepToday = sharedPreferences.getInt(KEY_PREF_STEP_TODAY, 0);
		}else{
			today = date;
			sharedPreferences.edit().putString(KEY_PREF_STEP_DATE, today).apply();
			stepToday = 0;
		}
		Log.d(TAG,"Date: "+date+", stepTotal:"+stepTotal);

		//Notification関連初期設定
		notificationManager = ((NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE));
		bgNotif = BitmapFactory.decodeResource(getResources(), R.mipmap.pika_bg);
			// todo 仲良し度に合わせた画像		背景は画面、ピカチュウはカラー → GIMPでつくる
		displayIntent = new Intent(getApplicationContext(), PikachuDisplayActivity.class);

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
		StepCountUpdater.removeMessages(currentMsgNotifNum);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		Log.d(TAG, "onStartCommand");

		//タイマー開始
		StepCountUpdater.removeMessages(currentMsgNotifNum);
		StepCountUpdater.sendEmptyMessage(MSG_SET_UPDATE);


		//センサー開始
		if(!sensorFlag){
			mSensorManager.registerListener(this, sensorStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
			sensorFlag = true;
		}

		//通知
		notificationUpdater();

		return START_STICKY;
	}


	public void savePikachuStatus(){
		sharedPreferences.edit()
				.putInt(KEY_PREF_FRIEND_POINT, friendStatusManager.getFriendPoint())
				.putInt(KEY_PREF_STEP_TODAY, stepToday)
				.putInt(KEY_PREF_STEP_TOTAL, stepTotal)
				.putInt(KEY_PREF_WATT_POINT, watt)
				.putInt(KEY_PREF_WATT_COUNT, wattCount)
				.apply();
	}


	public void notificationUpdater(){	//todo 通知の更新
		Log.d(TAG,"notificationUpdater stepToday:"+stepToday+", stepTotal:"+stepTotal);

		secondPage = new NotificationCompat.Builder(this)
				.setContentTitle(getString(R.string.step_total))
				.setContentText(String.format("%24d 歩", stepTotal))
				.build();

//		thirdPage = new NotificationCompat.Builder(this)
//				.setContentTitle(getString(R.string.notif_title_friend))
//				.setContentText("なみなカンジ Now!")
//				.build();

		Notification notification = new Notification.Builder(getApplicationContext())
				.setPriority(1)
				.setSmallIcon(R.mipmap.pika_icon_a)
				.setLargeIcon(bgNotif)
				.setContentTitle(String.format(getString(R.string.step_today)+"%10d 歩", stepToday))
				.setContentText(String.format("%19d Watt", watt))
				.extend(new Notification.WearableExtender()
						.addPage(secondPage)
//						.setBackground(bgNotif)
						.setCustomSizePreset(Notification.WearableExtender.SIZE_XSMALL))
//						.addPage(thirdPage))
//						//１ページ目にActivityの内容を表示
//						.setDisplayIntent(PendingIntent.getActivity(getApplicationContext(), 0, displayIntent,
//								PendingIntent.FLAG_UPDATE_CURRENT)))
				.addAction(R.mipmap.pika_icon_a,    //todo 白地アイコン作成
						getString(R.string.show_status),
						PendingIntent.getActivity(getApplicationContext(), 0, displayIntent, PendingIntent.FLAG_UPDATE_CURRENT))
				.build();

		notificationManager.notify(NOTIFICATION_ID, notification);
	}


	//Watt数計算
	public int wattCounter(int step){
		int w = (wattCount + step) / 20;
		wattCount = (wattCount + step) % 20;
		return w;
	}




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
			}
		}
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
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
