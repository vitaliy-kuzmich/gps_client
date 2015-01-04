package vitaliy.kuzmich.gps_client.messages.proto;

import java.nio.ByteBuffer;

import vitaliy.kuzmich.gps.messages.mobile.pojo.APMesg;
import vitaliy.kuzmich.gps.messages.mobile.pojo.Position;

public class PositionMessage extends Message {
	public PositionMessage(ByteBuffer buffer, int len, int code, int requestId) {
		super(buffer, len, code, requestId);
	}

	public PositionMessage(AM am) {
		super(am);
		Position a = (Position) am;
		buffer = ByteBuffer.allocate(getLen());

		buffer.putFloat(a.getAccuracy());
		buffer.putDouble(a.getAltitude());
		buffer.putDouble(a.getLatitude());
		buffer.putDouble(a.getLongtitude());
		buffer.putFloat(a.getSpeed());
		buffer.putFloat(a.getBearing());
		buffer.putLong(a.getCreationTime());

	}

	@Override
	public Position toPojo() {
		Position pos = new Position(this);

		pos.setAccuracy(buffer.getFloat());
		pos.setAltitude(buffer.getDouble());
		pos.setLatitude(buffer.getDouble());
		pos.setLongtitude(buffer.getDouble());
		pos.setSpeed(buffer.getFloat());
		pos.setBearing(buffer.getFloat());
		pos.setCreationTime(buffer.getLong());

		return pos;
	}
}
