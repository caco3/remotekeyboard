package de.caco3.remotekeyboard;

import org.json.JSONObject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Provides a list of all the shortcut/phrase replacement patterns we got.
 * 
 * @author patrick
 * 
 */
public class ReplacementsListActivity extends AppCompatActivity implements
		DialogInterface.OnClickListener {

	private static final int CONFIRMDELETE = 1;
	private static final int CONFIRMIMPORT = 2;

	private Cursor cursor;
	private int dialogType;
	private EditText urlinput;
	private static final String[] COLUMNS = { Schema.COLUMN_KEY,
			Schema.COLUMN_VALUE, Schema.COLUMN_ID };


	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.replacements_list);
		ListView listView = findViewById(android.R.id.list);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
				cursor.moveToPosition(position);
				Intent intent = new Intent(ReplacementsListActivity.this, ReplacementActivity.class);
				intent.putExtra(ReplacementActivity.DBKEY, cursor.getString(0));
				intent.putExtra(ReplacementActivity.DBVAL, cursor.getString(1));
				intent.putExtra(ReplacementActivity.DBROW, cursor.getLong(2));
				startActivity(intent);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		SQLiteDatabase database = new Schema(this).getReadableDatabase();
		load(database);
		database.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.replacements_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.item_add_replacement) {
			startActivity(new Intent(this, ReplacementActivity.class));
			return true;
		} else if (itemId == R.id.item_clear_list) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			dialogType = CONFIRMDELETE;
			builder.setTitle(R.string.title_confirm)
					.setMessage(R.string.msg_really_delete)
					.setPositiveButton(android.R.string.yes, this)
					.setNegativeButton(android.R.string.no, this).create().show();
			return true;
		} else if (itemId == R.id.item_export) {
			doExport();
			return true;
		} else if (itemId == R.id.item_import) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			dialogType = CONFIRMIMPORT;
			urlinput = new EditText(this);
			urlinput.setHint("http://");
			builder.setTitle(R.string.title_import).setView(urlinput)
					.setPositiveButton(android.R.string.yes, this)
					.setNegativeButton(android.R.string.no, this).create().show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (dialogType) {
			case CONFIRMDELETE: {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					SQLiteDatabase database = new Schema(this).getWritableDatabase();
					database.delete(Schema.TABLE_REPLACEMENTS, null, null);
					load(database);
					if (RemoteKeyboardService.self != null) {
						RemoteKeyboardService.self.loadReplacements();
					}
					database.close();
				}
				break;
			}
			case CONFIRMIMPORT: {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					new ImportTask(this).execute(urlinput.getText().toString());
				}
				break;
			}
		}
	}

	/**
	 * Create the Listadapter and put it on display.
	 * 
	 * @param database
	 *          database handle
	 */
	protected void load(SQLiteDatabase database) {
		int[] to = { R.id.entry_key, R.id.entry_value };
		cursor = database.query(Schema.TABLE_REPLACEMENTS, COLUMNS, null, null,
				null, null, Schema.COLUMN_KEY);
		ListView listView = findViewById(android.R.id.list);
		android.view.View emptyView = findViewById(android.R.id.empty);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.entry, cursor,
				COLUMNS, to, 0);
		listView.setAdapter(adapter);
		listView.setEmptyView(emptyView);
		if (RemoteKeyboardService.self!=null) {
			RemoteKeyboardService.self.loadReplacements();
		}
	}

	/**
	 * Export is no longer supported.
	 */
	private void doExport() {
		Toast.makeText(this, R.string.err_noclient, Toast.LENGTH_SHORT).show();
	}

}
