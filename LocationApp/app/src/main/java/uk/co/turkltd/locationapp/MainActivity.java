package uk.co.turkltd.locationapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.security.crypto.MasterKeys;
import androidx.security.crypto.EncryptedSharedPreferences;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;
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

    //Encryption key
    String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

    //
    EncryptedSharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    //Variables
    String serverURL, savedURL;
    boolean switchState, buttonState;

    //Widgets
    EditText serverInput;
    TextView displayURL;
    Button submitButton;
    ToggleButton locationButton;
    Switch consentSwitch;

    public MainActivity() throws GeneralSecurityException, IOException {
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

            editor = sharedPreferences.edit();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        displayURL = findViewById(R.id.displayURL);
        submitButton = findViewById(R.id.submitButton);
        serverInput = findViewById(R.id.serverInput);
        consentSwitch = findViewById(R.id.consentSwitch);
        locationButton = findViewById(R.id.locationButton);

        //Set displayURL to what is saved in shared preferences
        savedURL = sharedPreferences.getString("URL", "");
        displayURL.setText(savedURL);

        //Submit button to save server URL to shared preferences as well as display result in displayURL
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                serverURL = serverInput.getText().toString();

                editor.putString("URL", serverURL);
                editor.apply();

                savedURL = sharedPreferences.getString("URL", "");
                displayURL = findViewById(R.id.displayURL);
                displayURL.setText(savedURL);
            }
        });

        //Check state of switch from shared preferences and apply required settings.
        switchState = sharedPreferences.getBoolean("SWITCH", false);
        consentSwitch.setChecked(switchState);
        if (switchState = true){
            locationButton.setVisibility(View.VISIBLE);
        }

        //Check state of toggle button from shared preferences and apply required settings.
        buttonState = sharedPreferences.getBoolean("BUTTON", false);
        locationButton.setChecked(buttonState);

        //Switch to give make sure users have to give consent to send location
        consentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    locationButton.setVisibility(View.VISIBLE);
                    editor.putBoolean("SWITCH", true);
                    editor.apply();

                } else {
                   locationButton.setVisibility(View.INVISIBLE);
                   locationButton.setChecked(false);
                   editor.putBoolean("SWITCH", false);
                   editor.apply();
                }
            }
        });

        //Button to allow application to send location data to desired URL
        locationButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){


                    editor.putBoolean("BUTTON", true);
                    editor.apply();



                } else {
                    editor.putBoolean("BUTTON", false);
                    editor.apply();

                }
            }
        });
    }
}