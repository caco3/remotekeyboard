package de.caco3.remotekeyboard;

import java.util.HashMap;

import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.preference.PreferenceManager;
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

	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		self = this;
		handler = new Handler();
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
	 * @return true when an input field is currently bound to this IME, i.e.
	 *         this is the active keyboard and a text field has focus.
	 *         Used by the web client to show a "wrong keyboard selected"
	 *         banner when keystrokes would otherwise be silently dropped.
	 */
	public boolean isInputActive() {
		return getCurrentInputConnection() != null;
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
