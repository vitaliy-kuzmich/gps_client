package vitaliy.kuzmich.gps_client.services;

import java.io.File;

import vitaliy.kuzmich.R;
import vitaliy.kuzmich.gps.messages.mobile.pojo.Position;
import vitaliy.kuzmich.gps_client.Const;
import vitaliy.kuzmich.gps_client.messages.proto.Message;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GPSListenerMainService extends Service implements LocationListener {

	AService[] services;
	static File FILES_DIR;
	LocationManager locationManager;
	private vitaliy.kuzmich.gps.messages.mobile.pojo.Settings settings;
	WakeLock wakeLock;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(Const.LOG, "service created");
		services = new AService[3];
		services[AService.LOCAL_STORAGE] = new StorageService();
		services[AService.REMOTE_SERVER] = new RemoteService(this);
		services[AService.BROADCAST_SENDER] = new InfoBroadCastSender(this);
		PowerManager pm = (PowerManager) getSystemService(this.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotSleep");
		wakeLock.acquire();

	}

	Listener gpsStatlistener = new Listener() {
		public int connected, visible, count;

		@Override
		public void onGpsStatusChanged(int event) {
			if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS
					|| event == GpsStatus.GPS_EVENT_FIRST_FIX) {
				GpsStatus status = locationManager.getGpsStatus(null);
				Iterable<GpsSatellite> sats = status.getSatellites();
				connected = 0;
				visible = 0;
				count = 0;
				for (GpsSatellite sat : sats) {

					if (sat.usedInFix())
						connected++;
					else if (sat.getSnr() > 0)
						visible++;

					count++;
				}
				notifyUI(R.id.gpsStat, "count/visible/connected   " + count
						+ "/" + visible + "/" + connected);
			}
		}
	};

	public void start() {
		Log.i(Const.LOG, "initializing services");
		Thread t = null;
		for (AService s : services) {
			s.setServices(services);
			s.setSettings(settings);
			t = new Thread(s);
			// t.setDaemon(true);
			t.start();
		}

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				1000, 2f, this);
		locationManager.addGpsStatusListener(gpsStatlistener);
		Intent mainResp = new Intent(Const.MAIN_INTENT_RECEIVER);
		mainResp.putExtra(Const.KEY_ISENABLED, true);
		sendBroadcast(mainResp);
	}

	public void stop() {
		for (int i = 0; i < services.length; i++) {
			if (i == AService.BROADCAST_SENDER)
				continue;
			services[i].stop();
		}

		Intent mainResp = new Intent(Const.MAIN_INTENT_RECEIVER);
		if (locationManager != null) {
			locationManager.removeUpdates(this);
			locationManager.removeGpsStatusListener(gpsStatlistener);
		}
		mainResp.putExtra(Const.KEY_ISENABLED, false);
		sendBroadcast(mainResp);
		synchronized (this) {
			try {
				wait(1000);
			} catch (InterruptedException e) {
			}
		}
		services[AService.BROADCAST_SENDER].stop();
	}

	@Override
	public void onDestroy() {
		FILES_DIR = null;
		synchronized (this) {
			stop();
		}
		super.onDestroy();
		wakeLock.release();

	}

	public void applyNewSettings(
			vitaliy.kuzmich.gps.messages.mobile.pojo.Settings st) {
		synchronized (this) {

			settings = st;
			Editor e = sp.edit();
			e.putFloat(Const.KEY_SETT_GPS_MIN_CHANGE_DISTANCE,
					settings.getGpsMinChangeDistance());

			e.putLong(Const.KEY_SETT_GPS_MIN_INTERVAL,
					settings.getGpsMinInterval());

			e.putString(Const.KEY_SETT_SERVER_ADDRESS,
					settings.getServerAddress());
			e.putInt(Const.KEY_SETT_SERVER_PORT, settings.getServerPort());
			e.putInt(Const.KEY_SETT_SOCKETCONN_TIMEOUT,
					settings.getSocketConnectionTimeOut());

			e.commit();
			stop();
			start();
		}
	}

	@Override
	public void onLocationChanged(Location loc) {
		Position pos = new Position(null);
		pos.setAccuracy(loc.getAccuracy());
		pos.setAltitude(loc.getAltitude());
		pos.setCode(Message.POS_MESSAGE);
		pos.setLen(Const.POSITION_MES_LENGTH);
		pos.setRequestId(3434234);
		pos.setLatitude(loc.getLatitude());
		pos.setLongtitude(loc.getLongitude());
		pos.setSpeed(loc.getSpeed());
		pos.setBearing(loc.getBearing());
		pos.setCreationTime(System.currentTimeMillis());
		services[AService.REMOTE_SERVER].putMessage(pos);
	}

	@Override
	public void onProviderDisabled(String arg0) {
		notifyUI(R.id.gpsStat, "provider disabled " + arg0);
	}

	@Override
	public void onProviderEnabled(String arg0) {
		notifyUI(R.id.gpsStat, "provider enabled " + arg0);
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {

		String inf = "";
		if (arg1 == LocationProvider.OUT_OF_SERVICE)
			inf = "out of service";
		else if (arg1 == LocationProvider.TEMPORARILY_UNAVAILABLE)
			inf = "temporary unawaliable";
		else
			inf = "ok";
		notifyUI(R.id.gpsStat, inf);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	SharedPreferences sp;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(Const.LOG, "service started");
		SharedPreferences p = getSharedPreferences(Const.settingsFileName,
				MODE_PRIVATE);
		if (!p.getBoolean(Const.KEY_ISENABLED, false))
			stopSelf();
		if (FILES_DIR != null)
			return START_NOT_STICKY;
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		initSettings(sp);
		FILES_DIR = getFilesDir();

		Notification notification = new NotificationCompat.Builder(this)
				.setContentTitle("gps listener").build();
		startForeground(777, notification);
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			notifyUI(R.id.gpsStat, "initializing");
			synchronized (this) {
				start();
			}

		} else {
			disableRuning();

		}
		return START_STICKY;
	}

	private void disableRuning() {
		Intent mainResp = new Intent(Const.MAIN_INTENT_RECEIVER);
		/*
		 * Intent intent2 = new
		 * Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		 * 
		 * intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		 * getApplication().startActivity(intent2);
		 */

		mainResp.putExtra(Const.KEY_ISENABLED, false);
		sendBroadcast(mainResp);
		stopSelf();

	}

	public void notifyUI(int viewId, String msg) {
		Intent request = new Intent(Const.MAIN_INTENT_RECEIVER);
		request.putExtra(Const.KEY_VIEW_ID, viewId);
		request.putExtra(Const.KEY_INTENT_DATA, msg);
		services[AService.BROADCAST_SENDER].putMessage(request);
	}

	public void initSettings(SharedPreferences p) {
		try {
			settings = new vitaliy.kuzmich.gps.messages.mobile.pojo.Settings(
					null);
			settings.setGpsMinChangeDistance(Float.valueOf(p.getString(
					Const.KEY_SETT_GPS_MIN_CHANGE_DISTANCE, "2")));
			settings.setGpsMinInterval(Long.valueOf(p.getString(
					Const.KEY_SETT_GPS_MIN_INTERVAL, "1000")));
			settings.setServerAddress(p.getString(
					Const.KEY_SETT_SERVER_ADDRESS, "192.168.0.219"));
			settings.setServerPort(Integer.valueOf(p.getString(
					Const.KEY_SETT_SERVER_PORT, "8088")));
			settings.setSocketConnectionTimeOut(Integer.valueOf(p.getString(
					Const.KEY_SETT_SOCKETCONN_TIMEOUT, "50000")));

			String un = p.getString("username", "user");
			String ps = p.getString("password", "");
			((RemoteService) services[AService.REMOTE_SERVER]).setPassword(un);
			((RemoteService) services[AService.REMOTE_SERVER]).setUsername(ps);

		} catch (Exception ex) {
			notifyUI(R.id.gpsStat, "Settings error!");
			disableRuning();
			stopSelf();
		}
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
				+ (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
