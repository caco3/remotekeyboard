package de.caco3.remotekeyboard;

import java.io.IOException;
import java.util.HashMap;

import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.inputmethodservice.InputMethodService;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

public class RemoteKeyboardService extends InputMethodService implements
		OnKeyboardActionListener {

	public static final String TAG = "RemoteKeyboardService";

	/**
	 * For referencing our notification in the notification area.
	 */
	public static final int NOTIFICATION = 42;

	private static final String CHANNEL_ID = "remotekeyboard_service";

	/**
	 * For posting InputActions on the UI thread.
	 */
	protected Handler handler;

	/**
	 * Reference to the running service
	 */
	protected static RemoteKeyboardService self;

	/**
	 * Contains key/value replacement pairs
	 */
	protected HashMap<String, String> replacements;

	/**
	 * Reference to the HTTPS web server instance
	 */
	private WebKeyboardServer webServer;

	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		self = this;
		handler = new Handler();

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
					CHANNEL_ID,
					getString(R.string.app_name),
					NotificationManager.IMPORTANCE_LOW);
			channel.setShowBadge(false);
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
		}

		webServer = new WebKeyboardServer();
		try {
			webServer.start(this);
		} catch (IOException e) {
			Log.e(TAG, "Failed to start web server: " + e.getMessage(), e);
		}

		updateNotification();
		loadReplacements();
	}

	@Override
	public boolean onEvaluateFullscreenMode() {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
		return p.getBoolean("pref_fullscreen", false);
	}

	@Override
	public View onCreateInputView() {
		return null;
	}

	@Override
	public void onInitializeInterface() {
		super.onInitializeInterface();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (webServer != null) {
			webServer.stop();
		}
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION);
		self = null;
	}

	@Override
	public void onPress(int primaryCode) {
		if (primaryCode == 0) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showInputMethodPicker();
		}
	}

	@Override
	public void onRelease(int primaryCode) {
	}

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {
	}

	@Override
	public void onText(CharSequence text) {
	}

	@Override
	public void swipeLeft() {
	}

	@Override
	public void swipeRight() {
	}

	@Override
	public void swipeDown() {
	}

	@Override
	public void swipeUp() {
	}

	/**
	 * Update the notification to show the HTTPS server URL.
	 */
	protected void updateNotification() {
		// FIXME: This is anything but pretty! Apparently someone at Google thinks
		// that WLAN is ipv4 only.
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int addr = wifiInfo.getIpAddress();
		String ip = (addr & 0xFF) + "." + ((addr >> 8) & 0xFF) + "."
				+ ((addr >> 16) & 0xFF) + "." + ((addr >> 24) & 0xFF);

		String title = getResources().getString(R.string.notification_title);
		String content = getResources().getString(R.string.notification_waiting, ip);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
		builder.setContentText(content)
				.setContentTitle(title)
				.setOngoing(true)
				.setContentIntent(
						PendingIntent.getActivity(this, 0, new Intent(this,
								MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
				.setSmallIcon(R.drawable.ic_stat_service);
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.notify(NOTIFICATION, builder.build());
	}

	/**
	 * Called by SettingsActivity when fullscreen preference changes.
	 * The system will re-evaluate onEvaluateFullscreenMode() automatically.
	 */
	public void updateFullscreenMode() {
		// No-op: the framework re-evaluates onEvaluateFullscreenMode() as needed.
	}

	/**
	 * Load the replacements map from the database
	 */
	protected void loadReplacements() {
		HashMap<String, String> tmp = new HashMap<String, String>();
		SQLiteDatabase database = new Schema(RemoteKeyboardService.self)
				.getReadableDatabase();
		String[] columns = { Schema.COLUMN_KEY, Schema.COLUMN_VALUE };
		Cursor cursor = database.query(Schema.TABLE_REPLACEMENTS, columns, null,
				null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			tmp.put(cursor.getString(0), cursor.getString(1));
			cursor.moveToNext();
		}
		database.close();
		replacements = tmp;
	}

}
