package biel.com.gpstest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity {
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private static final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 1; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATE = 1000; // in Milliseconds

    private static final long POINT_RADIUS = 1000; // in Meters
    private static final long PROX_ALERT_EXPIRATION = -1;

    private static final String POINT_LATITUDE_KEY = "POINT_LATITUDE_KEY";
    private static final String POINT_LONGITUDE_KEY = "POINT_LONGITUDE_KEY";

    private static final String PROX_ALERT_INTENT =
            "biel.com.gpstest.ProximityAlert";

    private static final NumberFormat nf = new DecimalFormat("##.########");

    private LocationManager locationManager;

    private EditText latitudeEditText;
    private EditText longitudeEditText;
    private Button findCoordinatesButton;
    private Button savePointButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MINIMUM_TIME_BETWEEN_UPDATE,
                MINIMUM_DISTANCECHANGE_FOR_UPDATE,
                new MyLocationListener()
        );

        latitudeEditText = (EditText) findViewById(R.id.txtLat);
        longitudeEditText = (EditText) findViewById(R.id.txtLong);
        findCoordinatesButton = (Button) findViewById(R.id.btnPopulateFromLastKnown);
        savePointButton = (Button) findViewById(R.id.btnSet);

        findCoordinatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                populateCoordinatesFromLastKnownLocation();
            }
        });

        savePointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProximityAlertPoint();
            }
        });
    }

    private void saveProximityAlertPoint() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }
        Location location =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            Toast.makeText(this, "No last known location. Aborting...",
                    Toast.LENGTH_LONG).show();
            return;
        }
        saveCoordinatesInPreferences((float) location.getLatitude(),
                (float) location.getLongitude());
        addProximityAlert(location.getLatitude(), location.getLongitude());
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }

    private void addProximityAlert(double latitude, double longitude) {

        Intent intent = new Intent(PROX_ALERT_INTENT);
        PendingIntent proximityIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }
        locationManager.addProximityAlert(
                latitude, // the latitude of the central point of the alert region
                longitude, // the longitude of the central point of the alert region
                POINT_RADIUS, // the radius of the central point of the alert region, in meters
                PROX_ALERT_EXPIRATION, // time for this proximity alert, in milliseconds, or -1 to indicate no expiration
                proximityIntent // will be used to generate an Intent to fire when entry to or exit from the alert region is detected
        );

        IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
        registerReceiver(new ProximityIntentReceiver(), filter);

    }

    private void populateCoordinatesFromLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }
        Location location =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location!=null) {
            latitudeEditText.setText(nf.format(location.getLatitude()));
            longitudeEditText.setText(nf.format(location.getLongitude()));
        }
    }

    private void saveCoordinatesInPreferences(float latitude, float longitude) {
        SharedPreferences prefs =
                this.getSharedPreferences(getClass().getSimpleName(),
                        Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putFloat(POINT_LATITUDE_KEY, latitude);
        prefsEditor.putFloat(POINT_LONGITUDE_KEY, longitude);
        prefsEditor.commit();
    }

    private Location retrievelocationFromPreferences() {
        SharedPreferences prefs =
                this.getSharedPreferences(getClass().getSimpleName(),
                        Context.MODE_PRIVATE);
        Location location = new Location("POINT_LOCATION");
        location.setLatitude(prefs.getFloat(POINT_LATITUDE_KEY, 0));
        location.setLongitude(prefs.getFloat(POINT_LONGITUDE_KEY, 0));
        return location;
    }


    public class MyLocationListener implements LocationListener {
        public void onLocationChanged(Location location) {
            Location pointLocation = retrievelocationFromPreferences();
            float distance = location.distanceTo(pointLocation);
            if(distance < 500){
                Toast.makeText(MainActivity.this,
                        "Distance from Point:"+distance, Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(MainActivity.this,
                        "Distance from Point:"+distance, Toast.LENGTH_LONG).show();
            }

        }
        public void onStatusChanged(String s, int i, Bundle b) {
            Log.i("Main", s);
        }
        public void onProviderDisabled(String s) {
        }
        public void onProviderEnabled(String s) {
        }
    }
}
