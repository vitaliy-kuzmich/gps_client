package vitaliy.kuzmich.gps_client.messages.proto;

import java.nio.ByteBuffer;

import vitaliy.kuzmich.gps.messages.mobile.pojo.APMesg;
import vitaliy.kuzmich.gps_client.Const;

public abstract class Message implements AM {
	protected ByteBuffer buffer;
	private int len;
	private int code;

	public Message(ByteBuffer buffer, int len, int code, int requestId) {
		this.buffer = buffer;
		this.len = len;
		this.code = code;
		this.requestId = requestId;
	}

	private int requestId;

	public Message(AM a) {
		if (a != null) {
			setLen(a.getLen());
			setCode(a.getCode());
			setRequestId(a.getRequestId());
		}
	}

	public int getLen() {
		return len;
	}

	public void setLen(int len) {
		this.len = len;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public int getRequestId() {
		return requestId;
	}

	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	public Message(byte[] buf) {
		ByteBuffer tmp = ByteBuffer.wrap(buf);
	}

	public static final int AUTH_MESSAGE = 1;
	public static final int POS_MESSAGE = 2;
	public static final int SETTINGS_MESSAGE = 3;

	public abstract APMesg toPojo();

	public byte[] toBytes() {
		return toBuffer().array();
	}

	public ByteBuffer toBuffer() {
		ByteBuffer res = ByteBuffer.allocate(len + Const.PROTOCOL_MES_LENGTH);
		res.putInt(len);
		res.putInt(getRequestId());
		res.putInt(getCode());
		res.put(buffer.array());
		res.flip();
		return res;
	}

	public static Message toMessage(byte[] buf) {
		ByteBuffer tmp = ByteBuffer.wrap(buf);

		int len = tmp.getInt();
		int reqId = tmp.getInt();
		int code = tmp.getInt();
		tmp = tmp.slice();
		Message res = null;
		switch (code) {
		default:
			res = new PositionMessage(tmp, len, code, reqId);
			break;
		}
		return res;

	}

}
