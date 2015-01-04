package vitaliy.kuzmich.gps_client.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import vitaliy.kuzmich.R;
import vitaliy.kuzmich.gps.messages.mobile.pojo.APMesg;
import vitaliy.kuzmich.gps.messages.mobile.pojo.Position;
import vitaliy.kuzmich.gps_client.Const;
import vitaliy.kuzmich.gps_client.messages.proto.Message;

public class StorageService extends AService<APMesg> {
	private File file;

	@Override
	public void process(APMesg msg) {
		FileOutputStream res;
		notifyUI(R.id.cacheStat, "saving coordinates");
		synchronized (this) {
			try {
				res = new FileOutputStream(file, true);
				res.write(msg.toMessage().toBytes());
				res.flush();
				res.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		notifyUI(
				R.id.cacheStat,
				"saved "
						+ GPSListenerMainService.humanReadableByteCount(
								file.length(), false));
	}

	@Override
	public void init() {
		file = new File(GPSListenerMainService.FILES_DIR.getAbsolutePath()
				+ "/data.bin");
		notifyUI(
				R.id.cacheStat,
				"storage size "
						+ GPSListenerMainService.humanReadableByteCount(
								file.length(), false));
	}

	public void restoreToRemoteIfExists() {
		FileInputStream in;
		RemoteService remote = (RemoteService) services[AService.REMOTE_SERVER];
		if (file.exists()) {
			notifyUI(R.id.cacheStat, "restoring to server");
			synchronized (this) {
				try {
					in = new FileInputStream(file);
					byte[] tmpBuf = new byte[Const.POSITION_MES_LENGTH
							+ Const.PROTOCOL_MES_LENGTH];
					long fileLen = file.length();
					while (in.read(tmpBuf) > 0) {
						remote.putMessage(((Position) Message.toMessage(tmpBuf)
								.toPojo()));
						fileLen -= tmpBuf.length;
						tmpBuf = new byte[Const.POSITION_MES_LENGTH
								+ Const.PROTOCOL_MES_LENGTH];
						notifyUI(
								R.id.cacheStat,
								GPSListenerMainService.humanReadableByteCount(
										fileLen, false) + " left");
						try {
							wait(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (remote.getWasError().get()) {
							notifyUI(R.id.cacheStat, "restoring breaked");
							return;
						}

					}

					file.delete();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			notifyUI(R.id.cacheStat, "restoring done");
		}
	}

	@Override
	public void saveShutDown() {

	}

}
