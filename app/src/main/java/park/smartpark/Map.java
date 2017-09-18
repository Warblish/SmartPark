package park.smartpark;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import android.os.AsyncTask;
import android.util.*;

import org.json.*;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class Map extends FragmentActivity implements OnMapReadyCallback {

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
