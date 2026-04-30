package de.caco3.remotekeyboard;

import java.util.Iterator;
import java.util.List;

import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity implements
		DialogInterface.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		MaterialButton btnSelectKeyboard = findViewById(R.id.btn_select_keyboard);
		btnSelectKeyboard.setOnClickListener(v -> {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showInputMethodPicker();
		});

		MaterialButton btnCoffee = findViewById(R.id.btn_buy_coffee);
		btnCoffee.setOnClickListener(v -> {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(getString(R.string.buymeacoffee_url)));
			startActivity(browserIntent);
		});

		TextView tvVersion = findViewById(R.id.tv_version);
		tvVersion.setText(getString(R.string.version_label,
				BuildConfig.VERSION_NAME, BuildConfig.GIT_COMMIT));
	}

	@Override
	protected void onResume() {
		super.onResume();
		AppRater.appLaunched(this);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getString(WebKeyboardServer.PREF_PASSWORD, "").isEmpty()) {
			showPasswordSetupDialog();
			return;
		}

		// FIXME: This is anything but pretty! Apparently someone at Google thinks
		// that WLAN is ipv4 only.
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int addr = wifiInfo.getIpAddress();
		String ip = (addr & 0xFF) + "." + ((addr >> 8) & 0xFF) + "."
				+ ((addr >> 16) & 0xFF) + "." + ((addr >> 24) & 0xFF);

		TextView tv = (TextView) findViewById(R.id.quickinstructions);
		tv.setText(getResources().getString(R.string.app_quickinstuctions, ip));

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		List<InputMethodInfo> enabled = imm.getEnabledInputMethodList();
		Iterator<InputMethodInfo> it = enabled.iterator();

		boolean available = false;
		while (it.hasNext()) {
			available = it.next().getServiceName()
					.equals(RemoteKeyboardService.class.getCanonicalName());
			if (available) break;
		}

		if (!available) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.err_notenabled)
					.setTitle(R.string.err_notenabled_title)
					.setPositiveButton(android.R.string.yes, this)
					.setNegativeButton(android.R.string.no, this).create().show();
		}
	}

	private void showPasswordSetupDialog() {
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		input.setHint(getString(R.string.hint_password_min_length));
		int padding = (int) (16 * getResources().getDisplayMetrics().density);
		input.setPadding(padding, padding, padding, padding);

		AlertDialog dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.title_set_password)
				.setMessage(R.string.msg_password_required)
				.setView(input)
				.setCancelable(false)
				.setPositiveButton(R.string.btn_set_password, null)
				.create();

		dialog.show();
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
			String pwd = input.getText().toString().trim();
			if (pwd.length() < 4) {
				input.setError(getString(R.string.err_password_too_short));
			} else {
				PreferenceManager.getDefaultSharedPreferences(this)
						.edit().putString(WebKeyboardServer.PREF_PASSWORD, pwd).apply();
				dialog.dismiss();
				onResume();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.item_help) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse("https://github.com/caco3/RemoteKeyboard/blob/main/USER_GUIDE.md"));
			startActivity(browserIntent);
		} else if (itemId == R.id.item_replacements) {
			startActivity(new Intent(this, ReplacementsListActivity.class));
		} else if (itemId == R.id.item_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
		} else if (itemId == R.id.item_select) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showInputMethodPicker();
		}
		return false;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == DialogInterface.BUTTON_POSITIVE) {
			startActivity(new Intent(
					android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS));
		}
	}

}
