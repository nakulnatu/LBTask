package org.project.list;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

/**
Class to handle google map

Solves CS185c Section 3 Project LBTask

@author Nakul Natu(007224360) and Tarun Ramaswamy(007475208)
*/

@SuppressWarnings("rawtypes")
public class Current_Map_Overlay extends ItemizedOverlay 
{
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

	public Current_Map_Overlay(Drawable defaultMarker, Context context) 
	{
		super(defaultMarker);
	}

	public Current_Map_Overlay(Drawable defaultMarker) 
	{
		super(boundCenterBottom(defaultMarker));
	}

	public void addOverlay(OverlayItem overlay) 
	{
		mOverlays.add(overlay);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) 
	{
		return mOverlays.get(i);
	}

	@Override
	public int size() 
	{
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(int index) 
	{
		// Get the marker which was tapped
		OverlayItem item = mOverlays.get(index);

		if (item.getTitle() == "Current Location") 
		{
			// If its current location of user the display this dialog box
			AlertDialog.Builder dialog = new AlertDialog.Builder
			(
					Current_Location.savedContext);
			dialog.setTitle(item.getTitle());
			dialog.setMessage(item.getSnippet());
			dialog.setCancelable(true);
			
			final AlertDialog alert;
			alert = dialog.create();
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
		else 
		{
			
			//If its destination then prepare the contents to display
			final Dialog dialog = new Dialog(Current_Location.savedContext);
			dialog.setContentView(R.layout.dialog);
			dialog.setTitle(item.getSnippet());
			DBHelper dbOpen = new DBHelper(Current_Location.savedContext,
					"LBList", 3);
			SQLiteDatabase sqlDb = dbOpen.getReadableDatabase();
			final Cursor cursorTemp = sqlDb.rawQuery(
					"select * from CURRENT_TASKS where LOCATION='"
							+ item.getSnippet() + "'", null);
			cursorTemp.moveToNext();
			// Set the images for Business image and rating image
			ImageView image = (ImageView) dialog.findViewById(R.id.Image_Photo);
			Bitmap bitmap = null;
			try 
			{
				bitmap = BitmapFactory.decodeStream((InputStream) new URL(
						cursorTemp.getString(7)).getContent());
				image.setImageBitmap(bitmap);
				image = (ImageView) dialog.findViewById(R.id.Image_Rating);
				bitmap = BitmapFactory.decodeStream((InputStream) new URL(
						cursorTemp.getString(3)).getContent());
				image.setImageBitmap(bitmap);
			} 
			catch (MalformedURLException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			// Set the address and distance of destination
			TextView text = (TextView) dialog.findViewById(R.id.Text_Distance);
			text.setText("Distance:" + cursorTemp.getString(6) + "miles");
			text = (TextView) dialog.findViewById(R.id.Text_Address);
			text.setText("Address:" + cursorTemp.getString(8));
			Button button = (Button) dialog.findViewById(R.id.Button_Ok);
			cursorTemp.close();
			sqlDb.close();
			button.setOnClickListener(new OnClickListener() 
			{
				@Override
				public void onClick(View v) 
				{
					dialog.dismiss();
				}
			});
			dialog.setCanceledOnTouchOutside(true);
			// now that the dialog is set up, it's time to show it
			dialog.show();
		}
		return true;
	}

}
