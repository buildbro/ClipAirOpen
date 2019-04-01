package com.buildbrothers.clipair;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.User;

import static com.buildbrothers.clipair.MainActivity.PERM_PAIR_CODE;
import static com.buildbrothers.clipair.MainActivity.TEMP_UID_KEY;
import static com.buildbrothers.clipair.WelcomeActivity.ORIGIN_CODE_NAME;

public class AccountActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference userDbRef;
    private TextView signOutBtn;
    private TextView currentUserTextView;

    private SharedPreferences mPreference;
    private String userId;
    private String userEmail;

    private int originCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.setTitle("User Account");

        signOutBtn = findViewById(R.id.sign_out_btn);
        signOutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        Intent intent = getIntent();
        if (intent != null) {
            originCode = intent.getIntExtra(ORIGIN_CODE_NAME, 0);
        }

        mFirebaseAuth = FirebaseAuth.getInstance();

        mPreference = PreferenceManager.getDefaultSharedPreferences(this);
        userId = mPreference.getString(TEMP_UID_KEY, "");

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        userDbRef = mFirebaseDatabase.getReference("users");

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    //this user is signed in do your stuff
                    updateUserDetailsUI(user);
                    userEmail = user.getEmail();

                } else {
                    createSignInIntent();
                }
            }
        };

    }

    private void updateUserDetailsUI(FirebaseUser user) {
        currentUserTextView = findViewById(R.id.user_email);
        currentUserTextView.setText(user.getDisplayName() + " - " + user.getEmail());
    }

    public void createSignInIntent() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.EmailBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .build(),
                RC_SIGN_IN
        );
    }

    public void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                activateUser(userEmail);
                Toast.makeText(getApplicationContext(), "Sign in Successful!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Sign in Canceled!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void activateUser(final String email) {
        FirebaseInstanceId.getInstance()
                .getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    return;
                }

                String firebaseToken = task.getResult().getToken();
                SharedPreferences.Editor editor = mPreference.edit();
                editor.putString(PERM_PAIR_CODE, firebaseToken);
                editor.putBoolean("firstRun", false);
                editor.apply();
            }
        });

        Query userCheckQuery = mFirebaseDatabase.getReference().child("users")
                .orderByChild("userId").equalTo(email);
        userCheckQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() > 0) {
                    String userPushKey = dataSnapshot.getChildren().iterator().next().getKey();
                    SharedPreferences.Editor editor = mPreference.edit();
                    editor.putString(TEMP_UID_KEY, userPushKey);
                    editor.apply();

                    //TODO Update local pref userID to the one for this item
                } else {
                    //check if temporal account already exists
                    if (!userId.equals("")) {
                        //TODO update this user
                        Map<String, Object> userUpdateMap = new HashMap<>();
                        userUpdateMap.put("userId", email);
                        userUpdateMap.put("registered", true);
                        userDbRef.child(userId).updateChildren(userUpdateMap);

                    } else {
                        //register new user
                        String permPairCode = mPreference.getString(PERM_PAIR_CODE, "");

                        final DatabaseReference newUserDbRef = mFirebaseDatabase.getReference("users").push();
                        User user = new User(true, email, null, permPairCode);
                        newUserDbRef.setValue(user);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if (originCode == 1) {
            Intent homeIntent = new Intent(AccountActivity.this, MainActivity.class);
            startActivity(homeIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }
}
