package vitaliy.kuzmich.gps_client.services;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import vitaliy.kuzmich.gps.messages.mobile.pojo.InteruptQUEUE;
import vitaliy.kuzmich.gps.messages.mobile.pojo.Settings;
import vitaliy.kuzmich.gps_client.Const;
import android.content.Intent;
import android.util.Log;

public abstract class AService<T> implements Runnable {
	public static final int LOCAL_STORAGE = 0;
	public static final int REMOTE_SERVER = 1;
	public static final int BROADCAST_SENDER = 2;
	public static final int TEST_SENDER = 3;

	public static final int STAT_RUN = 1;
	public static final int STAT_PAUSED = 2;
	public static final int STAT_STOPPED = 3;
	protected final AtomicInteger runStatus = new AtomicInteger(0);
	protected final BlockingQueue<T> queue = new LinkedBlockingQueue<T>();
	protected AService services[];
	protected Settings settings;

	public Settings getSettings() {
		return settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	public void notifyUI(int viewId, String msg) {
		Intent request = new Intent(Const.MAIN_INTENT_RECEIVER);
		request.putExtra(Const.KEY_VIEW_ID, viewId);
		request.putExtra(Const.KEY_INTENT_DATA, msg);
		services[AService.BROADCAST_SENDER].putMessage(request);
	}

	public AService[] getServices() {
		return services;
	}

	public void setServices(AService[] services) {
		this.services = services;
	}

	public void putMessage(T msg) {
		try {
			queue.put(msg);
		} catch (InterruptedException e) {
			Log.e(Const.LOG, "Unable to put message so service");
		}

	}

	@Override
	public void run() {
		runStatus.set(STAT_RUN);
		init();
		while (true) {
			if (runStatus.get() == STAT_STOPPED)
				break;
			if (runStatus.get() == STAT_PAUSED) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
			try {
				Object ob = queue.take();
				if (ob instanceof InteruptQUEUE)
					break;
				else
					process((T) ob);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		saveShutDown();
	}

	public void stop() {
		runStatus.set(STAT_STOPPED);
		try {
			queue.put((T) new InteruptQUEUE(null));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public void pause() {
		runStatus.set(STAT_PAUSED);
	}

	public void restore() {
		runStatus.set(STAT_RUN);
		synchronized (this) {
			this.notifyAll();
		}
	}

	protected abstract void init();

	protected abstract void process(T msg);

	protected abstract void saveShutDown();
}
