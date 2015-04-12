package com.example.binloc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class MainActivity extends Activity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    public static final int DOCUMENT_REQUEST_CODE = 0;

    public static final boolean debugToasts = false;
    
    Bin [] closeBins = new Bin [5];
	static Bin myLocation = new Bin(new LatLng(0, 0), 0B1000);

    GoogleMap map = null;

    volatile boolean locFound = false;

    Document [] docs = new Document[closeBins.length];

	private void loadBins() throws IOException {
        Pattern p = Pattern.compile("([0-9]+\\.[0-9]+),\\s(-[0-9]+\\.[0-9]+)\\stypes\\s=\\s([0-9]+)");

        String str = "";
        InputStream is = getAssets().open("bins.txt");
        //InputStream is = getResources().openRawResource(R.raw.bins);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (is != null) {
            for (int i = 0; (str = reader.readLine()) != null; ++i) {
                str += "\n";
                Matcher m = p.matcher(str);
                m.find();

                double lat = Double.parseDouble(m.group(1)), lon = Double.parseDouble(m.group(2));
                int types = Integer.parseInt(m.group(3));

                Bin curBin = new Bin(new LatLng(lat, lon), types);
                if (i < closeBins.length || closeBins[closeBins.length - 1].compareTo(curBin) > 0) {
                    int farthest = Math.min(closeBins.length - 1, i);
                    closeBins[farthest] = curBin;
                    for (int j = farthest; j > 0 && closeBins[j - 1].compareTo(curBin) > 0; --j) {
                        closeBins[j] = closeBins[j - 1];
                        closeBins[j - 1] = curBin;
                    }
                }
            }
        }
        is.close();
    }

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (debugToasts) {
        	Toast.makeText(getBaseContext(), "Starting Load", Toast.LENGTH_LONG).show();
        }
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //*/
        updateUserLocation();
	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateUserLocation(){
//    	 textLat = (TextView)findViewById(R.id.textLat);
//       textLong = (TextView)findViewById(R.id.textLong);

         LocationManager Lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
         LocationListener Ll = new myLocationListener();
         Lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, Ll);
    }

    //*
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (debugToasts) {
        	Toast.makeText(getBaseContext(), "Map Ready", Toast.LENGTH_LONG).show();
        }
        map = googleMap;
        map.setOnMarkerClickListener(this);
        if (closeBins[0] != null) {
            addMarkers();
        }
    }

    private void addMarkers() {
        if (debugToasts) {
        	Toast.makeText(getBaseContext(), "Adding my location Marker", Toast.LENGTH_LONG).show(); }
        map.addMarker(new MarkerOptions().position(myLocation.getLatLng()).title("Me"));

        GMapV2Direction directionFinder = new GMapV2Direction();
        if (debugToasts) {
        	Toast.makeText(getBaseContext(), "Requesting documents", Toast.LENGTH_LONG).show(); }

        /*
        for (int i = 0; i < closeBins.length; ++i) {
            docs[i] = directionFinder.getDocument(myLocation.getLatLng(), closeBins[i].getLatLng(), GMapV2Direction.MODE_WALKING);
            if (docs[i] == null) {
                if (debugToasts) {
                    Toast.makeText(getBaseContext(), "Failed to get Document for " + (i + 1) + "th node", Toast.LENGTH_LONG).show();
                    Toast.makeText(getBaseContext(), "Aborting", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }

        if (debugToasts) {
            Toast.makeText(getBaseContext(), "Finding Closest and Placing markers", Toast.LENGTH_LONG).show();
        }
        int closest = 0;
        for (int i = 0, minDist = directionFinder.getDistanceValue(docs[docs.length]+5000); i < closeBins.length; ++i) {
            if (directionFinder.getDistanceValue(docs[i]) < minDist) {
                MarkerOptions mOptions = new MarkerOptions();
                mOptions = mOptions.position(closeBins[closest].getLatLng());
                mOptions = mOptions.title("Bin" + (closest+1));
                map.addMarker(mOptions);

                closest = i;
                minDist = directionFinder.getDistanceValue(docs[i]);
            } else {
                MarkerOptions mOptions = new MarkerOptions();
                mOptions = mOptions.position(closeBins[i].getLatLng());
                mOptions = mOptions.title("Bin" + (i+1));
                map.addMarker(mOptions);
            }
        }
        /*/

        int closest = 0;
        for (int i = 0; i < closeBins.length; ++i) {
            if (i == 0) {
                MarkerOptions mOptions = new MarkerOptions();
                mOptions = mOptions.position(closeBins[0].getLatLng());
                mOptions = mOptions.title("Bin" + 1);
                map.addMarker(mOptions);
            } else {
                MarkerOptions mOptions = new MarkerOptions();
                mOptions = mOptions.position(closeBins[i].getLatLng());
                mOptions = mOptions.title("Bin" + (i+1));
                map.addMarker(mOptions);
            }
        }

        if (debugToasts) {
            Toast.makeText(getBaseContext(), "Markers Added", Toast.LENGTH_LONG).show();
        }

        /*
        ArrayList<LatLng> directionPoint = directionFinder.getDirection(docs[closest]);
        PolylineOptions rectLine = new PolylineOptions().width(3).color(Color.RED);

        for (int i = 0; i < directionPoint.size(); i++) {
            rectLine.add(directionPoint.get(i));
        }

        map.addPolyline(rectLine);
        if (debugToasts) {
            Toast.makeText(getBaseContext(), "Directions Added", Toast.LENGTH_LONG).show();
        }
        //*/
    }
    //*/

    public void startMaps(LatLng end, char mode){
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + end.latitude +"," + end.longitude +"&mode=" + mode);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Pattern p = Pattern.compile("Bin([0-9]+)");
        Matcher m = p.matcher(marker.getTitle());
        if (m.find())
        {
            startMaps(closeBins[Integer.parseInt(m.group(1))-1].getLatLng(), 'w');
        }
        return false;
    }

    /*
    public void requestDocument(LatLng start, LatLng end, String mode, int binNum) {
        String url = "https://maps.googleapis.com/maps/api/directions/xml?"
                + "origin=" + start.latitude + "," + start.longitude
                + "&destination=" + end.latitude + "," + end.longitude
                + "&sensor=false&units=metric&mode="+mode
                + "&key=AIzaSyCWNzd3bEhlHgRKtZ8_FImb2iE-FPitg8Q";

        try {
            Intent serviceIntent = new Intent(this, DownloadIntentService.class);
            PendingIntent pendingResult = createPendingResult(DOCUMENT_REQUEST_CODE, new Intent(), 0);
            serviceIntent.putExtra(DownloadIntentService.URL_EXTRA, url);
            serviceIntent.putExtra(DownloadIntentService.PENDING_RESULT_EXTRA, pendingResult);
            serviceIntent.putExtra(DownloadIntentService.BIN_NUMBER, binNum);
            startService(serviceIntent);
            /*
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpPost httpPost = new HttpPost(url);
            HttpResponse response = httpClient.execute(httpPost, localContext);
            InputStream in = response.getEntity().getContent();
            //*/  /*

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DownloadIntentService.RESULT_CODE) {
            DocumentBuilder builder = null;
            try {
                builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            int binNum = data.getIntExtra(DownloadIntentService.BYTE_ARR_EXTRA, 0);
            InputStream in = new ByteArrayInputStream(data.getByteArrayExtra(DownloadIntentService.BYTE_ARR_EXTRA));

            try {
                docs[binNum] = builder.parse(in);
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (binNum == 4) {
                GMapV2Direction directionFinder = new GMapV2Direction();

                for (int i = 0; i < closeBins.length; ++i) {
                    if (docs[i] == null && debugToasts) {
                        Toast.makeText(getBaseContext(), "Failed to get Document for " + (i + 1) + "th node", Toast.LENGTH_LONG).show();
                        Toast.makeText(getBaseContext(), "Aborting", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                if (debugToasts) {
                    Toast.makeText(getBaseContext(), "Finding Closest and Placing markers", Toast.LENGTH_LONG).show();
                }
                int closest = 0;
                for (int i = 1, minDist = directionFinder.getDistanceValue(docs[0]); i < closeBins.length; ++i) {
                    if (directionFinder.getDistanceValue(docs[i]) < minDist) {
                        MarkerOptions mOptions = new MarkerOptions();
                        mOptions = mOptions.position(closeBins[closest].getLatLng());
                        mOptions = mOptions.title("Bin" + (closest+1));
                        map.addMarker(mOptions);

                        closest = i;
                        minDist = directionFinder.getDistanceValue(docs[i]);
                    } else {
                        MarkerOptions mOptions = new MarkerOptions();
                        mOptions = mOptions.position(closeBins[i].getLatLng());
                        mOptions = mOptions.title("Bin" + (i+1));
                        map.addMarker(mOptions);
                    }
                }

                if (debugToasts) {
                    Toast.makeText(getBaseContext(), "Markers Added", Toast.LENGTH_LONG).show();
                }

                ArrayList<LatLng> directionPoint = directionFinder.getDirection(docs[closest]);
                PolylineOptions rectLine = new PolylineOptions().width(3).color(Color.RED);

                for (int i = 0; i < directionPoint.size(); i++) {
                    rectLine.add(directionPoint.get(i));
                }

                map.addPolyline(rectLine);
                if (debugToasts) {
                    Toast.makeText(getBaseContext(), "Directions Added", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    //*/

    class myLocationListener implements LocationListener{

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			if(location != null){
				double pLong = location.getLongitude();
				double pLat = location.getLatitude();

				myLocation = new Bin(new LatLng(pLat, pLong), 0B1000);

//				textLat.setText(Double.toString(pLat));
//				textLong.setText(Double.toString(pLong));

                if (!locFound)
                {
                    locFound = true;

                    if (debugToasts)
                    {
                        Toast.makeText(getBaseContext(), "Found Location", Toast.LENGTH_LONG).show();
                    }

                    try {
                        loadBins();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    //*
                    if (map != null) {
                        addMarkers();
                    }
                    //*/
                }
			}
        }

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onProviderDisabled(String provider) {}
    	
    }
    
}
