package uk.co.turkltd.locationapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.MasterKeys;
import androidx.security.crypto.EncryptedSharedPreferences;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Criteria;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {
    //Objects
    EncryptedSharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    LocationManager locationManager;

    //Variables
    String masterKeyAlias, serverURL, savedURL;
    boolean switchState, buttonState;
    double longitude, latitude;

    //Widgets
    EditText serverInput;
    TextView displayURL, longitudeValue, latitudeValue;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            //Encrypt data being sent to shared preferences, located in data/data/uk.co.turkltd.locationapp/shared_prefs/serverURL.xml
            sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    "serverURL",
                    masterKeyAlias,
                    getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );


        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        editor = sharedPreferences.edit();
        editor.apply();

        displayURL = findViewById(R.id.displayURL);
        submitButton = findViewById(R.id.submitButton);
        serverInput = findViewById(R.id.serverInput);
        consentSwitch = findViewById(R.id.consentSwitch);
        longitudeValue = findViewById(R.id.longitudeValue);
        latitudeValue = findViewById(R.id.latitudeValue);
        locationButton = findViewById(R.id.locationButton);

        checkStates();

        //Submit button to save server URL to shared preferences as well as display result in displayURL
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveURL();
            }
        });

        //Switch to make sure users have to give consent to send location
        consentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    consentOn();
                } else {
                    consentOff();
                }
            }
        });

        //Button to allow application to send location data to desired URL
        locationButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sendLocation();
                } else {
                    noSendLocation();
                }
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
    }

    public void saveURL() {
        serverURL = serverInput.getText().toString();
        editor.putString("URL", serverURL);
        editor.apply();
        savedURL = sharedPreferences.getString("URL", "");
        displayURL = findViewById(R.id.displayURL);
        displayURL.setText(savedURL);
    }

    public void alertUser() {
        new AlertDialog.Builder(this)
                .setTitle("Location settings")
                .setMessage("Location services are deactivated, please enable them to use the application")
                .setPositiveButton("Location settings", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        dialog.dismiss();
                        startActivity(i);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void consentOn() {
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gps || !network) {
            alertUser();
            consentSwitch.setChecked(false);
        } else if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
            consentSwitch.setChecked(false);
        } else {
            locationButton.setVisibility(View.VISIBLE);
            editor.putBoolean("SWITCH", true);
            editor.apply();
        }
    }

    public void consentOff() {
        locationButton.setVisibility(View.INVISIBLE);
        locationButton.setChecked(false);
        editor.putBoolean("SWITCH", false);
        editor.putBoolean("BUTTON", false);
        editor.apply();
    }

    private void sendLocation() {
        Log.v("method", "1");
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);
        Log.v("method", "2");
        if (provider != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(provider, 0, 0, receiveLocation);
                editor.putBoolean("BUTTON", true);
                editor.apply();
                Log.v("method", "3");
            }
        }
    }

    private final LocationListener receiveLocation = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();

            String lat = Double.toString(latitude);
            String lon = Double.toString(longitude);

            latitudeValue.setText(lat);
            longitudeValue.setText(lon);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private void noSendLocation() {
        editor.putBoolean("BUTTON", false);
        editor.apply();
    }
}