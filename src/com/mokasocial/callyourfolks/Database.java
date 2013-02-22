package com.mokasocial.callyourfolks;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class Database {

	/** GLOBAL DATABASE DEFINITIONS */
	private static final String DATABASE_NAME = "callyourfolks.db";
	private static final int DATABASE_VERSION = 1;

	/** TABLE DEFINITIONS */
	private static final String TABLE_FOLKS = "folks";

	/** COLUMN DEFINITIONS */
	public static final String COLUMN_CONTACT = "contact_uri";
	public static final String COLUMN_FREQUENCY = "call_frequency";

	/** CONTEXT DEFINITION */
	private final Context mContext;
	private final DatabaseHelper mDatabaseHelper;
	private SQLiteDatabase mSQLiteDatabase;

	public Database(Context context) {
		mContext = context;
		mDatabaseHelper = new DatabaseHelper(mContext);
	}

	public class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createTableFolks(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub
		}

		private void createTableFolks(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FOLKS + " ("
					+ COLUMN_CONTACT + " TEXT PRIMARY KEY, "
					+ COLUMN_FREQUENCY + " INT);"
			);
		}
	}

	/**
	 * Open a connection to the database as defined in the Database class.
	 * 
	 * @return Database handle
	 */
	private Database open() throws SQLException {
		mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Close the open database handle.
	 */
	private void close() {
		mDatabaseHelper.close();
	}

	public static ArrayList<Folk> fetchAllFolks(Context context) throws NotFoundException {

		Database dbObj = new Database(context);
		dbObj.open();

		Cursor cursor = dbObj.mSQLiteDatabase.query(TABLE_FOLKS, new String[] { COLUMN_CONTACT, COLUMN_FREQUENCY },
				null, null, null, null, COLUMN_FREQUENCY + " asc");
		
		ArrayList<Folk> folks = new ArrayList<Folk>();
		
		if (cursor.moveToFirst()) {
			do {
				Folk thisFolk = new Folk();
				String contactUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT));
				// Log.d("Database", contactUri);
				thisFolk.contact_uri = Uri.parse(contactUri);
				thisFolk.frequency = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY));
				folks.add(thisFolk);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		
		dbObj.close();

		return folks;
	}
	
	public static Folk fetchFolkByContactUri(Context context, Uri folkContactUri) throws NotFoundException{
		Database dbObj = new Database(context);
		dbObj.open();

		Cursor result = dbObj.mSQLiteDatabase.query(TABLE_FOLKS,
				null, TABLE_FOLKS + "." + COLUMN_CONTACT + " = '" + folkContactUri.toString() + "'", null, null, null, null);

		if(result.getCount() < 1) {
			result.close();
			dbObj.close();
			throw new NotFoundException();
		}

		result.moveToFirst();
		
		Folk folk = new Folk();
		folk.contact_uri = Uri.parse(result.getString(result.getColumnIndexOrThrow(COLUMN_CONTACT)));
		folk.frequency = result.getInt(result.getColumnIndexOrThrow(COLUMN_FREQUENCY));

		result.close();
		dbObj.close();
		return folk;
	}

	public static int saveFolk(Context context, Folk folk) {
		Database dbObj = new Database(context);
		dbObj.open();

		final ContentValues values = new ContentValues();

		values.put(COLUMN_CONTACT, folk.contact_uri.toString());
		values.put(COLUMN_FREQUENCY, folk.frequency);

		int rowId = (int) dbObj.mSQLiteDatabase.replace(TABLE_FOLKS, null, values);
		dbObj.close();
		return rowId;
	}
	
	public static int deleteFolk(Context context, Folk folk) {
		Database dbObj = new Database(context);
		dbObj.open();
		int result = dbObj.mSQLiteDatabase.delete(TABLE_FOLKS, COLUMN_CONTACT + " = \"" + folk.contact_uri.toString() +"\"", null);
		dbObj.close();
		return result;
	}
}
