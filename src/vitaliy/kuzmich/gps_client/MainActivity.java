package vitaliy.kuzmich.gps_client;

import vitaliy.kuzmich.R;
import vitaliy.kuzmich.gps_client.services.GPSListenerMainService;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class MainActivity extends Activity {
	public TextView gpsStat;
	public TextView localCache;
	public TextView serverStat;
	public CheckBox isEnabled;
	Receiver receiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		gpsStat = (TextView) findViewById(R.id.gpsStat);
		localCache = (TextView) findViewById(R.id.cacheStat);
		serverStat = (TextView) findViewById(R.id.serverStat);
		isEnabled = (CheckBox) findViewById(R.id.isEnabled);
		receiver = new Receiver(this);
		IntentFilter tmp = new IntentFilter(Const.MAIN_INTENT_RECEIVER);
		registerReceiver(receiver, tmp);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
	}

	SharedPreferences sp;

	@Override
	protected void onResume() {
		super.onResume();
		restoreState();

	}

	public void restoreState() {
		SharedPreferences p = getSharedPreferences(Const.settingsFileName,
				MODE_PRIVATE);
		boolean isEn = p.getBoolean(Const.KEY_ISENABLED, false);
		isEnabled.setChecked(isEn);
		toogleRun(isEnabled.isChecked());
	}

	public void gps(View v) {
		CheckBox vv = (CheckBox) v;
		SharedPreferences p = getSharedPreferences(Const.settingsFileName,
				MODE_PRIVATE);
		p.edit().putBoolean(Const.KEY_ISENABLED, vv.isChecked()).commit();
		toogleRun(vv.isChecked());
	}

	private void toogleRun(boolean t) {
		if (t) {
			Intent i = new Intent(this, GPSListenerMainService.class);
			startService(i);
		} else {
			Intent i = new Intent(this, GPSListenerMainService.class);
			stopService(i);
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();

	}

	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) {
		switch (keycode) {
		case KeyEvent.KEYCODE_MENU:
			startActivity(new Intent(this, PrefActivity.class));
			return true;
		}

		return super.onKeyDown(keycode, e);
	}
}
