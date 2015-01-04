package vitaliy.kuzmich.gps_client.messages.proto;

public interface AM {
	public int getLen();

	public void setLen(int len);

	public int getCode();

	public void setCode(int code);

	public int getRequestId();

	public void setRequestId(int requestId);

}
