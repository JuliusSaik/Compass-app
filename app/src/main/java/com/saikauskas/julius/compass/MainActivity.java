package com.saikauskas.julius.compass;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.FloatMath;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.util.Half.EPSILON;
import static android.util.Half.floor;
import static android.util.Half.toHexString;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    ImageView ivCompass;
    TextView tvDirection, tvLocation;
    ProgressBar progressBar;
    ImageView bttnUpdateLocation;

    private SensorManager sensorManager;
    private ResultReceiver resultReceiver;

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];

    float azimuth = 0f;
    private float correctAzimuth = 0f;

    public static final int REQUEST_CODE_LOCATION_PERMISSION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivCompass = findViewById(R.id.ivCompass);
        tvDirection = findViewById(R.id.tvDirection);
        tvLocation = findViewById(R.id.tvLocation);
        progressBar = findViewById(R.id.progressBar);
        bttnUpdateLocation = findViewById(R.id.ivBttnUpdateLoc);


        //resultReceiver = new AdressResultReceiver(new Handler());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


        /*
        bttnUpdateLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_LOCATION_PERMISSION);
                } else {
                    getCurrentLocation();
                }

            }
        });

         */

    }

    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Can't get location, permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

     */

    /*
    public void getCurrentLocation(){

        progressBar.setVisibility(View.VISIBLE);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        LocationServices.getFusedLocationProviderClient(MainActivity.this).requestLocationUpdates(locationRequest,
                new LocationCallback(){

                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);

                        LocationServices.getFusedLocationProviderClient(MainActivity.this).removeLocationUpdates(this);
                        if (locationResult != null && locationResult.getLocations().size() > 0){
                            int latestLocationIndex = locationResult.getLocations().size() - 1;

                            double latidude =
                                    locationResult.getLocations().get(latestLocationIndex).getLatitude();

                            double longitude =
                                    locationResult.getLocations().get(latestLocationIndex).getLongitude();


                            Location location = new Location("providerNA");
                            location.setLatitude(latidude);
                            location.setLongitude(longitude);

                            fetchAddressFromLatLong(location);
                        }

                        else{progressBar.setVisibility(View.GONE);}


                    }
                },Looper.getMainLooper());
                }*/


    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        final float alpha = 0.97f;

        synchronized (this){
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                mGeomagnetic[0] = alpha * mGeomagnetic[0]+(1-alpha) * event.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1]+(1-alpha) * event.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2]+(1-alpha) * event.values[2];

            }

            else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                mGravity[0] = alpha * mGravity[0]+(1-alpha) * event.values[0];
                mGravity[1] = alpha * mGravity[1]+(1-alpha) * event.values[1];
                mGravity[2] = alpha * mGravity[2]+(1-alpha) * event.values[2];
            }

            float R[] = new float[9];
            float I[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

            if (success){

                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                azimuth = (float)Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360)%360;

                String stringAzimuth = String.valueOf(Math.round(azimuth));
                String bearingText = "North";

                Animation animation = new RotateAnimation(-correctAzimuth, -azimuth, Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                correctAzimuth = azimuth;

                animation.setDuration(1000);
                animation.setInterpolator(new LinearInterpolator());
                animation.setRepeatCount(Animation.INFINITE);
                animation.setFillAfter(true);

                ivCompass.startAnimation(animation);

                if ( (360 >= azimuth && azimuth >= 337.5) || (0 <= azimuth && azimuth <= 22.5) ) bearingText = "North";
                else if (azimuth > 22.5 && azimuth < 67.5) bearingText = "North East";
                else if (azimuth >= 67.5 && azimuth <= 112.5) bearingText = "East";
                else if (azimuth > 112.5 && azimuth < 157.5) bearingText = "South Eeast";
                else if (azimuth >= 157.5 && azimuth <= 202.5) bearingText = "South";
                else if (azimuth > 202.5 && azimuth < 247.5) bearingText = "South West";
                else if (azimuth >= 247.5 && azimuth <= 292.5) bearingText = "West";
                else if (azimuth > 292.5 && azimuth < 337.5) bearingText = "North West";

                tvDirection.setText(stringAzimuth + "\u00B0" + " " + bearingText);
            }

        }

    }

    /*
    private void fetchAddressFromLatLong(Location location){
        Intent intent = new Intent(MainActivity.this, FetchAdressIntentService.class);
        intent.putExtra(Constants.RECEIVER, resultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        startService(intent);
    }

     */

    /*
    public class AdressResultReceiver extends ResultReceiver{

        AdressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);

            if (resultCode == Constants.SUCCESS_RESULT){
                tvLocation.setText(resultData.getString(Constants.RESULT_DATA_KEY));
            } else {
                Toast.makeText(MainActivity.this, resultData.getString(Constants.RESULT_DATA_KEY), Toast.LENGTH_SHORT).show();
            }
            progressBar.setVisibility(View.GONE);
        }
    }
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
