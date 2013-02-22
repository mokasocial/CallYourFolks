package com.mokasocial.callyourfolks;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class FolkActivity extends Activity {

	// bundle keys
	static final String EXTRA_ACTION = "action";
	static final String EXTRA_CONTACT_URI = "contact_uri";

	// action codes
	static final int NEW_FOLK = 1;
	static final int EDIT_FOLK = 2;

	int actionCode; // one of the above
	Context mContext;
	Uri contactUri;
	GoogleAnalyticsTracker tracker;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_folk);
		mContext = this;

		tracker = GoogleAnalyticsTracker.getInstance();

		// Start the tracker in manual dispatch mode...
		tracker.start("UA-12331601-7", this);
		tracker.setCustomVar(1, "Navigation Type", "Button click", 2);

		// Handle our seekbar
		final SeekBar seekBar = (SeekBar) findViewById(R.id.frequency_slider);
		final TextView seekBarValue = (TextView) findViewById(R.id.seekbarvalue);

		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int numDays = getDaysFromProgressBar(progress);
				String timeString = getStringDescriptionOfDays(numDays);
				Spanned text = Html.fromHtml(getResources().getText(R.string.every) + " <b>" + timeString + "</b>");
				seekBarValue.setText(text);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

		});

		// Grab bundled data for contact
		Intent intent = this.getIntent();
		Bundle extras = intent.getExtras();
		contactUri = (Uri) extras.get(EXTRA_CONTACT_URI);
		Folk folk;
		try {
			folk = Database.fetchFolkByContactUri(this, contactUri);
		} catch (NotFoundException e) {
			// this is a new contact then, so blank freq.
			folk = new Folk();
			folk.contact_uri = contactUri;
		}

		String contactName = getFolkName(folk);

		TextView header = (TextView) findViewById(R.id.folk_header);
		Spanned text = Html.fromHtml("Choose how often you'd like to be reminded to talk with <b>" + contactName + "</b> on the phone:");
		header.setText(text);

		// Set seekbar to current frequency
		int seekBarNum = getProgressBarFromDays(folk.frequency);
		seekBar.setProgress(1); // this is here so that even if seekBarNum is 0,
								// onChange() will get called. :(
		seekBar.setProgress(seekBarNum);

		// Set up buttons
		final Button saveButton = (Button) findViewById(R.id.save);
		saveButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Folk thisFolk = new Folk();
				thisFolk.contact_uri = contactUri;
				thisFolk.frequency = getDaysFromProgressBar(seekBar.getProgress());
				saveToDatabase(thisFolk);
				setResult(Activity.RESULT_OK);
				finish();
			}
		});
	}

	void saveToDatabase(Folk folk) {
		Database.saveFolk(this, folk);
	}

	void deleteFromDatabase(Folk folk) {
		Database.deleteFolk(this, folk);
	}

	public String getFolkName(Folk folk) {
		String contactName = "";
		ContentResolver cr = getContentResolver();
		Cursor cur = cr.query(folk.contact_uri, null, null, null, null);
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				// id =
				// cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
				contactName = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			}
		}
		if (contactName == null) {
			// Null name? WTF?
			contactName = "Unnamed Contact";
		} else if (contactName == "") {
			// No name? WTF?
			contactName = "Unnamed Contact";
		}
		return contactName;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete:
			Folk thisFolk = new Folk();
			thisFolk.contact_uri = contactUri;
			deleteFromDatabase(thisFolk);
			setResult(Activity.RESULT_OK);
			tracker.trackPageView("/DeletedFolk");
			tracker.dispatch();
			finish();
			return true;
		}

		return false;
	}

	public static int getDaysFromProgressBar(int progress) {
		switch (progress) {
		case 7:
			return 14;
		case 8:
			return 21;
		case 9:
			return 30;
		case 10:
			return 60;
		case 11:
			return 90;
		default:
			return progress + 1;
		}
	}

	public static int getProgressBarFromDays(int days) {

		switch (days) {
		case 14:
			return 7;
		case 21:
			return 8;
		case 30:
			return 9;
		case 60:
			return 10;
		case 90:
			return 11;
		default:
			return days - 1;
		}
	}

	public static String getStringDescriptionOfDays(int days) {
		switch (days) {
		case 1:
			return "day";
		case 7:
			return "week";
		case 14:
			return "2 weeks";
		case 21:
			return "3 weeks";
		case 30:
			return "month";
		case 60:
			return "2 months";
		case 90:
			return "3 months";
		default:
			return days + " days";
		}

	}

	// handy ascii art chart:
	// progress
	// 0 1 2 3 4 5 6 7 8 9 10 11
	// days
	// 1 2 3 4 5 6 7 14 21 30 60 90
	// string
	// 1 2 3 4 5 6 w 2w 3w 1m 2m 3m
}