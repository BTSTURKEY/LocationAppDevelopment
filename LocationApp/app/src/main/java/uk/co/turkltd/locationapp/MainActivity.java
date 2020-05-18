package uk.co.turkltd.locationapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.MasterKeys;
import androidx.security.crypto.EncryptedSharedPreferences;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    //Objects
    EncryptedSharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationManager locationManager;
    RequestQueue queue;
    StringRequest stringRequest;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    //Variables
    String masterKeyAlias, savedURL, latitudeString, longitudeString, timeStampString;
    String unique_ID = null;
    boolean switchState, buttonState, gps, network;
    long timeStamp;
    double latitude = 0.0, longitude = 0.0;

    //Widgets
    EditText serverInput;
    TextView displayURL, currentValue;
    Button submitButton;
    ToggleButton locationButton;
    Switch consentSwitch;

    public MainActivity() {
        try {
            masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            //Encrypt data being sent to shared preferences located in
            // data/data/uk.co.turkltd.locationapp/shared_prefs/settingsLM.xml
            sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    "settingsLM",
                    masterKeyAlias,
                    getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        //Initialise objects
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000); // 10 seconds
        locationRequest.setFastestInterval(5 * 1000); // 5 seconds

        editor = sharedPreferences.edit();
        editor.apply();

        //Initialise widgets
        displayURL = findViewById(R.id.displayURL);
        submitButton = findViewById(R.id.submitButton);
        serverInput = findViewById(R.id.serverInput);
        consentSwitch = findViewById(R.id.consentSwitch);
        currentValue = findViewById(R.id.currentValue);
        locationButton = findViewById(R.id.locationButton);

        //Check and set UUID
        if (unique_ID == null) {
            unique_ID = sharedPreferences.getString("unique_ID", null);
            if (unique_ID == null) {
                unique_ID = UUID.randomUUID().toString();
                editor.putString("unique_ID", unique_ID);
                editor.apply();
            }
        }

        //Checks which states the button and switches much be in
        checkStates();

        //Submit button to save server URL to shared preferences as well as display result in
        // displayURL
        submitButton.setOnClickListener(v -> saveURL());

        //Switch to make sure users have to give consent to send location
        consentSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                consentOn();
            } else {
                consentOff();
            }
        });

        //Button to allow application to send location data to desired URL
        locationButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                //sendCurrentLocation();
                sendLocation();
            } else {
                stopLocation();
            }
        });
    }

    public void checkStates() {
        //Set displayURL to what is saved in shared preferences
        savedURL = sharedPreferences.getString("URL", "");
        displayURL.setText(savedURL);
        //Check state of switch from shared preferences and apply required settings.
        switchState = sharedPreferences.getBoolean("SWITCH", false);
        consentSwitch.setChecked(switchState);
        if (switchState = true) {
            locationButton.setVisibility(View.VISIBLE);
        }
        //Check state of toggle button from shared preferences and apply required settings.
        buttonState = sharedPreferences.getBoolean("BUTTON", false);
        locationButton.setChecked(buttonState);
        if (locationButton.isChecked()) {
            sendLocation();
        }
    }

    public void saveURL() {
        savedURL = serverInput.getText().toString();
        editor.putString("URL", savedURL);
        editor.apply();
        displayURL.setText(savedURL);
    }

    public void alertUser() {
        new AlertDialog.Builder(this)
                .setTitle("Location settings")
                .setMessage("Location services are deactivated, please enable them to use the " +
                        "application")
                .setPositiveButton("Location settings", (dialog, which) -> {
                    Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    dialog.dismiss();
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void consentOn() {
        gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gps || !network) {
            alertUser();
            consentSwitch.setChecked(false);
        } else if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }, 1000);
            consentSwitch.setChecked(false);
        } else {
            locationButton.setVisibility(View.VISIBLE);
            editor.putBoolean("SWITCH", true);
            editor.apply();
        }
    }

    public void consentOff() {
        consentSwitch.setChecked(false);
        locationButton.setVisibility(View.INVISIBLE);
        locationButton.setChecked(false);
        editor.putBoolean("SWITCH", false);
        editor.putBoolean("BUTTON", false);
        editor.apply();
        stopLocation();
    }

    private void sendLocation() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    longitude = location.getLongitude();
                    latitude = location.getLatitude();
                    timeStamp = location.getTime();
                    Date d = new Date(timeStamp);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    currentValue.setText((longitude + " " + latitude));
                    latitudeString = Double.toString(latitude);
                    longitudeString = Double.toString(longitude);
                    timeStampString = df.format(d);
                    editor.putBoolean("BUTTON", true);
                    editor.apply();
                    sendPostRequest();
                }
            }
        };
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        startService();
    }

    private void stopLocation() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        locationButton.setChecked(false);
        editor.putBoolean("BUTTON", false);
        editor.apply();
        cancelPostRequest();
        stopService();
    }

    private void sendPostRequest() {
        queue = Volley.newRequestQueue(this);
        savedURL = sharedPreferences.getString("URL", "");
        stringRequest = new StringRequest(Request.Method.POST, savedURL,
                response -> Log.e("HttpClient", "success! response: " + response),
                error -> Log.e("HttpClient", "error: " + error.toString())) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("UUID", unique_ID);
                params.put("latitude", latitudeString);
                params.put("longitude", longitudeString);
                params.put("time_stamp", timeStampString);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        queue.add(stringRequest);
    }

    private void cancelPostRequest() {
        stringRequest.cancel();
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.putExtra("inputExtra", "Location Service");
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
    }
}
