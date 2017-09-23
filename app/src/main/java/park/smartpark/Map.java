package park.smartpark;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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

public class Map extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    volatile boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
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
        mMap = googleMap;
        loadMarkers();
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
            Log.wtf("testerror", e.toString());
        }
        return null;
    }
    private class HttpAsyncGet extends AsyncTask<String, Void, ArrayList<MarkerOptions>> {
        @Override
        protected ArrayList<MarkerOptions> doInBackground(String... url) {
            return getMarkers(url[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(ArrayList<MarkerOptions> result) {
            Log.wtf("test", "cp5");
            for(MarkerOptions m : result) {
                mMap.addMarker(m);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(m.getPosition()));
            }
            //Find the extreme bounds for LatLng (get the northern most, southern most, eastern most, western most points on the map)
            double[] extreme_coords = new double[4]; //Northern, southern, eastern, western
            for(int i = 0; i<4; i++) {
                for (MarkerOptions m : result) {
                    if (extreme_coords[i] == 0){
                        //The most extreme coordinate has not been set so just set it to the first marker in the series
                        if(i==0 || i==1){
                            extreme_coords[i] = m.getPosition().latitude;
                        } else if(i==2 || i==3){
                            extreme_coords[i] = m.getPosition().longitude;
                        }
                    } else{
                        //Update if the next coordinate is more extreme than the current one in extreme_coords
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
            Log.wtf("northern most extreme bound", Double.toString(extreme_coords[0]));
            Log.wtf("southern most extreme bound", Double.toString(extreme_coords[1]));
            Log.wtf("eastern most extreme bound", Double.toString(extreme_coords[2]));
            Log.wtf("western most extreme bound", Double.toString(extreme_coords[3]));
            //Create bounds for the camera so the map encompasses the whole campus, but is still zoomed in
            //Create new LatLng objects for the two corners (northwest and southeast)
            LatLng northeast = new LatLng(extreme_coords[0], extreme_coords[2]);
            LatLng southwest = new LatLng(extreme_coords[1], extreme_coords[3]);
            //Create new LatLngBounds object
            LatLngBounds bounds =  new LatLngBounds(southwest, northeast);
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            Log.wtf("test", "cp6");
        }
    }
    public ArrayList<MarkerOptions> getMarkers(String url) {
        ArrayList<MarkerOptions> ms = new ArrayList<MarkerOptions>();
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
                int spots = dataArray.getJSONObject(i).getInt("openspots");
                String lotname = dataArray.getJSONObject(i).getString("name");
                LatLng location = new LatLng(lat,longi);
                MarkerOptions m = new MarkerOptions();
                m.position(location);
                m.title(lotname);
                m.snippet("Available Spots: " + spots);

                ms.add(m);

            }

        } catch (Exception e) {
            Log.wtf("testerror2", e.getLocalizedMessage());
        }
        return ms;
    }
}
