package org.project.list;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

/**
  Class to handle events regarding location updates, adding deleting 
  
  modifying lists
  
  Solves CS185c Section 3 Project LBTask
  
  @author Nakul Natu(007224360) and Tarun Ramaswamy(007475208)
 */

public class Current_Location extends MapActivity implements LocationListener,
		OnClickListener, OnGestureListener, OnTouchListener 
{
	// Variables for Location Detection
	LocationManager locationManager;
	ListView listTemp;
	public static Context savedContext;
	double currentLongitude;
	double currentLatitude;
	public static final ArrayList<String> title = new ArrayList<String>();
	public static final ArrayList<String> link = new ArrayList<String>();
	long timeStamp = 0;
	
	// MainList variables
	public static int update = 0;
	public static String[] updateText = new String[5];
	public static int pos = 0;
	Cursor mainCursor;
	public static ArrayList<String> items = new ArrayList<String>();

	// Add_Item variables
	static final int DATE_DIALOG_ID = 0;
	static final int TIME_DIALOG_ID = 1;
	String selectedDate = "";
	String selectedTime = "";
	Boolean incomplete = true;

	// Variable to detect if application is running into background
	static int isRunningInBackground = 0;

	// Variables to detect swipe
	private static final int SWIPE_MIN_DISTANCE = 20;
	private static final int SWIPE_THRESHOLD_VELOCITY = 20;
	private GestureDetector gestureScanner;
	
	// App id storage
	String [] appid;
	int lastUsed=2;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.currentlist);
		
		//Set the view for respective orientation 
		onConfigurationChanged(getResources().getConfiguration());
		// Save the context for future use
		savedContext = this;
		// Save the app ids
		appid=new String[3];
		appid[0]="qoycGDEmendeIw9OHJg3aw";
		appid[1]="qoycGDEmendeIw9OHJg3aw";
		appid[2]="qoycGDEmendeIw9OHJg3aw";
		
		//If last known location is available show list using that
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		Location location = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) 
		{
			onLocationChanged(location);
		}
		
		// Set the listeners for buttons 
		View tempView = findViewById(R.id.BButton_MainList);
		tempView.setOnClickListener(this);
		tempView = findViewById(R.id.Button_AddTask);
		tempView.setOnClickListener(this);
		listTemp = (ListView) findViewById(R.id.List_Current);
		listTemp.setOnTouchListener(this);
		
		//Application is running
		isRunningInBackground = 0;

		// Set the scanner to handle gesture events
		gestureScanner = new GestureDetector(this);
		//Request the location updates 
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				50000, 0, this);
	}

	@Override
	protected void onResume() 
	{
		super.onResume();
		//Application is running
		isRunningInBackground = 0;
	}

	@Override
	protected void onPause() 
	{
		super.onPause();
		//Application is running in background
		isRunningInBackground = 1;
	}

	@Override
	protected void onRestart() 
	{
		super.onRestart();
		//Application is running
		isRunningInBackground = 0;
	}

	public void onProviderDisabled(String provider) 
	{
		Log.v("GPS", "Disabled");

		Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	public void onProviderEnabled(String provider) 
	{
		Log.v("GPS", "Enabled");
	}

	public void onStatusChanged(String provider, int status, Bundle extras) 
	{
		switch (status) 
		{
			case LocationProvider.OUT_OF_SERVICE:
				Log.v("GPS", "Status Changed: Out of Service");
				break;
	
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				Log.v("GPS", "Status Changed: Temporarily Unavailable");
				break;
	
			case LocationProvider.AVAILABLE:
				Log.v("GPS", "Status Changed: Available");
				break;
		}
	}

	@Override
	public void onLocationChanged(Location location) 
	{
		/** If location update is already received in 
		last second then don't listen to this update */
		if (Math.abs(System.currentTimeMillis() - timeStamp) > 40000) 
		{
			//Set the current timestamp
			timeStamp = System.currentTimeMillis();
			// Prepare the task list from the table
			DBHelper dbOpen = new DBHelper(this, "LBList", 3);
			SQLiteDatabase sqlDb = dbOpen.getReadableDatabase();
			sqlDb.execSQL("delete from CURRENT_TASKS");
			final Cursor tempCursor = sqlDb.rawQuery(
					"select * from TASKS order by ID ASC", null);

			if (tempCursor.getCount() > 0) 
			{
				/** Prepare array lists for delete items, notify the user
				   populate current list */
				ArrayList<String> toDelete = new ArrayList<String>();
				ArrayList<String> toNotify = new ArrayList<String>();
				ArrayList<String> items = new ArrayList<String>();
				ArrayList<String> name = new ArrayList<String>();

				for (int i = 0; i < tempCursor.getCount(); i++) 
				{
					tempCursor.moveToNext();

					String tempDate = tempCursor.getString(4);
					String tempTime = tempCursor.getString(5);
					// Check the date of each view with current date
					SimpleDateFormat dateFormater = new SimpleDateFormat(
							"MM/dd/yyyyHH:mm");
					Date currentDate = new Date();
					Date savedDate = null;
					try 
					{
						savedDate = dateFormater.parse(tempDate + tempTime);
					} 
					catch (ParseException e) 
					{
						e.printStackTrace();
					}
					// Check if the task is already expired or not
					if (currentDate.before(savedDate)) 
					{
						// Add to the current list checking
						String tempItem = tempCursor.getString(2);
						tempItem = tempItem.replaceAll(" ", "+");
						tempItem = tempItem.replaceAll(" ", "%");
						tempItem = tempItem.replaceAll(" ", "#");
						items.add(tempItem);
						name.add(tempCursor.getString(1));
						long timeDifference = savedDate.getTime()
								- currentDate.getTime();
						if (timeDifference <= 2 * 60 * 60 * 1000) 
						{
							/** If task is going to expire in 
							   2 hours notify the user */
							toNotify.add(tempCursor.getString(1));
						}
					} 
					else 
					{
						toDelete.add(tempCursor.getString(1));
					}
				}

				sqlDb.close();
				try 
				{
					title.clear();
					for (int j = 0; j < items.size(); j++) 
					{
						String ywsid="";
						switch(lastUsed)
						{
							case 0:
								ywsid=appid[1];
								lastUsed=1;
								break;
							case 1:
								ywsid=appid[2];
								lastUsed=2;
								break;
							case 2:
								ywsid=appid[0];
								lastUsed=0;
						}
						// Prepared the url to send to the yelp api
						String savedUrl = "http://api.yelp.com/business_review_search?term=="
								+ items.get(j).toString()
								+ "&lat="
								+ Double.toString(location.getLatitude())
								+ "&long="
								+ Double.toString(location.getLongitude())
								+ "&radius=2.0&limit=3&ywsid="+ywsid;
						
						currentLongitude = location.getLongitude();
						currentLatitude = location.getLatitude();

						// Start the connection using the url
						URL url = new URL(savedUrl);
						HttpURLConnection urlConn = (HttpURLConnection) url
								.openConnection();
						Log.i("newslist", "urlconnection succeeded");
						// Clear the array lists

						link.clear();

						// Get the input stream for the connected url
						BufferedReader in = new BufferedReader(
								new InputStreamReader(urlConn.getInputStream()));

						String inputLine;
						String jsontext = "";
						while ((inputLine = in.readLine()) != null) 
						{
							jsontext = jsontext + inputLine;
						}
						int ind = jsontext.indexOf("business");
						inputLine = jsontext.substring(ind + 12);

						// Convert the json reply from url into json entries
						JSONArray entries = new JSONArray(inputLine);

						urlConn.disconnect();
						// Parse json 
						String[] current_location = new String[8];
						int i=0;
						if (entries.length() != 0) 
						{
							title.add(name.get(j).toString());
							for (i = 0; i < entries.length(); i++) 
							{
								// Save the data for inserting
								current_location[0] = name.get(j).toString();
								JSONObject post = entries.getJSONObject(i);

								current_location[1] = post.getString("name");
								current_location[1] = current_location[1]
										.replaceAll("'", "''");
								current_location[2] = post
										.getString("rating_img_url_small");
								current_location[2] = current_location[2]
										.replaceAll("'", "''");
								current_location[3] = post
										.getString("latitude");
								current_location[4] = post
										.getString("longitude");
								current_location[5] = post
										.getString("distance");
								current_location[6] = post
										.getString("photo_url");
								current_location[6] = current_location[6]
										.replaceAll("'", "''");

								// Prepare the address
								if (!(current_location[7] = post
										.getString("address1"))
										.contentEquals("")) 
								{

									if (!post.getString("address2")
											.contentEquals("")) 
									{
										current_location[7] = current_location[7]
												+ ","
												+ post.getString("address2");

										if (!post.getString("address3")
												.contentEquals("")) 
										{
											current_location[7] = current_location[7]
													+ ","
													+ post.getString("address3");
										}
									}
									current_location[7] = current_location[7]
											+ "," + post.getString("city")
											+ ","
											+ post.getString("state_code")
											+ ","
											+ post.getString("country_code")
											+ " " + post.getString("zip");
								}

								current_location[7] = current_location[7]
										.replaceAll("'", "''");
								// Insert the data into temporary table
								SQLiteDatabase sqlDbinsert = dbOpen
										.getWritableDatabase();
								sqlDbinsert
										.execSQL("insert or replace into CURRENT_TASKS (NAME,LOCATION,RATINGURL,LATITUDE,LONGITUDE,DISTANCE,PHOTO,ADDRESS)values ('"
												+ current_location[0]
												+ "','"
												+ current_location[1]
												+ "','"
												+ current_location[2]
												+ "','"
												+ current_location[3]
												+ "','"
												+ current_location[4]
												+ "','"
												+ current_location[5]
												+ "','"
												+ current_location[6]
												+ "','"
												+ current_location[7] + "')");
								sqlDbinsert.close();
							}
						}

					}
					
					/** If there are task which can be done at this location 
					   show them to the user */
					listTemp = (ListView) findViewById(R.id.List_Current);
					listTemp.setAdapter(new ArrayAdapter<String>(this,
							android.R.layout.simple_list_item_1, title));
					listTemp.setOnItemClickListener(new OnItemClickListener() 
					{
						public void onItemClick(AdapterView<?> parent,
								View view, int position, long id) 
						{
							/* If user clicks on the list item then show the 
							results obtained from yelp */
							
							DBHelper dbOpen = new DBHelper(savedContext,
									"LBList", 3);
							final SQLiteDatabase sqlDb = dbOpen
									.getReadableDatabase();
							final Cursor cursorTemp = sqlDb.rawQuery(
									"select * from CURRENT_TASKS where NAME='"
											+ title.get(position)
											+ "' order by ID ASC", null);

							final String[] location = new String[cursorTemp
									.getCount()];
							

							for (int i = 0; i < cursorTemp.getCount(); i++) 
							{
								cursorTemp.moveToNext();
								location[i] = cursorTemp.getString(2)
										+ " ,Distance:"
										+ cursorTemp.getString(6);
							}

							sqlDb.close();

							final AlertDialog alert;

							AlertDialog.Builder builder = new AlertDialog.Builder(
									savedContext);

							builder.setItems(location,
									new DialogInterface.OnClickListener() 
							{
										public void onClick(
												DialogInterface dialog,
												int item) 
										{
											/** if user click on the location 
											then show it onto the map */
											cursorTemp.close();
											onCreateMap(location[item]);
											ViewFlipper vf = (ViewFlipper) findViewById(R.id.ViewFlipper01);
											vf.setAnimation(AnimationUtils
													.loadAnimation(
															savedContext,
															R.anim.push_left_in));
											vf.setDisplayedChild(0);
											vf.showNext();
										}
							});
							builder.setCancelable(true);
							alert = builder.create();
							alert.setCanceledOnTouchOutside(true);
							alert.setButton("Ok",
									new DialogInterface.OnClickListener() 
							{
										public void onClick(
												DialogInterface dialog,
												int item) {
											alert.dismiss();
										}
							});

							alert.show();

						}
					});
					if (isRunningInBackground == 1) 
					{
						/* If application is running in background 
						we have to throw notification about current tasks */
						String ns = Context.NOTIFICATION_SERVICE;
						NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
						int icon = R.drawable.appicon;
						CharSequence tickerText = "Tasks";
						long when = System.currentTimeMillis();

						Notification notification = new Notification(icon,
								tickerText, when);
						CharSequence contentText = "";
						if(!title.isEmpty())
						{
							contentText = "Click to see the tasks to be done. ";
						}
						
						if (!toNotify.isEmpty()) 
						{
							contentText = contentText
									+ "There are some tasks which will be expired in an hour , such as";
							for (int i = 0; i < toNotify.size(); i++) 
							{
								contentText = contentText + " "
										+ toNotify.get(i);
							}
						}
						// Prepare custom view for notification
						RemoteViews contentView = new RemoteViews(
								getPackageName(), R.layout.notification);
						contentView.setImageViewResource(R.id.image,
								R.drawable.appicon);
						contentView.setTextViewText(R.id.text, contentText);
						notification.contentView = contentView;

						Intent notificationIntent = new Intent(this,
								Current_Location.class);
						
						//Set flags for notification intent as well as notification
						notificationIntent
								.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
										| Intent.FLAG_ACTIVITY_SINGLE_TOP);
						PendingIntent contentIntent = PendingIntent
								.getActivity(this, 0, notificationIntent, 0);
						notification.flags = Notification.FLAG_AUTO_CANCEL
								| Notification.FLAG_ONGOING_EVENT;
						notification.defaults |= Notification.DEFAULT_VIBRATE;
						notification.defaults |= Notification.DEFAULT_SOUND;
						notification.contentIntent = contentIntent;
						//Notify the user
						if(!title.isEmpty() || !toNotify.isEmpty())
						{
							notificationManager.notify(1, notification);
						}
					} 
					else 
					{
						if (!toNotify.isEmpty()) 
						{
							// Show an alert dialog about the expiring tasks
							String message = "There are some tasks which will be expired in an hour , such as ";
							for (int i = 0; i < toNotify.size(); i++) 
							{
								message = message + toNotify.get(i);
							}

							AlertDialog.Builder dialog = new AlertDialog.Builder(
									Current_Location.savedContext);
							dialog.setTitle("Tasks about to expired");
							dialog.setMessage(message);
							dialog.setCancelable(true);
							final AlertDialog alert;
							alert = dialog.create();

							alert.setButton("OK",
									new DialogInterface.OnClickListener() 
							{
										public void onClick(
												DialogInterface dialog,
												int item) 
										{
											alert.dismiss();
										}
							});
							alert.dismiss();
							alert.show();
						}
					}
					

					tempCursor.close();
					// Delete the expired tasks
					dbOpen = new DBHelper(this, "LBList", 3);
					sqlDb = dbOpen.getWritableDatabase();

					for (int k = 0; k < toDelete.size(); k++) 
					{
						sqlDb.execSQL("delete from TASKS where NAME='"
								+ toDelete.get(k).toString() + "'");
					}
					sqlDb.close();
					dbOpen.close();
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onClick(View tempView) 
	{
		// Handle the click events for various buttons in an application
		ViewFlipper vf = (ViewFlipper) findViewById(R.id.ViewFlipper01);

		switch (tempView.getId()) 
		{
			case R.id.BButton_MainList:
				onCreateMainlist();
				vf.setAnimation(AnimationUtils.loadAnimation(tempView.getContext(),
						R.anim.push_left_in));
				vf.setDisplayedChild(1);
				vf.showNext();
				break;
	
			case R.id.Button_MapMainTask:
				onCreateMainlist();
				vf.setAnimation(AnimationUtils.loadAnimation(tempView.getContext(),
						R.anim.push_left_in));
				vf.setDisplayedChild(1);
				vf.showNext();
				break;
	
			case R.id.Button_AddTask:
				onCreateAdditem();
				vf.setAnimation(AnimationUtils.loadAnimation(tempView.getContext(),
						R.anim.push_up_in));
				vf.setDisplayedChild(2);
				vf.showNext();
				break;
	
			case R.id.Button_Add:
				onCreateAdditem();
				vf.setAnimation(AnimationUtils.loadAnimation(tempView.getContext(),
						R.anim.push_up_in));
				vf.setDisplayedChild(2);
				vf.showNext();
				break;
	
			case R.id.Button_Home:
				mainCursor.close();
				vf.setAnimation(AnimationUtils.loadAnimation(tempView.getContext(),
						R.anim.push_up_in));
				vf.setDisplayedChild(3);
				vf.showNext();
				break;
	
			case R.id.Button_CurrentList:
				vf.setAnimation(AnimationUtils.loadAnimation(tempView.getContext(),
						R.anim.push_up_in));
				vf.setDisplayedChild(3);
				vf.showNext();
				break;
	
			case R.id.Button_Save:
				save();
				if (incomplete) {
					onCreateMainlist();
					removeDialog(DATE_DIALOG_ID);
					removeDialog(TIME_DIALOG_ID);
					vf.setAnimation(AnimationUtils.loadAnimation(
							tempView.getContext(), R.anim.push_left_in));
					vf.setDisplayedChild(1);
					vf.showNext();
				}
				break;
	
			case R.id.Button_Clear:
				clear();
				removeDialog(DATE_DIALOG_ID);
				removeDialog(TIME_DIALOG_ID);
				break;
	
			case R.id.Button_Discard:
				onCreateMainlist();
				removeDialog(DATE_DIALOG_ID);
				removeDialog(TIME_DIALOG_ID);
				vf.setAnimation(AnimationUtils.loadAnimation(tempView.getContext(),
						R.anim.push_left_in));
				vf.setDisplayedChild(1);
				vf.showNext();
	
			case R.id.Button_Background1:
				CloseKeyboard(tempView);
		}
	}

	@Override
	protected boolean isRouteDisplayed() 
	{
		// Do nothing
		return false;
	}

	public void onCreateMainlist() 
	{
		// Create main list from table TASKS
		update = 0;
		mainlistUpdate();
		
		ListView listMain = (ListView) findViewById(R.id.List_Item);
		listMain.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) 
			{
				final AlertDialog alert;

				AlertDialog.Builder builder = new AlertDialog.Builder(
						Current_Location.savedContext);

				mainCursor.moveToPosition(position);
				pos = mainCursor.getInt(0);
				//Save the text if user wants to update
				updateText[0] = mainCursor.getString(1);
				updateText[1] = mainCursor.getString(2);
				updateText[2] = mainCursor.getString(3);
				updateText[3] = mainCursor.getString(4);
				updateText[4] = mainCursor.getString(5);

				String[] itemlist = new String[5];
				itemlist[0] = "Name:" + updateText[0];
				itemlist[1] = "Location:" + updateText[1];
				itemlist[2] = "Description:" + updateText[2];
				itemlist[3] = "Date:" + updateText[3];
				itemlist[4] = "Time:" + updateText[4];

				builder.setItems(itemlist,
						new DialogInterface.OnClickListener() 
				{
							public void onClick(DialogInterface dialog, int item) 
							{
								// Do nothing
							}
				});

				builder.setCancelable(true);
				alert = builder.create();
				alert.setCanceledOnTouchOutside(true);
				alert.setButton("Update",
						new DialogInterface.OnClickListener() 
				{
							public void onClick(DialogInterface dialog, int item) 
							{
								update = 1;
								// Prepare for update this task
								onCreateAdditem();
								ViewFlipper vf = (ViewFlipper) findViewById(R.id.ViewFlipper01);

								vf.setAnimation(AnimationUtils.loadAnimation(
										savedContext, R.anim.push_left_in));
								vf.setDisplayedChild(2);
								vf.showNext();
							}
				});

				alert.setButton2("Cancel",
						new DialogInterface.OnClickListener() 
				{
							public void onClick(DialogInterface dialog, int item) 
							{
								alert.dismiss();
							}
				});

				alert.setButton3("Delete",
						new DialogInterface.OnClickListener() 
				{
							public void onClick(DialogInterface dialog, int item) 
							{	// Delete the corresponding task from database
								DBHelper dbOpen = new DBHelper(
										Current_Location.savedContext,
										"LBList", 3);
								SQLiteDatabase sqlDb = dbOpen
										.getWritableDatabase();
								sqlDb.execSQL("DELETE FROM TASKS WHERE ID="
										+ Integer.toString(pos));
								sqlDb.close();
								if (title.contains(updateText[0])) 
								{
									title.remove(updateText[0]);
									listTemp = (ListView) findViewById(R.id.List_Current);
									listTemp.setAdapter(new ArrayAdapter<String>(
											savedContext,
											android.R.layout.simple_list_item_1,
											title));
								}
								// Update the mainlist
								mainlistUpdate();

							}
				});
				alert.show();
			}
		});

		// Add click and touch listeners to the buttons and list on this view
		View tempView = findViewById(R.id.Button_Add);
		tempView.setOnClickListener(this);
		tempView = findViewById(R.id.Button_Home);
		tempView.setOnClickListener(this);

		listMain = (ListView) findViewById(R.id.List_Item);
		listMain.setOnTouchListener(this);
	}

	public void mainlistUpdate() 
	{
		// Populate tasks into main list
		ListView tempList = (ListView) findViewById(R.id.List_Item);

		DBHelper dbOpen = new DBHelper(Current_Location.savedContext, "LBList",
				3);
		SQLiteDatabase sqlDb = dbOpen.getReadableDatabase();
		mainCursor = sqlDb
				.rawQuery("select * from TASKS order by ID ASC", null);

		items.clear();
		for (int i = 0; i < mainCursor.getCount(); i++) 
		{
			mainCursor.moveToNext();
			items.add(mainCursor.getString(1));
		}
		sqlDb.close();

		tempList.setAdapter(new ArrayAdapter<String>(
				Current_Location.savedContext,
				android.R.layout.simple_list_item_1, items));
		tempList.setTextFilterEnabled(true);
	}

	public void onCreateAdditem() 
	{
		// Prepare for add new task or update task respectively
		View tempView = this.findViewById(R.id.Button_Save);
		tempView.setOnClickListener(this);
		tempView = this.findViewById(R.id.Button_Clear);
		tempView.setOnClickListener(this);
		tempView = this.findViewById(R.id.Button_Discard);
		tempView.setOnClickListener(this);
		tempView = this.findViewById(R.id.Button_Background1);
		tempView.requestFocus();
		tempView.setOnClickListener(this);
		tempView = this.findViewById(R.id.Scroll_Add);
		tempView.setOnTouchListener(this);
		TextView tempTextview = (TextView) this.findViewById(R.id.Text_Date);
		tempTextview.setOnTouchListener(this);
		tempTextview = (TextView) this.findViewById(R.id.Text_Time);
		tempTextview.setOnTouchListener(this);
		tempTextview = (TextView) this.findViewById(R.id.Text_Description);
		tempTextview
				.setOnEditorActionListener(new TextView.OnEditorActionListener() 
		{
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) 
					{
						// If return or done close the keyboard
						if (actionId == 0 || actionId == 6)
							CloseKeyboard((View) v);
						return true;
					}
		});
		
		tempTextview = (TextView) this.findViewById(R.id.Text_Location);
		tempTextview
				.setOnEditorActionListener(new TextView.OnEditorActionListener() 
		{
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) 
					{
						// If return or done close the keyboard
						if (actionId == 0 || actionId == 6)
							CloseKeyboard((View) v);
						return true;
					}
		});
		
		
		tempTextview = (TextView) this.findViewById(R.id.Text_Name);
		tempTextview
				.setOnEditorActionListener(new TextView.OnEditorActionListener() 
		{
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) 
					{
						// If return or done close the keyboard
						if (actionId == 0 || actionId == 6)
							CloseKeyboard((View) v);
						return true;
					}
		});
		// If task is being updated then set texts with previous data 
		if (Current_Location.update == 1) 
		{
			tempTextview.setText(Current_Location.updateText[0]);
			tempTextview = (TextView) this.findViewById(R.id.Text_Location);
			tempTextview.setText(Current_Location.updateText[1]);
			tempTextview = (TextView) this.findViewById(R.id.Text_Description);
			tempTextview.setText(Current_Location.updateText[2]);
			tempTextview = (TextView) this.findViewById(R.id.Text_Date);
			tempTextview.setText(Current_Location.updateText[3]);
			tempTextview = (TextView) this.findViewById(R.id.Text_Time);
			tempTextview.setText(Current_Location.updateText[4]);
		} 
		else 
		{
			// If new task then clear all the boxes
			clear();
		}
	}

	public void save() 
	{
		String[] insertText = new String[5];
		String message="";
		incomplete = true;
		TextView tempTextview = (TextView) this.findViewById(R.id.Text_Name);
		insertText[0] = tempTextview.getText().toString();
		
		/** Check if the task is not already present in 
		the table or it is not the update operation */
		if (!items.contains(insertText[0]) || update==1) 
		{
			tempTextview = (TextView) this.findViewById(R.id.Text_Location);
			insertText[1] = tempTextview.getText().toString();
			tempTextview = (TextView) this.findViewById(R.id.Text_Description);
			insertText[2] = tempTextview.getText().toString();
			tempTextview = (TextView) this.findViewById(R.id.Text_Date);
			insertText[3] = tempTextview.getText().toString();
			tempTextview = (TextView) this.findViewById(R.id.Text_Time);
			insertText[4] = tempTextview.getText().toString();
			CloseKeyboard(tempTextview);
			
			// Check the date of each view with current date
			SimpleDateFormat dateFormater = new SimpleDateFormat(
					"MM/dd/yyyyHH:mm");
			Date currentDate = new Date();
			Date savedDate = null;
			try 
			{
				savedDate = dateFormater.parse(insertText[3] + insertText[4]);
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
			// Check if the task is already expired or not
			if (currentDate.after(savedDate)){
				message="Enter expiry time higher than current time";
				incomplete = false;
			}
			// Check if any of textboxes are empty
			for (int i = 0; i < 5; i++) 
			{
				insertText[i] = insertText[i].trim();
				if (insertText[i].contentEquals("")) 
				{
					incomplete = false;
					message="All Fields Are Mandetory";
				}
			}
			
			if (incomplete) 
			{
				// If all the boxes are filled then prepare query 
				DBHelper dbOpen = new DBHelper(this, "LBList", 3);
				SQLiteDatabase sqlDb = dbOpen.getWritableDatabase();

				if (Current_Location.update == 0) 
				{
					sqlDb.execSQL("INSERT OR REPLACE INTO TASKS (NAME,LOCATION,DESCRIPTION,DATE,TIME)VALUES ('"
							+ insertText[0]
							+ "','"
							+ insertText[1]
							+ "','"
							+ insertText[2]
							+ "','"
							+ insertText[3]
							+ "','"
							+ insertText[4] + "')");
				} 
				else 
				{
					sqlDb.execSQL("INSERT OR REPLACE INTO TASKS VALUES ('"
							+ Integer.toString(Current_Location.pos) + "','"
							+ insertText[0] + "','" + insertText[1] + "','"
							+ insertText[2] + "','" + insertText[3] + "','"
							+ insertText[4] + "')");
				}

				sqlDb.close();
				update = 0;
			} 
			else 
			{
				//If any of the field is empty alert the user
				final AlertDialog alert;
				AlertDialog.Builder builder = new AlertDialog.Builder(
						Current_Location.savedContext);
				builder.setCancelable(true);
				builder.setMessage(message);
				alert = builder.create();
				alert.setCanceledOnTouchOutside(true);
				alert.setButton("OK", new DialogInterface.OnClickListener() 
				{
					public void onClick(DialogInterface dialog, int item) 
					{
						alert.dismiss();
					}
				});
				alert.show();
			}
		} 
		else 
		{
			// If task is already present then inform the user
			incomplete = false;
			final AlertDialog alert = new AlertDialog.Builder(savedContext)
					.create();
			alert.setMessage("Task with this name is already present");
			alert.setCancelable(true);
			alert.setCanceledOnTouchOutside(true);

			alert.setButton("Ok", new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog, int item) 
				{
					alert.dismiss();
				}
			});
			alert.show();
		}
	}

	public void clear() 
	{
		// Set all the text views to blank
		TextView tempName = (TextView) this.findViewById(R.id.Text_Name);
		TextView tempLocation = (TextView) this
				.findViewById(R.id.Text_Location);
		TextView tempDescription = (TextView) this
				.findViewById(R.id.Text_Description);
		TextView tempDate = (TextView) this.findViewById(R.id.Text_Date);
		TextView tempTime = (TextView) this.findViewById(R.id.Text_Time);
		tempName.setText("");
		tempLocation.setText("");
		tempDescription.setText("");
		tempDate.setText("");
		tempTime.setText("");
	}

	@Override
	protected Dialog onCreateDialog(int id) 
	{	
		// Prepare dialog for either for picking date or time
		
		Calendar c = Calendar.getInstance();
		int cyear = c.get(Calendar.YEAR);
		int cmonth = c.get(Calendar.MONTH);
		int cday = c.get(Calendar.DAY_OF_MONTH);
		
		
		switch (id) 
		{
			case DATE_DIALOG_ID:
				return new DatePickerDialog(this, mDateSetListener, cyear, cmonth,
						cday);
	
			case TIME_DIALOG_ID:
				return new TimePickerDialog(this, mTimeSetListener,
						c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
		}
		return null;
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() 
	{	
		/** Dialog for setting the picked date to the textbox
		and his onDateSet method */
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) 
		{
			selectedDate = String.valueOf(monthOfYear + 1) + "/"
					+ String.valueOf(dayOfMonth) + "/" + String.valueOf(year);
			TextView temp_date = (TextView) findViewById(R.id.Text_Date);
			temp_date.setText(selectedDate);
			
		}
	};

	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() 
	{
		/** Dialog for setting the picked time to the textbox
		and his onTimeSet method */
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) 
		{
			selectedTime = "";
			if (hourOfDay < 10) 
			{
				selectedTime = "0";
			}
			selectedTime = selectedTime + hourOfDay + ":";
			if (minute < 10) 
			{
				selectedTime = selectedTime + "0";
			}
			selectedTime = selectedTime + minute;
			TextView temp_time = (TextView) findViewById(R.id.Text_Time);
			temp_time.setText(selectedTime);
		}
	};

	@Override
	public boolean onTouchEvent(MotionEvent me) 
	{
		// Return our gesture scanner
		return gestureScanner.onTouchEvent(me);
	}

	void CloseKeyboard(View tempView) 
	{	// Close the soft keyboard
		InputMethodManager imm = (InputMethodManager) this
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(tempView.getWindowToken(), 0);
	}

	public void onCreateMap(String name) 
	{	// Prepare map for the user
		MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		// Set the onClick Listeners for the toolbar buttons
		View tempView = this.findViewById(R.id.Button_CurrentList);
		tempView.setOnClickListener(this);
		tempView = this.findViewById(R.id.Button_MapMainTask);
		tempView.setOnClickListener(this);
		// Set the overlays to display to null
		List<Overlay> mapOverlays = mapView.getOverlays();
		mapOverlays.clear();
		// Set the default marker
		Drawable drawable = this.getResources().getDrawable(R.drawable.marker);
		Current_Map_Overlay itemizedoverlay = new Current_Map_Overlay(drawable);
		name = name.substring(0, name.indexOf(',') - 1);
		// Get the information of the destination
		DBHelper dbOpen = new DBHelper(savedContext, "LBList", 3);
		SQLiteDatabase sqlDb = dbOpen.getReadableDatabase();
		final Cursor tempCursor = sqlDb.rawQuery(
				"select * from CURRENT_TASKS where LOCATION='" + name + "'",
				null);
		tempCursor.moveToFirst();
		String latitude = tempCursor.getString(4);
		String longitude = tempCursor.getString(5);

		sqlDb.close();
		// Prepare geopoint for destination
		GeoPoint pointDestination = new GeoPoint(
				(int) (Double.valueOf(latitude) * 1000000),
				(int) (Double.valueOf(longitude) * 1000000));
		OverlayItem overlayitemDest = new OverlayItem(pointDestination,
				tempCursor.getString(1), tempCursor.getString(2));
		// Prepare geo point for the source location
		Location location = locationManager
		.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) 
		{
			currentLatitude=location.getLatitude();
			currentLongitude=location.getLongitude();
		}
		GeoPoint pointCurrent = new GeoPoint((int) (currentLatitude * 1000000),
				(int) (currentLongitude * 1000000));
		OverlayItem overlayitemCurrent = new OverlayItem(pointCurrent,
				"Current Location", "I'm Here!");

		itemizedoverlay.addOverlay(overlayitemCurrent);
		// Get the marker for destination
		Drawable drawDest = this.getResources().getDrawable(R.drawable.markerb);
		drawDest.setBounds(0, 0, drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight());
		overlayitemDest.setMarker(drawDest);

		itemizedoverlay.addOverlay(overlayitemDest);
		mapOverlays.add(itemizedoverlay);
		tempCursor.close();
		MapController mc = mapView.getController();
		// Set the focus of map to the current location
		mc.animateTo(pointCurrent);
		mc.setZoom(12);
		mapView.invalidate();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{	// Change the width of the views corresponding to orientation
		super.onConfigurationChanged(newConfig);
		View tempView;
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) 
		{
			tempView = findViewById(R.id.Button_Save);
			LayoutParams param = tempView.getLayoutParams();
			param.width = 160;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_Clear);
			param = tempView.getLayoutParams();
			param.width = 160;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_Discard);
			param = tempView.getLayoutParams();
			param.width = 160;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.BButton_MainList);
			param = tempView.getLayoutParams();
			param.width = 240;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_AddTask);
			param = tempView.getLayoutParams();
			param.width = 240;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_CurrentList);
			param = tempView.getLayoutParams();
			param.width = 240;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_MapMainTask);
			param = tempView.getLayoutParams();
			param.width = 240;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_Add);
			param = tempView.getLayoutParams();
			param.width = 240;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_Home);
			param = tempView.getLayoutParams();
			param.width = 240;
			tempView.setLayoutParams(param);
			
			tempView = findViewById(R.id.Text_Description);
			param = tempView.getLayoutParams();
			param.width = 310;
			tempView.setLayoutParams(param);
			
			tempView = findViewById(R.id.Text_Date);
			param = tempView.getLayoutParams();
			param.width = 310;
			tempView.setLayoutParams(param);
			
			tempView = findViewById(R.id.Text_Location);
			param = tempView.getLayoutParams();
			param.width = 310;
			tempView.setLayoutParams(param);
			
			tempView = findViewById(R.id.Text_Name);
			param = tempView.getLayoutParams();
			param.width = 310;
			tempView.setLayoutParams(param);
			
			tempView = findViewById(R.id.Text_Time);
			param = tempView.getLayoutParams();
			param.width = 310;
			tempView.setLayoutParams(param);
			
		} 
		else 
		{
			tempView = findViewById(R.id.Button_Save);
			LayoutParams param = tempView.getLayoutParams();
			param.width = 107;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_Clear);
			param = tempView.getLayoutParams();
			param.width = 106;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_Discard);
			param = tempView.getLayoutParams();
			param.width = 107;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.BButton_MainList);
			param = tempView.getLayoutParams();
			param.width = 160;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_AddTask);
			param = tempView.getLayoutParams();
			param.width = 160;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_CurrentList);
			param = tempView.getLayoutParams();
			param.width = 160;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_MapMainTask);
			param = tempView.getLayoutParams();
			param.width = 160;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_Add);
			param = tempView.getLayoutParams();
			param.width = 160;
			tempView.setLayoutParams(param);

			tempView = findViewById(R.id.Button_Home);
			param = tempView.getLayoutParams();
			param.width = 160;
			tempView.setLayoutParams(param);
			
			tempView = findViewById(R.id.Text_Description);
			param = tempView.getLayoutParams();
			param.width = 190;
			tempView.setLayoutParams(param);
			
			tempView = findViewById(R.id.Text_Date);
			param = tempView.getLayoutParams();
			param.width = 190;
			tempView.setLayoutParams(param);
			
			tempView = findViewById(R.id.Text_Location);
			param = tempView.getLayoutParams();
			param.width = 190;
			tempView.setLayoutParams(param);
			
			tempView = findViewById(R.id.Text_Name);
			param = tempView.getLayoutParams();
			param.width = 190;
			tempView.setLayoutParams(param);
			
			tempView = findViewById(R.id.Text_Time);
			param = tempView.getLayoutParams();
			param.width = 190;
			tempView.setLayoutParams(param);
		}
	}

	@Override
	public boolean onDown(MotionEvent e) 
	{
		// Do nothing
		return false;
	}

	@Override
	public boolean onFling(MotionEvent firsttouch, MotionEvent lasttouch,
			float velocityX, float velocityY) 
	{	// We will check for the swipe in this method
		try 
		{
			// right to left swipe
			if (firsttouch.getX() - lasttouch.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) 
			{	/** Get the view shown to the user and 
				show the corresponding next view to the user */
				ViewFlipper vf = (ViewFlipper) findViewById(R.id.ViewFlipper01);
				switch (vf.getDisplayedChild()) 
				{
					case 0:
						onCreateMainlist();
						vf.setAnimation(AnimationUtils.loadAnimation(this,
								R.anim.slide_left));
						vf.setDisplayedChild(1);
						vf.showNext();
						break;
					case 2:
						onCreateAdditem();
						vf.setAnimation(AnimationUtils.loadAnimation(this,
								R.anim.slide_left));
						vf.setDisplayedChild(2);
						vf.showNext();
						break;
					case 3:
						onCreateMainlist();
						CloseKeyboard(vf);
						vf.setAnimation(AnimationUtils.loadAnimation(this,
								R.anim.slide_left));
						vf.setDisplayedChild(1);
						vf.showNext();
						break;
				}
			}
			// Left to right swipe
			else if (lasttouch.getX() - firsttouch.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) 
			{
				ViewFlipper vf = (ViewFlipper) findViewById(R.id.ViewFlipper01);
				switch (vf.getDisplayedChild()) 
				{
					case 0:
						onCreateAdditem();
						vf.setAnimation(AnimationUtils.loadAnimation(this,
								R.anim.slide_right));
						vf.setDisplayedChild(2);
						vf.showNext();
						break;
					case 2:
						mainCursor.close();
						vf.setAnimation(AnimationUtils.loadAnimation(this,
								R.anim.slide_right));
						vf.setDisplayedChild(3);
						vf.showNext();
						break;
					case 3:
						CloseKeyboard(vf);
						onCreateMainlist();
						vf.setAnimation(AnimationUtils.loadAnimation(this,
								R.anim.slide_left));
						vf.setDisplayedChild(1);
						vf.showNext();
						break;
					}
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) 
	{	// Do nothing
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) 
	{	// Do nothing
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) 
	{	// Do nothing
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) 
	{	// Do nothing
		return false;
	}

	@Override
	public boolean onTouch(View tempView, MotionEvent event) 
	{
		//Show the corresponding dialog or send to gesture controller
		switch (tempView.getId()) 
		{
			case R.id.Text_Date:
				showDialog(DATE_DIALOG_ID);
				return false;
	
			case R.id.Text_Time:
				showDialog(TIME_DIALOG_ID);
				return false;
	
			default:
				if (tempView.getId() == R.id.Scroll_Add)
					CloseKeyboard(tempView);
				return gestureScanner.onTouchEvent(event);
		}
	}
}