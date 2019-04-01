package com.buildbrothers.clipair;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import model.User;

import static com.buildbrothers.clipair.MainActivity.PERM_PAIR_CODE;
import static com.buildbrothers.clipair.MainActivity.TEMP_UID_KEY;

public class WelcomeActivity extends AppCompatActivity {

    public static final String ORIGIN_CODE_NAME = "origin";
    public static final int ORIGIN_CODE_WELCOME = 1; //1 = WelcomeActivity, 2 = MainActivity
    private SharedPreferences mPreferences;
    private FirebaseDatabase mFirebaseDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mFirebaseDatabase = FirebaseDatabase.getInstance();
    }

    public void startAsGuest(View v) {
        FirebaseInstanceId.getInstance()
                .getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    return;
                }

                String firebaseToken = task.getResult().getToken();
                tempRegistration(firebaseToken);
            }
        });

    }

    public void startAsUser(View view) {
        Intent accountIntent = new Intent(WelcomeActivity.this, AccountActivity.class);
        accountIntent.putExtra(ORIGIN_CODE_NAME, ORIGIN_CODE_WELCOME);
        startActivity(accountIntent);
        finish(); //TODO this is temporal
    }


    private void tempRegistration(final String permPairCode) {
        final DatabaseReference myRef = mFirebaseDatabase.getReference("users").push();
        User user = new User(false, null, null, permPairCode);
        myRef.setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putString(TEMP_UID_KEY, myRef.getKey());
                    editor.putString(PERM_PAIR_CODE, permPairCode);
                    editor.putBoolean("firstRun", false);
                    editor.apply();
                    Toast.makeText(getApplicationContext(), "ClipAir temp user created!", Toast.LENGTH_LONG).show();
                    Intent homeIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(homeIntent);
                    finish();
                }
            }
        });
    }
}
