package com.example.zeiterfassung;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends AppCompatActivity {

    private AppDatabase db;
    private static final String ADMIN_PASSWORD = "2002";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "worktime-db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries() // Für Entwicklungszwecke – in Produktion asynchron!
                .build();

        final EditText etName = findViewById(R.id.etName);
        final EditText etPin = findViewById(R.id.etPin);
        // PIN als Passwortfeld (maskiert) konfigurieren
        etPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String pin = etPin.getText().toString().trim();
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(pin)) {
                Toast.makeText(LoginActivity.this, "Bitte Name und PIN eingeben", Toast.LENGTH_SHORT).show();
                return;
            }
            String pinHash = hashPin(pin);
            User user = db.userDao().getUserByName(name);
            if (user == null) {
                // Benutzer existiert nicht – Admin-Passwort abfragen
                final EditText etAdmin = new EditText(LoginActivity.this);
                etAdmin.setTransformationMethod(PasswordTransformationMethod.getInstance());
                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("Admin-Passwort erforderlich")
                        .setMessage("Dieser Benutzer existiert nicht. Bitte Admin-Passwort eingeben , um einen neuen Benutzer anzulegen.")
                        .setView(etAdmin)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String adminInput = etAdmin.getText().toString().trim();
                                if (ADMIN_PASSWORD.equals(adminInput)) {
                                    User newUser = new User(name, pinHash);
                                    db.userDao().insert(newUser);
                                    Toast.makeText(LoginActivity.this, "Neuer Benutzer angelegt", Toast.LENGTH_SHORT).show();
                                    launchMain(newUser);
                                } else {
                                    Toast.makeText(LoginActivity.this, "Falsches Admin-Passwort", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Abbrechen", null)
                        .show();
            } else {
                if (!user.pinHash.equals(pinHash)) {
                    Toast.makeText(LoginActivity.this, "Falscher PIN", Toast.LENGTH_SHORT).show();
                    return;
                }
                launchMain(user);
            }
        });
    }

    private void launchMain(User user) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("userId", user.id);
        intent.putExtra("userName", user.name);
        startActivity(intent);
        finish();
    }

    private String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
}
