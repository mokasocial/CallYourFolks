package com.mokasocial.callyourfolks;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class MainActivity extends ListActivity {
	
	public static final int CHOOSE_FOLKS = 1;
	public static final int EDIT_FOLK = 2;

	public static final int CALL_YOUR_FOLKS_UNIQUE_ID = 2;
	
	FolkAdapter mFolkAdapter;
	public Context mContext;
	GoogleAnalyticsTracker tracker;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        tracker = GoogleAnalyticsTracker.getInstance();

	    // Start the tracker in manual dispatch mode...
	    tracker.start("UA-12331601-7", this);
	    tracker.setCustomVar(1, "Navigation Type", "Button click", 2);
        tracker.trackPageView("/MainActivity");
        
        setContentView(R.layout.main);
        mContext = this;
        
        CallReminderManager.initNotifications(mContext);
        
        try {
			mFolkAdapter = new FolkAdapter(mContext, R.layout.folk_row, Database.fetchAllFolks(mContext));
			setListAdapter(mFolkAdapter);
		} catch (NotFoundException e) {
			Log.e("Main", "Unable to get any folks from database");
		}
		
		// set up add button
		final Button addButton = (Button) findViewById(R.id.add);
		addButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				//startActivityForResult, passing in this Intent (and a request code integer, PICK_CONTACT
		    	Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
		    	startActivityForResult(intent, CHOOSE_FOLKS);
			}
		});
		
		// Hide the welcome text on the landing page when tapped
		findViewById(R.id.landing_content).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				TranslateAnimation slide = new TranslateAnimation(0.0f, 0.0f, 0.0f, -(v.getHeight()));
				slide.setDuration(1000);
				slide.setAnimationListener(collapseListener);
				
				findViewById(R.id.landing_content).startAnimation(slide);
			}
		});
	
    }
    
    Animation.AnimationListener collapseListener = new Animation.AnimationListener() {
		public void onAnimationEnd(Animation animation) {
			findViewById(R.id.landing_content).setVisibility(View.GONE);
		}

		public void onAnimationRepeat(Animation animation) {
			// not needed
		}

		public void onAnimationStart(Animation animation) {
			// not needed
		}
	};


    @Override
	public boolean onSearchRequested(){
        //startActivityForResult, passing in this Intent (and a request code integer, PICK_CONTACT
    	Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
    	startActivityForResult(intent, CHOOSE_FOLKS);
		return false;
    }

    @Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
    	if (resultCode == RESULT_OK) {
    		switch (requestCode) {
    		case EDIT_FOLK:
    			// We came back from editing a contact
    			tracker.trackPageView("/EditedFolk");
    			tracker.dispatch();

//    			Log.d("Main", "Got an edit folk, doing notifyDataSetChanged();");
//    			try {
//					Thread.sleep(100);
//				} catch (InterruptedException e1) {
//					e1.printStackTrace();
//				}
    			mFolkAdapter.notifyDataSetChanged();
    			break;
    		case CHOOSE_FOLKS:
    			// We chose a new contact
    			tracker.trackPageView("/AddedNewFolk");
    			tracker.dispatch();
    			Cursor cursor = null;
    			try {
    				Uri result = data.getData();
    				Log.v("MainActivity", "Got a contact result: " + result.toString());

    				// get the contact id from the Uri
    				String id = result.getLastPathSegment();

    				// query for everything phone  
    				cursor = getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?", new String[] { id }, null);

    				int phoneIdx = cursor.getColumnIndex(Phone.DATA);

    				// Are there any?
    				if (cursor.moveToFirst()) {
    					// One or more
    					String phoneEntry = cursor.getString(phoneIdx);
    					Log.v("MainActivity", "Got phone: " + phoneEntry);
    					while (cursor.moveToNext()){
    						phoneEntry = cursor.getString(phoneIdx);
        					Log.v("MainActivity", "Got phone: " + phoneEntry);
    					}
    					
    					// Start edit activity
                    	Uri contactUri = Contacts.lookupContact(getContentResolver(), result);
                    	Intent intent = new Intent(this, FolkActivity.class);
                    	intent.putExtra(FolkActivity.EXTRA_ACTION, FolkActivity.NEW_FOLK);
                    	intent.putExtra(FolkActivity.EXTRA_CONTACT_URI, contactUri);                    	
                		startActivityForResult(intent, EDIT_FOLK);
       				} else {
    					Log.w("MainActivity", "No results");
    					Toast.makeText(this, "This contact doesn't have any phone numbers!", Toast.LENGTH_LONG).show();  
    				}
    			} catch (Exception e) {  
    				Log.e("MainActivity", "Failed to get phone data", e);  
    			} finally {  
    				if (cursor != null) {  
    					cursor.close();
    				}    				
    			}
    			break;  
    		}  

    	} else {  
    		Log.w("ActivityMain", "Warning: activity result not ok");  
    	}  
    }
    
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// Start a FolkActivity where we can edit this contact's information
		final Folk folk = mFolkAdapter.getItem(position);
		Uri contactUri = folk.contact_uri;
		Intent intent = new Intent(this, FolkActivity.class);
    	intent.putExtra(FolkActivity.EXTRA_ACTION, FolkActivity.NEW_FOLK);
    	intent.putExtra(FolkActivity.EXTRA_CONTACT_URI, contactUri);                    	
		startActivityForResult(intent, EDIT_FOLK);
	}
	
  @Override
  protected void onDestroy() {
    super.onDestroy();
    // Stop the tracker when it is no longer needed.
    tracker.stop();
  }
}