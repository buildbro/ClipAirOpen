package com.buildbrothers.clipair;

import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import model.Device;
import model.Pairing;
import model.User;
import utils.PairCodeGenerator;

import static com.buildbrothers.clipair.MainActivity.PERM_PAIR_CODE;
import static com.buildbrothers.clipair.MainActivity.TEMP_UID_KEY;

public class ScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference devicesRef;
    private ZXingScannerView mScannerView;
    private SharedPreferences sharedPreferences;
    private String userId;
    private String userPermPairCode;

    private boolean notPaired;
    private String senderPermPairCode;
    private String deviceName;
    private String senderDeviceName;

    private ValueEventListener pairingItemListener;
    private ValueEventListener verifyPairCodeItemListener;
    private Query mQuery;
    private Query verifyCodeQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        devicesRef = mFirebaseDatabase.getReference("pairedDevices");
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        userId = sharedPreferences.getString(TEMP_UID_KEY, "");
        userPermPairCode = sharedPreferences.getString(PERM_PAIR_CODE, "");

        notPaired = true;
    }

    @Override
    public void handleResult(Result rawResult) {
        String pairCode = rawResult.getText();

        verifyPairCode(pairCode);
    }

    private void verifyPairCode(final String pairCode) {
        verifyPairCodeItemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.getChildrenCount() > 0) {
                    User user = dataSnapshot.getChildren().iterator().next().getValue(User.class);
                    //Toast.makeText(getApplicationContext(), userId, Toast.LENGTH_LONG).show();
                    deviceName = Build.MODEL;
                    senderPermPairCode = user.getPermPairCode();
                    senderDeviceName = user.getDeviceName();
                    DatabaseReference pairingRef = mFirebaseDatabase.getReference("isPairing").push();
                    Pairing pairing = new Pairing(pairCode, userPermPairCode, deviceName);
                    pairingRef.setValue(pairing).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            startPairing(pairCode);
                        }
                    });

                } else {
                    Toast.makeText(getApplicationContext(), "Invalid pair code!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        verifyCodeQuery = mFirebaseDatabase.getReference().child("users")
                .orderByChild("pairCode").equalTo(pairCode);
        verifyCodeQuery.addValueEventListener(verifyPairCodeItemListener);
    }

    private void detachVerifyCodeItemListener() {
        if (verifyPairCodeItemListener != null) {
            verifyCodeQuery.removeEventListener(verifyPairCodeItemListener);
            verifyPairCodeItemListener = null;
        }
    }

    private void  startPairing(final String code) {
        pairingItemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() > 0) {
                    Device device = dataSnapshot.getChildren().iterator().next().getValue(Device.class);
                    if (notPaired) {
                        notPaired = false;
                        DatabaseReference pairRef = mFirebaseDatabase.getReference("pairedDevices").push();
                        Device pairDevice = new Device(userId, senderPermPairCode, code, deviceName, senderDeviceName, true);
                        pairRef.setValue(pairDevice).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()){
                                    Toast.makeText(getApplicationContext(), "Pairing 100% done", Toast.LENGTH_LONG).show();
                                    //TODO Delete pairing from Firebase
                                    //TODO Close this screen
                                    finish();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        mQuery = mFirebaseDatabase.getReference().child("pairedDevices")
                .orderByChild("pairCode").equalTo(code);
        mQuery.addValueEventListener(pairingItemListener);
    }

    private void detachPairingItemListener() {
        if (pairingItemListener != null) {
            mQuery.removeEventListener(pairingItemListener);
        }
    }

    /*private void tempRegistration() {
        final DatabaseReference myRef = mDatabase.getReference("users").push();
        PairCodeGenerator permPairCode = new PairCodeGenerator()
        user = new User(false, null, null);
        myRef.setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(TEMP_UID_KEY, myRef.getKey());
                    editor.apply();
                    Toast.makeText(getApplicationContext(), "ClipAir temp user created!", Toast.LENGTH_LONG).show();
                    userId = myRef.getKey();
                    insertClipItem();
                }
            }
        });
    } */

    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
        detachPairingItemListener();
        detachVerifyCodeItemListener();
    }
}
