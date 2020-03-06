package uk.co.turkltd.locationapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.MasterKeys;
import androidx.security.crypto.EncryptedSharedPreferences;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {

    String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

    EncryptedSharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    String serverURL, savedURL;
    EditText serverInput;
    TextView displayURL;
    Button submitButton;
    Button locationButton;

    public MainActivity() throws GeneralSecurityException, IOException {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
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

        savedURL = sharedPreferences.getString("URL", "");

        displayURL = findViewById(R.id.displayURL);
        displayURL.setText(savedURL);


        serverInput = findViewById(R.id.serverInput);

        submitButton = findViewById(R.id.submitButton);
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

        locationButton = findViewById(R.id.locationButton);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){

            }
        });
    }
}