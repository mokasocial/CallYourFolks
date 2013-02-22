package com.mokasocial.callyourfolks;

import java.util.ArrayList;
import java.util.Random;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FolkAdapter extends ArrayAdapter<Folk> {

	public Context mContext;
	public ArrayList<Folk> mItems;

	public FolkAdapter(Context context, int textViewResourceId, ArrayList<Folk> objects) {
		super(context, textViewResourceId, objects);
		mItems = objects;
		mContext = context;
	}

	@Override
	public int getCount() {
		return mItems.size();
	}

	@Override
	public Folk getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public int getPosition(Folk item) {
		return mItems.indexOf(item);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			LayoutInflater viewInflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = viewInflator.inflate(R.layout.folk_row, null);
		}

		Folk folk = mItems.get(position);
		if (folk != null) {
			final TextView name = (TextView) view.findViewById(R.id.folk_name);
			name.setText(getNameFromContentUri(folk.contact_uri));
			final TextView description = (TextView) view.findViewById(R.id.folk_contact_frequency);

			// Build talk-verbs array from source and choose one at random
			String[] talkVerb = mContext.getResources().getStringArray(R.array.talkVerbs);
			Random r = new Random();
			int index = r.nextInt(talkVerb.length);
			String talk = talkVerb[index].substring(0, 1).toUpperCase() + talkVerb[index].substring(1);

			description.setText(talk + " at least once every " + FolkActivity.getStringDescriptionOfDays(folk.frequency));

		} else {
			Log.d("FolkAdapter", "Contact was null!");
		}

		return view;
	}

	private String getNameFromContentUri(Uri contact_uri) {
		// Set header text
		String contactName = "";

		ContentResolver cr = mContext.getContentResolver();
		Cursor cur = cr.query(contact_uri, null, null, null, null);
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				contactName = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			}
		}

		if (contactName == "") {
			// No name? WTF?
			contactName = "Unnamed Contact";
		}
		return contactName;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		mItems = Database.fetchAllFolks(mContext);
	}

}
