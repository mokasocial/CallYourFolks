package com.mokasocial.callyourfolks;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallReminderManager {
	
	/**
	 * Set up the alarm service to run in the background.
	 * When the scheduled alarm hits, it checks for outstanding Folks to call, and creates notifications.
	 */
	public static void initNotifications(Context context) {

		Log.d("initNotifications", "initNotifications was called.");
		
		// get a Calendar object with current time
		Calendar cal = Calendar.getInstance();
		
		// add 1 minutes to the calendar object
		Intent intent = new Intent(context, NotificationMaker.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, MainActivity.CALL_YOUR_FOLKS_UNIQUE_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
		am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_HOUR, sender);
	}
}