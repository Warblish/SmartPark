package park.smartpark;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class Map extends AppCompatActivity implements OnMapReadyCallback {
    public HashMap<Integer, ParkingLot> lotDictionary = new HashMap<Integer, ParkingLot>();
    private ViewGroup infoWindow;
    private OnInfoWindowElemTouchListener infoButtonListener;
    private GoogleMap mMap;
    volatile boolean busy = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_map);

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            this.infoWindow = (ViewGroup)getLayoutInflater().inflate(R.layout.info_window, null);
            Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
            setSupportActionBar(myToolbar);
            this.infoButtonListener = new OnInfoWindowElemTouchListener(((Button)infoWindow.findViewById(R.id.button)))
            {
                @Override
                protected void onClickConfirmed(View v, Marker marker) {
                    openDirectionsTo(marker.getPosition());
                }
            };
            ((Button)infoWindow.findViewById(R.id.button)).setOnTouchListener(infoButtonListener);
        } catch (Exception e) {
            Log.wtf("testerror", e.toString());
        }
    }
    public void openDirectionsTo(LatLng pos) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + pos.latitude + "," + pos.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }
    /*
    Following functions handle the action bar
    onCreateOptionsMenu sets up the action bar and adds any buttons from res/menu/main_menu.xml
    onOptionsItemSelected determines what occurs when the action bar button is clicked
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.

        switch(item.getItemId()){
            case(R.id.action_settings):
                //Handle if the settings button is pressed (nothing happens right now)
                return true;
            case(R.id.action_pay):
                //Handle when payment icon is pressed
                Intent intent = new Intent(this, PayActivity.class);
                //Send additional information to the pay activity
                //String message = editText.getText().toString();
                //intent.putExtra(EXTRA_MESSAGE, message);
                startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            mMap = googleMap;
            final MapWrapperLayout mapWrapperLayout = (MapWrapperLayout) findViewById(R.id.map_relative_layout);

            loadMarkers();
            mapWrapperLayout.init(mMap, getPixelsFromDp(this, 39 + 20));
            //mMap.setOnMarkerClickListener(this);
            //mMap.setOnInfoWindowClickListener(this);
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    // Setting up the infoWindow with current's marker info
                    try {
                        int pid = Integer.parseInt(marker.getTitle());
                        if (lotDictionary.containsKey(pid)) {
                            ParkingLot lot = lotDictionary.get(pid);
                            ((TextView) infoWindow.findViewById(R.id.title)).setText(lot.lotname);
                            ((TextView) infoWindow.findViewById(R.id.snippet0)).setText("Available Spots: " + lot.openspots);
                            ((TextView) infoWindow.findViewById(R.id.snippet1)).setText("Faculty Spots: " + lot.facultyspots);
                            ((TextView) infoWindow.findViewById(R.id.snippet2)).setText("Handicapped Spots: " + lot.handicapspots);
                            if(lot.students) {
                                ((TextView) infoWindow.findViewById(R.id.snippet3)).setText("Student Parking Allowed");
                            } else {
                                ((TextView) infoWindow.findViewById(R.id.snippet3)).setText("Student Parking NOT Allowed");
                            }
                            infoButtonListener.setMarker(marker);

                            mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow);
                            return infoWindow;
                        }

                    } catch (Exception e) {
                        Log.wtf("test", e.toString());
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            Log.wtf("testerror", e.toString());
        }
    }
    public void loadMarkers() {
        new HttpAsyncGet().execute("https://michigan-parking.appspot.com/api/parkingdata/?key=2ljbiiI7bo");
    }
    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
    public JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
        try {
            URL u = new URL(url);
            URLConnection con = u.openConnection();
            con.connect();
            InputStream is = con.getInputStream();//gah
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                String jsonText = readAll(rd);
                JSONArray json = new JSONArray(jsonText);
                return json;
            } finally {
                is.close();
            }
        } catch (Exception e) {
            //Log.wtf("testerror", e.toString());
        }
        return null;
    }
    private class HttpAsyncGet extends AsyncTask<String, Void, ArrayList<ParkingLot>> {
        @Override
        protected ArrayList<ParkingLot> doInBackground(String... url) {
            return getLots(url[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(ArrayList<ParkingLot> result) {
            Log.wtf("test", "size: " + result.size());
            for(ParkingLot m : result) {
                Log.wtf("test", m.getPosition().toString());
                MarkerOptions c = new MarkerOptions().position(m.getPosition());
                c.title("" + m.pid);
                lotDictionary.put(m.pid, m);
                mMap.addMarker(c);
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(m.getPosition()));
            }
            //Find the extreme bounds for LatLng (get the northern most, southern most, eastern most, western most points on the map)
            double[] extreme_coords = new double[4]; //Northern, southern, eastern, western
            for(int i = 0; i<4; i++) {
                for (ParkingLot m : result) {
                    if (extreme_coords[i] == 0){
                        //The most extreme coordinate has not been set so just set it to the first marker in the series
                        if(i==0 || i==1){
                            extreme_coords[i] = m.getPosition().latitude;
                        } else if(i==2 || i==3){
                            extreme_coords[i] = m.getPosition().longitude;
                        }
                    } else{
                        switch(i){
                            case 0:
                                //Most nothern
                                if(m.getPosition().latitude > extreme_coords[i]){
                                    extreme_coords[i] = m.getPosition().latitude;
                                }
                                break;
                            case 1:
                                //Most southern
                                if(m.getPosition().latitude < extreme_coords[i]){
                                    extreme_coords[i] = m.getPosition().latitude;
                                }
                                break;
                            case 2:
                                //Most eastern
                                if(m.getPosition().longitude > extreme_coords[i]){
                                    extreme_coords[i] = m.getPosition().longitude;
                                }
                                break;
                            case 3:
                                //Most western
                                if(m.getPosition().longitude < extreme_coords[i]){
                                    extreme_coords[i] = m.getPosition().longitude;
                                }
                                break;
                        }
                    }
                }
            }
            //Print values to console for debugging purposes
            //Log.wtf("northern most extreme bound", Double.toString(extreme_coords[0]));
            //Log.wtf("southern most extreme bound", Double.toString(extreme_coords[1]));
            //Log.wtf("eastern most extreme bound", Double.toString(extreme_coords[2]));
            //Log.wtf("western most extreme bound", Double.toString(extreme_coords[3]));
            //Create bounds for the camera so the map encompasses the whole campus, but is still zoomed in
            //Create new LatLng objects for the two corners (northwest and southeast)
            LatLng northeast = new LatLng(extreme_coords[0], extreme_coords[2]);
            LatLng southwest = new LatLng(extreme_coords[1], extreme_coords[3]);
            //Create new LatLngBounds object
            LatLngBounds bounds =  new LatLngBounds(southwest, northeast);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }
    }
    public ArrayList<ParkingLot> getLots(String url) {
        ArrayList<ParkingLot> ms = new ArrayList<ParkingLot>();
        //JSONArray dataArray = new JSONArray();
        try {
            JSONArray dataArray = readJsonFromUrl("http://michigan-parking.appspot.com/api/parkingdata/?key=2ljbiiI7bo");
            /*Iterator x = data.keys();
            while (x.hasNext()){
                Log.wtf("test", "cp3");
                String key = (String) x.next();
                dataArray.put(data.get(key));
            }*/

            for (int i = 0; i < dataArray.length(); i++)
            {
                double lat = dataArray.getJSONObject(i).getDouble("lat");
                double longi = dataArray.getJSONObject(i).getDouble("long");
                int pid = dataArray.getJSONObject(i).getInt("id");
                int openspots = dataArray.getJSONObject(i).getInt("openspots");
                int handicapspots = dataArray.getJSONObject(i).getInt("handicapspots");
                int facultyspots = dataArray.getJSONObject(i).getInt("handicapspots");
                int totalspots = dataArray.getJSONObject(i).getInt("maximumopenspots");
                String lotname = dataArray.getJSONObject(i).getString("name");
                boolean students = dataArray.getJSONObject(i).getInt("students_allowed")==1;
                LatLng location = new LatLng(lat,longi);
                ParkingLot m = new ParkingLot(pid, location, lotname, totalspots, openspots, handicapspots, facultyspots, students);
                ms.add(m);
                /*MarkerOptions m = new MarkerOptions();
                m.position(location);
                m.title(lotname);
                m.snippet("Available Spots: " + spots);

                ms.add(m);*/

            }

        } catch (Exception e) {
            Log.wtf("testerror2", e.toString());
        }
        return ms;
    }
    public static int getPixelsFromDp(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }
}