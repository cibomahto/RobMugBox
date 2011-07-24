package cibomahto.robmug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class RobMugBoxActivity extends Activity {
	
	Location myLocation;
	Location wafaaLocation;
	Location wafaaHomeLocation;
	
	private Timer myTimer;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.setRequestedOrientation(
        		ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        // Set up home location
        wafaaHomeLocation = new Location("fixed");
        wafaaHomeLocation.setLatitude(40.726753);
        wafaaHomeLocation.setLongitude(-73.996324);
        
        // Start a timer to do screen updates
		myTimer = new Timer();
		myTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}

		}, 0, 30000);
		
		/* Use the LocationManager class to obtain GPS locations */
		// TODO: Can we just query for last known?
		LocationManager mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		LocationListener mlocListener = new MyLocationListener();
		mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
		mlocManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, 0, 0, mlocListener);
    } 
    
    /* Lets grab the text of the project website, and try to pull the latitude and longitude out!
	    Website is here:
	    http://www.3rdi.me/server.php?order_by=timestamp&order_dir=desc&limit=1
	
		and we expect something like this:
    	[{"id":"214507","filename":"06-25-2011\/06-35-20.jpg","timestamp":"1308983720","latitude":"52.5186","longitude":"13.4031","altitude":"130","replies":0}]
      	
      	So lets be naive and cheat!
    */
    private Location getWafaaLocation() {
    	
    	final String websiteURL = "http://www.3rdi.me/server.php?order_by=timestamp&order_dir=desc&limit=1";
    	
    	Location newLocation = null;
    	String locationData = null;
    	
    	try {
    		URL url = new URL(websiteURL);
    		URLConnection con = url.openConnection();
    		InputStream in = con.getInputStream();

			BufferedReader reader;
    		
			reader = new BufferedReader(
			    new InputStreamReader(in)
			  );
    	 
    		// There should only be one line of data.
			locationData = reader.readLine();
			
		} catch (ClientProtocolException e) {
	        Toast.makeText(getApplicationContext(),
	        		"protocol exception!",
	        		Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
	        Toast.makeText(getApplicationContext(),
	        		"io exception!",
	        		Toast.LENGTH_SHORT).show();
		}
		
    	//if we got both latitude and longitude, hooray!
		if (locationData != null) {
			
			String latString = locationData.substring(locationData.indexOf("latitude\":\"")+11,locationData.indexOf("\",\"longitude"));
			String longString = locationData.substring(locationData.indexOf("longitude\":\"")+12,locationData.indexOf("\",\"altitude"));
			
			double latitude = Double.valueOf(latString);
			double longitude = Double.valueOf(longString);
			
	        newLocation = new Location("web");
	        newLocation.setLatitude(latitude);
	        newLocation.setLongitude(longitude);
		}
    	
    	return newLocation;
    }
    
    int robEnableColor = 0xFF00FF00;
    int mugEnableColor = 0xFFFF0000;
    int disabledColor =  0xFF202020;
    
    private void updateDisplay(boolean rob, boolean mug) {
    	TextView robView = (TextView)findViewById(R.id.textViewRob);
    	TextView mugView = (TextView)findViewById(R.id.textViewMug);
    	
    	robView.setTextColor(rob?robEnableColor:disabledColor);
    	mugView.setTextColor(mug?mugEnableColor:disabledColor);    	
    }
    
    /* Timer example from here:
     * http://steve.odyfamily.com/?p=12
     */
	private void TimerMethod()
	{
		//This method is called directly by the timer
		//and runs in the same thread as the timer.

		//We call the method that will work with the UI
		//through the runOnUiThread method.
		this.runOnUiThread(Timer_Tick);
	}

	// If the distance between lat & long are small, then they are close.
	private boolean areLocationsClose(Location a, Location b) {
		double maxDistance = .01;
		
		double distanceLat = Math.abs(a.getLatitude() - b.getLatitude());
		double distanceLong = Math.abs(a.getLongitude() - b.getLongitude()); 
		double distance = Math.sqrt(distanceLat*distanceLat + distanceLong*distanceLong);
		
		return distance < maxDistance;
	}
	
	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			
			// Try to update wafaa location
			Location newWafaaLocation = getWafaaLocation();
			if (newWafaaLocation != null) {
				wafaaLocation = newWafaaLocation;
			}
		
//			// If we know my location, display it.
//			if (myLocation != null) {
//				Toast.makeText(getApplicationContext(),
//						"My current location is: " + "Lat = "
//						+ myLocation.getLatitude() + "Long = " + myLocation.getLongitude(),
//						Toast.LENGTH_SHORT).show();
//			}
//			else {
//				Toast.makeText(getApplicationContext(), "Dunno my location!",
//						Toast.LENGTH_SHORT).show();				
//			}
//			
//			// If we know wafaa's location, display it.
//			if (wafaaLocation != null) {
//				Toast.makeText(getApplicationContext(),
//						"Wafaa current location is: " + "Lat = "
//						+ wafaaLocation.getLatitude() + "Long = " + wafaaLocation.getLongitude(),
//						Toast.LENGTH_SHORT).show();
//			}
//			else {
//				Toast.makeText(getApplicationContext(), "Dunno wafaa location!",
//						Toast.LENGTH_SHORT).show();				
//			}

			boolean wafaaIsHome = false;
			boolean wafaaIsNear = false;
			
			// If we know both Wafaa's location and his home location, compute rob
			if (wafaaLocation != null && wafaaHomeLocation != null) {
				wafaaIsHome = areLocationsClose(wafaaLocation, wafaaHomeLocation); 
			}
			
			// If we know both our location and Wafaa's location, compute mug
			if (wafaaLocation != null && myLocation != null) {
				wafaaIsNear = areLocationsClose(wafaaLocation, myLocation); 
			}
			
			boolean rob = false;
			boolean mug = false;
			
			if (!wafaaIsHome) {
				if (wafaaIsNear) {
					mug = true;
				}
				else {
					rob = true;
				}
			}
			
			updateDisplay(rob, mug);
		}
	};
	
	/* Class My Location Listener, gets, well, my location!
	 * From here:
	 * http://www.firstdroid.com/2010/04/29/android-development-using-gps-to-get-current-location-2/
	 * */
	public class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location loc) {
			myLocation = loc;
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

	}/* End of Class MyLocationListener */
}