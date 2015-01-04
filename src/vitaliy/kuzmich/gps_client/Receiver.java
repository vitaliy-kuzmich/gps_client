package vitaliy.kuzmich.gps_client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

public class Receiver extends BroadcastReceiver {
	MainActivity main;

	public Receiver(MainActivity act) {
		main = act;
	}

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		int viewId = arg1.getIntExtra("viewId", -1);
		if (viewId < 0) {
			boolean isEnabled = arg1.getExtras().getBoolean(
					Const.KEY_ISENABLED, false);
			main.isEnabled.setChecked(isEnabled);
		} else {
			((TextView) main.findViewById(viewId)).setText(arg1.getExtras()
					.getString(Const.KEY_INTENT_DATA));
		}
	}
}
