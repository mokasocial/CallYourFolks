package com.mokasocial.callyourfolks;

import java.util.ArrayList;
import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

public class NotificationMaker extends BroadcastReceiver {

	Long DAY_IN_MILLIS = Long.valueOf(24 * 60 * 60 * 1000);

	@Override
	public void onReceive(Context context, Intent intent) {
		// The alarm service has decided it is time to check out our folks.
		// We might make some notifications.
		Log.d("NotificationMaker", "onReceive was called.");

		// Get a reference to the NotificationManager:
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
		mNotificationManager.cancelAll();
		String[] talkVerb = context.getResources().getStringArray(R.array.talkedVerbs);

		try {
			ArrayList<Folk> folks = Database.fetchAllFolks(context);

			// check each folk to see if we need to call them
			for (Folk folk : folks) {
				// clear old notifications
				String id = "";
				String contactName = "";
				Long contactLastContacted = (long) 0;
				ContentResolver cr = context.getContentResolver();
				Cursor cur = cr.query(folk.contact_uri, null, null, null, null);
				if (cur.getCount() > 0) {
					while (cur.moveToNext()) {
						// Log.d("Checking contact", "for " + folk.contact_uri +
						// ": has a result: " +
						// cur.getString(cur.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)));
						id = cur.getString(cur.getColumnIndex(ContactsContract.Data._ID));
						contactName = cur.getString(cur.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
						contactLastContacted = Long.parseLong(cur.getString(cur.getColumnIndex(ContactsContract.Data.LAST_TIME_CONTACTED)));
						// Log.d("Contact row: ",
						// DatabaseUtils.dumpCursorToString(cur));
						if (contactLastContacted == 0) {
							// are we positive? this could be a dumb contacts
							// engine (like htc hero or aria)
							// check outgoing call log
							String[] projection = new String[] { BaseColumns._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE };
							Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DEFAULT_SORT_ORDER);
							if (cursor.getCount() > 0) {
								while (cursor.moveToNext()) {
									String newName = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
									if (newName != null) {
										if (newName.contentEquals(contactName)) {
											contactLastContacted = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
											break;
										}
									}
								}
							}
							cursor.close();
						}
					}
				} else {
					Log.d("No contact!", folk.contact_uri + " has no results!");
				}

				Long folkIntervalMillis = folk.frequency * DAY_IN_MILLIS;
				Log.d("contactLastContacted", contactLastContacted + "");

				// are we overdue to call them?
				if (contactLastContacted == 0) {
					// they've *never* talked to this contact...
					CharSequence contentText;
					Random r = new Random();
					int index = r.nextInt(talkVerb.length);
					contentText = "You haven't " + talkVerb[index] + " in forever!";
					Notification notification = makeNotification(contactName);

					// Define the Notification's expanded message and Intent:
					Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Contacts.CONTENT_URI);
					notificationIntent.setData(folk.contact_uri);
					PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
					notification.setLatestEventInfo(context, "Call " + contactName, contentText, contentIntent);

					// Pass the Notification to the NotificationManager:
					mNotificationManager.notify(id.hashCode(), notification);
				} else if (contactLastContacted + folkIntervalMillis < System.currentTimeMillis()) {
					long daysSinceDecimalForm = (System.currentTimeMillis() - contactLastContacted) / DAY_IN_MILLIS;
					int daysSince = (int) daysSinceDecimalForm;

					// Define the Notification's expanded message and Intent:
					CharSequence contentTitle = "Call " + contactName;
					CharSequence contentText;

					Random r = new Random();
					int index = r.nextInt(talkVerb.length);

					Notification notification = makeNotification(contactName);

					if (daysSince == 1) {
						contentText = "You haven't " + talkVerb[index] + " all day!";
					} else {
						contentText = "You haven't " + talkVerb[index] + " in " + daysSince + " days!";
					}

					Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Contacts.CONTENT_URI);
					notificationIntent.setData(folk.contact_uri);
					PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
					notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

					// Pass the Notification to the NotificationManager:
					mNotificationManager.notify(id.hashCode(), notification);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	Notification makeNotification(String contactName) {
		// Instantiate the Notification:
		int icon = R.drawable.notification_icon;
		CharSequence tickerText = "Call " + contactName;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.FLAG_AUTO_CANCEL;

		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		return notification;
	}
}