package com.buildbrothers.clipair;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import model.History;

import static com.buildbrothers.clipair.MainActivity.TEMP_UID_KEY;

class WidgetDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private Intent intent;
    List<History> mHistoryArray = new ArrayList<>();

    private FirebaseDatabase mFirebaseDatabase;
    private Query histroyRef;

    private ValueEventListener historyListener;

    public WidgetDataProvider(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }

    public void retrieveRecentHistory() {
        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mHistoryArray.clear();
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    History recentHistory = dataSnapshot1.getValue(History.class);
                    mHistoryArray.add(recentHistory);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String userId = preferences.getString(TEMP_UID_KEY, "");

        if (!userId.equals("")) {
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            histroyRef = mFirebaseDatabase.getReference("users/" + userId + "/clips").limitToLast(5);
            histroyRef.addListenerForSingleValueEvent(historyListener);
        }
    }

    @Override
    public void onCreate() {
        retrieveRecentHistory();
    }

    @Override
    public void onDataSetChanged() {
        retrieveRecentHistory();
    }

    @Override
    public void onDestroy() {
        if (histroyRef != null) {
            histroyRef.removeEventListener(historyListener);
        }
    }

    @Override
    public int getCount() {
        return mHistoryArray.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.clipair_appwidget_list_item);
        rv.setTextViewText(R.id.widget_history_text, mHistoryArray.get(position).getMainText());

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private String trimText(String text) {
        if (text.length() >= 45) {
            text = text.substring(0, Math.min(text.length(), 45)) +"...";
        }
        return text;
    }
}
