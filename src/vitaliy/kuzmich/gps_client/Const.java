package vitaliy.kuzmich.gps_client;

public class Const {
	public static final String LOG = "gps_SS";
	
	public static final String MAIN_INTENT_RECEIVER = "vitaliy.kuzmich.gps_client.MainActivity";
	public static final String settingsFileName = "settings";
	public static final String KEY_ISENABLED = "isEnabled";
	public static final String KEY_INTENT_DATA = "data";
	public static final String KEY_VIEW_ID = "viewId";
	public static final String KEY_SETT_GPS_MIN_CHANGE_DISTANCE = "gpsMinChangeDistance";
	public static final String KEY_SETT_GPS_MIN_INTERVAL = "gpsMinInterval";

	public static final String KEY_SETT_SOCKETCONN_TIMEOUT = "socketConnectionTimeOut";
	public static final String KEY_SETT_SERVER_ADDRESS = "serverAddress";
	public static final String KEY_SETT_SERVER_PORT = "serverPort";

	public static final int POSITION_MES_LENGTH = 44;
	public static final int SETTINGS_BASIC_LENGTH = 20;
	public static final int PROTOCOL_MES_LENGTH = 12;

}
