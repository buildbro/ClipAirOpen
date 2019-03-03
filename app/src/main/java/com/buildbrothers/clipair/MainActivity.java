package com.buildbrothers.clipair;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import model.History;

public class MainActivity extends AppCompatActivity implements RecyclerViewClickListener {
    private RecyclerView historyRecyclerView;
    private ProgressBar progressBar;
    private HistoryListAdapter mAdapter;
    private List<History> mHistoryArray = new ArrayList<>();

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;

    private SharedPreferences preferences;
    private String userId;

    public static final String TEMP_UID_KEY = "UID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent sIntent = new Intent(this, ClipBoardService.class);
        this.startService(sIntent);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        userId = preferences.getString(TEMP_UID_KEY, "");

        initializeUI();
    }

    private void initializeUI() {
        progressBar = findViewById(R.id.history_progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        historyRecyclerView = findViewById(R.id.history_recycler_view);
        mAdapter = new HistoryListAdapter(getApplication(), this);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference("users/" + userId + "/clips");
        myRef.keepSynced(true);

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mHistoryArray.clear();
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    History history = dataSnapshot1.getValue(History.class);
                    //history.setProjectID(dataSnapshot1.getKey().toString());
                    mHistoryArray.add(history);
                }
                historyRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                historyRecyclerView.setAdapter(mAdapter);
                mAdapter.setHistoryItems(mHistoryArray);

                if (progressBar.getVisibility() == View.VISIBLE) {
                    progressBar.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.search:
                //newGame();
                return true;
            case R.id.help:
                //showHelp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRowClicked(int position) {

    }

    @Override
    public void onViewClicked(View v, int position) {

    }
}
