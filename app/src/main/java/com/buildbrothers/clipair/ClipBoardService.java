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

import java.security.SecureRandom;

import model.History;
import model.User;
import utils.PairCodeGenerator;

import static utils.Constants.TEMP_UID_KEY;


public class ClipBoardService extends Service {
    private ClipboardManager mClipBoardManager;
    IBinder mBinder;
    int mStartMode;

    private FirebaseDatabase mDatabase;

    private SharedPreferences preferences;

    private String copiedText;
    private String userId;
    private ClipboardManager.OnPrimaryClipChangedListener clipChangeListener;

    private boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mClipBoardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        if (clipChangeListener == null) {
            clipChangeListener = new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    String clipLabel = "default";
                    if (mClipBoardManager.getPrimaryClip().getDescription().getLabel() != null) {
                        clipLabel = mClipBoardManager.getPrimaryClip().getDescription().getLabel().toString();
                    }
                    if (!clipLabel.equals("auto_copy_text")) {
                        copiedText = mClipBoardManager.getPrimaryClip().getItemAt(0)
                                .coerceToText(getApplicationContext()).toString();
                        mDatabase = FirebaseDatabase.getInstance();
                        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                        String tempUid = preferences.getString(TEMP_UID_KEY, "");
                        if (!tempUid.equalsIgnoreCase("")) {
                            Toast.makeText(getApplicationContext(), tempUid, Toast.LENGTH_LONG).show();
                            userId = tempUid;
                            insertClipItem();
                        } else {
                            Toast.makeText(getApplicationContext(), "Oops! Something went wrong.", Toast.LENGTH_LONG).show();
                        }
                    }

                }
            };
        }

        if (!isRunning) {
            if (mClipBoardManager != null) {
                mClipBoardManager.addPrimaryClipChangedListener(clipChangeListener);
            }
            isRunning = true;
        }

        return mStartMode;
    }

    private void insertClipItem() {
        if (userId != null) {
            History history = new History(null, copiedText, false);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (clipChangeListener != null) {
            mClipBoardManager.removePrimaryClipChangedListener(clipChangeListener);
            clipChangeListener = null;
        }
    }
}
