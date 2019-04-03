package com.buildbrothers.clipair;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import model.User;

import static utils.Constants.DB_PATH_USERS;
import static utils.Constants.FIRST_RUN_KEY;
import static utils.Constants.ORIGIN_CODE_KEY;
import static utils.Constants.ORIGIN_CODE_VALUE_WELCOME;
import static utils.Constants.PERM_PAIR_CODE;
import static utils.Constants.TEMP_UID_KEY;

public class WelcomeActivity extends AppCompatActivity {

    private SharedPreferences mPreferences;
    private FirebaseDatabase mFirebaseDatabase;
    private TextView guestSignInLink;
    private ProgressBar signInProgressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mFirebaseDatabase = FirebaseDatabase.getInstance();

        initializeUI();
    }

    private void initializeUI() {
        guestSignInLink = findViewById(R.id.sign_in_guest);
        signInProgressBar = findViewById(R.id.sign_in_pb);
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
        accountIntent.putExtra(ORIGIN_CODE_KEY, ORIGIN_CODE_VALUE_WELCOME);
        startActivity(accountIntent);
        finish();
    }


    private void tempRegistration(final String permPairCode) {
        guestSignInLink.setVisibility(View.GONE);
        signInProgressBar.setVisibility(View.VISIBLE);

        final DatabaseReference myRef = mFirebaseDatabase.getReference(DB_PATH_USERS).push();
        User user = new User(false, null, null, permPairCode);
        myRef.setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    guestSignInLink.setVisibility(View.VISIBLE);
                    signInProgressBar.setVisibility(View.GONE);
                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putString(TEMP_UID_KEY, myRef.getKey());
                    editor.putString(PERM_PAIR_CODE, permPairCode);
                    editor.putBoolean(FIRST_RUN_KEY, false);
                    editor.apply();
                    Toast.makeText(getApplicationContext(), R.string.temp_user_created, Toast.LENGTH_LONG).show();
                    Intent homeIntent = new Intent(WelcomeActivity.this, MainActivity.class);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                } else {
                    guestSignInLink.setVisibility(View.VISIBLE);
                    signInProgressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}
