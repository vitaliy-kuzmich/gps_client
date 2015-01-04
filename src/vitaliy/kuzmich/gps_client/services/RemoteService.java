package vitaliy.kuzmich.gps_client.services;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import vitaliy.kuzmich.R;
import vitaliy.kuzmich.gps.messages.mobile.pojo.APMesg;
import vitaliy.kuzmich.gps.messages.mobile.pojo.Auth;
import vitaliy.kuzmich.gps.messages.mobile.pojo.Settings;
import vitaliy.kuzmich.gps_client.Const;
import vitaliy.kuzmich.gps_client.messages.proto.AuthMessage;
import vitaliy.kuzmich.gps_client.messages.proto.Message;
import vitaliy.kuzmich.gps_client.messages.proto.SettingsMessage;
import android.util.Log;

public class RemoteService extends AService<APMesg> {
	volatile SocketChannel socketChannel;

	private AtomicBoolean wasError = new AtomicBoolean(false);

	public AtomicBoolean getWasError() {
		return wasError;
	}

	public void setWasError(AtomicBoolean wasError) {
		this.wasError = wasError;
	}

	private AtomicBoolean isAuthenticated = new AtomicBoolean(false);
	private AtomicBoolean authInProgress = new AtomicBoolean(false);

	private GPSListenerMainService gps;
	private String username;
	private String password;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public RemoteService(GPSListenerMainService serv) {
		gps = serv;

	}

	Thread t = new Thread(new Runnable() {

		int readedLen = -1, requestId, code, len;
		ByteBuffer res = ByteBuffer.allocate(2000);

		@Override
		public void run() {
			byte[] data = null;
			while (runStatus.get() == STAT_RUN) {

				try {
					connect();
					readedLen = socketChannel.read(res);
					if (readedLen < Const.PROTOCOL_MES_LENGTH)
						continue;
					res.flip();

					try {
						len = res.getInt();
						requestId = res.getInt();
						code = res.getInt();
						if ((readedLen - Const.PROTOCOL_MES_LENGTH) == len) {
							data = new byte[len];
							res.get(data);
						} else {
							res.clear();
							res.position(readedLen - 1);
							continue;
						}

					} catch (BufferUnderflowException ex) {
						res.clear();
						res.position(readedLen - 1);
						continue;
					}
					res.compact();
					Log.i(Const.LOG, "message wth code arrived:" + code);
					switch (code) {
					case Message.AUTH_MESSAGE:
						AuthMessage msg = new AuthMessage(
								ByteBuffer.wrap(data), len, code, requestId);
						Auth a = msg.toPojo();
						if (a.getPassword().equals(password)) {
							Log.i(Const.LOG,
									"Authentication ok" + a.getUsername());
							notifyUI(R.id.serverStat, "connected");
							isAuthenticated.set(true);
							wasError.set(false);
							((StorageService) services[AService.LOCAL_STORAGE])
									.restoreToRemoteIfExists();
						} else {
							isAuthenticated.set(false);
							wasError.set(true);
							Log.i(Const.LOG,
									"Authentication failed" + a.getUsername());
							notifyUI(R.id.serverStat, a.getPassword());

						}
						authInProgress.set(false);
						break;
					case Message.SETTINGS_MESSAGE:
						SettingsMessage set = new SettingsMessage(ByteBuffer
								.wrap(data), len, code, requestId);
						gps.applyNewSettings((Settings) set.toPojo());
						break;
					default:
						break;
					}

				} catch (Exception e) {
					wasError.set(true);
					isAuthenticated.set(false);
					res.clear();

				}
				if (wasError.get()) {
					notifyUI(R.id.serverStat, "connecting error");
					synchronized (this) {
						try {
							wait(20000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

					}
				}

			}
		}
	});

	private synchronized void connect() throws Exception {
		if (socketChannel == null || !socketChannel.isConnected()
				|| wasError.get()) {
			notifyUI(R.id.serverStat, "connecting");
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(true);
			socketChannel.socket().setKeepAlive(true);
			socketChannel.socket().connect(
					new InetSocketAddress(settings.getServerAddress(),
							settings.getServerPort()),
					settings.getSocketConnectionTimeOut());

			if (socketChannel.isConnected()) {
				Auth a = new Auth(null);
				a.setCode(Message.AUTH_MESSAGE);
				a.setRequestId(1);
				a.setUsername(username);
				a.setPassword(password);
				a.setLen((username + ":" + password).getBytes().length);
				notifyUI(R.id.serverStat, "authentication");
				synchronized (t) {
					t.notify();
				}
				socketChannel.write(a.toMessage().toBuffer());

				authInProgress.set(true);
			}

		}

	}

	@Override
	public void process(APMesg msg) {
		init();
		if ((wasError.get() || !isAuthenticated.get())
				&& msg.getCode() == Message.POS_MESSAGE) {
			services[AService.LOCAL_STORAGE].putMessage(msg);

		} else if (socketChannel != null && socketChannel.isConnected()) {
			try {
				socketChannel.write(msg.toMessage().toBuffer());

			} catch (Exception e) {
				wasError.set(true);
				isAuthenticated.set(false);
				notifyUI(R.id.serverStat, "error sending message!");
				if (msg.getCode() == Message.POS_MESSAGE)
					services[AService.LOCAL_STORAGE].putMessage(msg);
			}
		}
	}

	@Override
	public void saveShutDown() {
		if (socketChannel != null && socketChannel.isConnected()) {
			try {
				socketChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		notifyUI(R.id.serverStat, "disconnected");
	}

	@Override
	public void init() {
		if (!t.isAlive()) {
			t.setDaemon(true);
			t.start();
		} else {
			synchronized (t) {
				t.notify();
			}

		}
	}
}
