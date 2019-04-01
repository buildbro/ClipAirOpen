package com.buildbrothers.clipair;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import model.History;

import static com.buildbrothers.clipair.MainActivity.TEMP_UID_KEY;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_UTILS = "Utility";
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDbRef;

    private SharedPreferences preferences;

    private ClipboardManager mClipboardManager;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        //check for the data/notification entry from the payload

        mFirebaseDatabase = FirebaseDatabase.getInstance();

        if (remoteMessage != null) {

            preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String userId = preferences.getString(TEMP_UID_KEY, "");

            mDbRef = mFirebaseDatabase.getReference("users/" + userId + "/clips").push();
            final String message = remoteMessage.getData().get("body");
            final String title = remoteMessage.getData().get("title");
            History history = new History("12", message, "Just now", true);
            mDbRef.setValue(history).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        showNotification(title, message);
                        addToClipboard(message);
                    }
                }
            });
        }
    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        String channelId = getString(R.string.default_notification_channel_id);
        String channelReadableName = getString(R.string.default_notification_channel_name);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_edit_white_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelReadableName,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(0, builder.build());
    }

    private void addToClipboard(String text) {
        mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        mClipboardManager.setPrimaryClip(ClipData.newPlainText("auto_copy_text", text));
    }
}

