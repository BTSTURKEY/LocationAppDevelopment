package uk.co.turkltd.locationapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.security.crypto.MasterKeys;
import androidx.security.crypto.EncryptedSharedPreferences;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {
    //Objects
    EncryptedSharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationManager locationManager;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    //Variables
    String masterKeyAlias, serverURL, savedURL;
    boolean switchState, buttonState, locationPermission;
    long timeStamp;
    double latitude, longitude;

    //Widgets
    EditText serverInput;
    TextView displayURL, currentValue, updatedValue;
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
            //Encrypt data being sent to shared preferences located in
            // data/data/uk.co.turkltd.locationapp/shared_prefs/serverURL.xml
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

        //Initialise objects
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        editor = sharedPreferences.edit();
        editor.apply();

        displayURL = findViewById(R.id.displayURL);
        submitButton = findViewById(R.id.submitButton);
        serverInput = findViewById(R.id.serverInput);
        consentSwitch = findViewById(R.id.consentSwitch);
        currentValue = findViewById(R.id.currentValue);
        updatedValue = findViewById(R.id.updatedValue);
        locationButton = findViewById(R.id.locationButton);


        //Checks which states the button and switches much be in
        checkStates();

        //Submit button to save server URL to shared preferences as well as display result in
        // displayURL
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
                    sendCurrentLocation();
                    sendLocationUpdates();
                } else {
                    stopLocation();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationPermission){
            sendLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationPermission){
            sendLocationUpdates();
        }
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
        displayURL = findViewById(R.id.displayURL);
        displayURL.setText(serverURL);
    }

    public void alertUser() {
        new AlertDialog.Builder(this)
                .setTitle("Location settings")
                .setMessage("Location services are deactivated, please enable them to use the " +
                        "application")
                .setPositiveButton("Location settings", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
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
        }
        else if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION},1000);
            consentSwitch.setChecked(false);
        }
        else {
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

    private void sendCurrentLocation(){
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            longitude = location.getLongitude();
                            latitude = location.getLatitude();
                            timeStamp = location.getTime();
                            currentValue.setText(String.format(String.valueOf(longitude), latitude));
                            locationPermission = true;
                            editor.putBoolean("BUTTON", true);
                            editor.apply();
                        }
                        else {
                            locationButton.setChecked(false);
                            alertUser();
                        }
                    }
                });
    }

    private void sendLocationUpdates() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        timeStamp = location.getTime();
                        updatedValue.setText(String.format(String.valueOf(longitude), latitude));
                    }
                }
            }
        };
    }

    private void stopLocation() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        locationPermission = false;
        editor.putBoolean("BUTTON", false);
        editor.apply();
    }
}