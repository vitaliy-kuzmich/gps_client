package vitaliy.kuzmich.gps.messages.mobile.pojo;

import vitaliy.kuzmich.gps_client.messages.proto.AM;
import vitaliy.kuzmich.gps_client.messages.proto.Message;

public class InteruptQUEUE extends APMesg {

	public InteruptQUEUE(AM am) {
		super(am);
	}

	/*@Override
	public void readExternal(ObjectInput arg0) throws IOException,
			ClassNotFoundException {
		
	}

	@Override
	public void writeExternal(ObjectOutput arg0) throws IOException {
		
	}*/

	@Override
	public Message toMessage() {
		return null;
	}

}
