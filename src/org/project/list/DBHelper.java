 package org.project.list;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
/**
Class to handle database operation

Solves CS185c Section 3 Project LBTask

@author Nakul Natu(007224360) and Tarun Ramaswamy(007475208)
*/
public class DBHelper extends SQLiteOpenHelper 
{

	public DBHelper(Context context, String name, int version) 
	{
		super(context, name, null, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		// Create table if already not exists
		try 
		{
			db.execSQL("CREATE TABLE IF NOT EXISTS TASKS (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME TEXT, LOCATION TEXT, DESCRIPTION TEXT, DATE TEXT, TIME TEXT)");
			db.execSQL("CREATE TABLE IF NOT EXISTS CURRENT_TASKS (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME TEXT, LOCATION TEXT, RATINGURL TEXT, LATITUDE TEXT, LONGITUDE TEXT, DISTANCE TEXT, PHOTO TEXT, ADDRESS TEXT)");
		} 
		catch (SQLException e) 
		{
			Log.e("SqliteAndroid", "DBOpenHelper", e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) 
	{
		// Drop table if upgrading database
		db.execSQL("DROP TABLE IF EXISTS PEOPLE");
		this.onCreate(db);
	}

}
