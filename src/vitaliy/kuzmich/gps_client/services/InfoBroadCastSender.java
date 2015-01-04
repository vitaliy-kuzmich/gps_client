package vitaliy.kuzmich.gps_client.services;

import android.content.Intent;

public class InfoBroadCastSender extends AService<Intent> {
	GPSListenerMainService gpsListener;

	public InfoBroadCastSender(GPSListenerMainService gpsListener) {
		this.gpsListener = gpsListener;
	}

	@Override
	protected void init() {

	}

	@Override
	protected void process(Intent msg) {
		gpsListener.sendBroadcast(msg);
	}

	@Override
	protected void saveShutDown() {

	}

}
