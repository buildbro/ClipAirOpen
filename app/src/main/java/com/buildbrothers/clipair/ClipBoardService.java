package com.buildbrothers.clipair;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import model.History;
import model.User;

import static com.buildbrothers.clipair.MainActivity.TEMP_UID_KEY;

public class ClipBoardService extends Service {
    private ClipboardManager mClipBoardManager;
    IBinder mBinder;
    int mStartMode;

    private FirebaseDatabase mDatabase;
    private User user;

    private SharedPreferences preferences;

    private String copiedText;
    private String userId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mClipBoardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        mClipBoardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                copiedText = mClipBoardManager.getPrimaryClip().getItemAt(0)
                        .coerceToText(getApplicationContext()).toString();
                mDatabase = FirebaseDatabase.getInstance();
                preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                String tempUid = preferences.getString(TEMP_UID_KEY, "");
                if (!tempUid.equalsIgnoreCase("")) {
                    //TODO: submit for GUEST
                    Toast.makeText(getApplicationContext(), tempUid, Toast.LENGTH_LONG).show();
                    userId = tempUid;
                    insertClipItem();
                } else {
                    tempRegistration();
                }


                Toast.makeText(getApplicationContext(), copiedText, Toast.LENGTH_LONG).show();
            }
        });

        return mStartMode;
    }

    private void tempRegistration() {
        final DatabaseReference myRef = mDatabase.getReference("users").push();
        user = new User(false, null);
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
    }

    private void insertClipItem() {
        if (userId != null) {
            History history = new History("11", copiedText, "Just now", false);
            DatabaseReference myRef = mDatabase.getReference("users/" + userId + "/clips").push();
            myRef.setValue(history).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "New Clip Item added!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}