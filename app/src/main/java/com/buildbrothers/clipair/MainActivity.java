package com.buildbrothers.clipair;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.github.thunder413.datetimeutils.DateTimeStyle;
import com.github.thunder413.datetimeutils.DateTimeUtils;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.Device;
import model.History;
import model.Pairing;
import model.User;

import static utils.Constants.DB_PATH_IS_PAIRING;
import static utils.Constants.DB_PATH_PAIRED_DEVICES;
import static utils.Constants.DB_PATH_USERS;
import static utils.Constants.EXTRA_BODY_KEY;
import static utils.Constants.FIRST_RUN_KEY;
import static utils.Constants.ORIGIN_CODE_KEY;
import static utils.Constants.ORIGIN_CODE_VALUE_MAIN;
import static utils.Constants.PAIR_CODE_SIZE;
import static utils.Constants.PERM_PAIR_CODE;
import static utils.Constants.TEMP_UID_KEY;
import static utils.GenUtils.isNetworkAvailable;

public class MainActivity extends AppCompatActivity implements RecyclerViewClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private RecyclerView historyRecyclerView;
    private ProgressBar progressBar;
    private HistoryListAdapter mAdapter;
    private List<History> mHistoryArray = new ArrayList<>();

    private FloatingActionMenu fabMenu;
    private FloatingActionButton scannerBtn, codeBtn;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;

    private SharedPreferences preferences;
    private String userTempKey;
    private String userPermPairCode;
    private boolean notPaired;

    private String senderPermPairCode;
    private String deviceName;
    private String senderDeviceName;
    private LinearLayout progressBarContainer;
    private ProgressBar pairingProgressBar;
    private TextView progressBarMessage;
    private AlertDialog dialogC;

    private ValueEventListener historyItemListener;
    private ValueEventListener pairedDevicesListener;
    private Query mQuery;
    private AlertDialog dialogDetails;
    private History currentHistoryItem;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private boolean isFirstRun;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        userTempKey = preferences.getString(TEMP_UID_KEY, "");
        userPermPairCode = preferences.getString(PERM_PAIR_CODE, "");

        preferences.registerOnSharedPreferenceChangeListener(this);

        checkIfFirstRun();

        initializeUI();

        fabMenu.setClosedOnTouchOutside(true);

        scannerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openScanner();
            }
        });

        codeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCodeDialog();
            }
        });

        mFirebaseAuth = FirebaseAuth.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user == null) {
                    checkIfFirstRun();
                }
            }
        };

        if(!isNetworkAvailable(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "Your are currently offline", Toast.LENGTH_LONG).show();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void checkIfFirstRun() {
        isFirstRun = preferences.getBoolean(FIRST_RUN_KEY, true);
        if (isFirstRun) {
            Intent welcomeIntent = new Intent(MainActivity.this, WelcomeActivity.class);
            welcomeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(welcomeIntent);
        }
    }

    private void initializeUI() {

        //Views for fab
        fabMenu = findViewById(R.id.menu);
        scannerBtn = findViewById(R.id.menu_item_qr);
        codeBtn = findViewById(R.id.menu_item_code);

        progressBar = findViewById(R.id.history_progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        emptyView = findViewById(R.id.empty_view);
        historyRecyclerView = findViewById(R.id.history_recycler_view);
        mAdapter = new HistoryListAdapter(MainActivity.this, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, true);
        layoutManager.setStackFromEnd(true);
        historyRecyclerView.setLayoutManager(layoutManager);
        historyRecyclerView.setAdapter(mAdapter);
    }

    private void retrieveHistory() {

        if (historyItemListener == null) {
            historyItemListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    mHistoryArray.clear();
                    for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                        History history = dataSnapshot1.getValue(History.class);

                        if (history == null) {
                            throw new NullPointerException("History is null");
                        }
                        history.setPushKey(dataSnapshot1.getKey());
                        mHistoryArray.add(history);
                    }

                    mAdapter.setHistoryItems(mHistoryArray);

                    if (progressBar.getVisibility() == View.VISIBLE) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    if (mHistoryArray.isEmpty()) {
                        historyRecyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        
                    } else {
                        historyRecyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
        }

        if (!userTempKey.equals("")) {
            myRef = mFirebaseDatabase.getReference(DB_PATH_USERS + "/" + userTempKey + "/clips");
            myRef.keepSynced(true);

            myRef.addValueEventListener(historyItemListener);
        }
    }

    private void detachHistoryItemListener() {
        if (historyItemListener != null) {

            if (dialogDetails != null) {
                if (!dialogDetails.isShowing()) {
                    return;
                }
            }
            myRef.removeEventListener(historyItemListener);
            historyItemListener = null;
        }
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
            case R.id.help:
                showHelp();
                return true;
            case R.id.account:
                gotoAccount();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void gotoAccount() {
        Intent accountIntent = new Intent(MainActivity.this, AccountActivity.class);
        accountIntent.putExtra(ORIGIN_CODE_KEY, ORIGIN_CODE_VALUE_MAIN);
        startActivity(accountIntent);
    }

    private void showHelp() {
        Intent helpIntent = new Intent(MainActivity.this, HelpActivity.class);
        startActivity(helpIntent);
    }

    @Override
    public void onRowClicked(final int position) {

        currentHistoryItem = mHistoryArray.get(position);

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_details, null);
        TextView mTextView = mView.findViewById(R.id.main_text);
        TextView dateTextView = mView.findViewById(R.id.date_created);
        ImageView share = mView.findViewById(R.id.btnShare);
        ImageView delete = mView.findViewById(R.id.btnDelete);

        Date datePosted = new Date(currentHistoryItem.getTimePostedLong());
        dateTextView.setText(DateTimeUtils.formatWithStyle(datePosted, DateTimeStyle.MEDIUM));
        mTextView.setText(currentHistoryItem.getMainText());
        mBuilder.setView(mView);
        dialogDetails = mBuilder.create();
        dialogDetails.show();

        //dialog buttons control
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ShareActivity.class);
                intent.putExtra(EXTRA_BODY_KEY, currentHistoryItem.getMainText());
                startActivity(intent);
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference deleteRef = mFirebaseDatabase.getReference(DB_PATH_USERS + "/" + userTempKey + "/clips/" + currentHistoryItem.getPushKey());
                deleteRef.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Item removed!", Toast.LENGTH_LONG).show();
                            mAdapter.notifyItemRemoved(position);
                        }
                    }
                });
                dialogDetails.dismiss();
            }
        });

    }

    @Override
    public void onViewClicked(View v, int position) {

    }

    private void openScanner() {
        Intent intent = new Intent(getApplicationContext(), ScannerActivity.class);
        startActivity(intent);
    }

    private void openCodeDialog() {
        AlertDialog.Builder codeDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        View codeDialogView = getLayoutInflater().inflate(R.layout.dialog_code, null);
        EditText codeEditText = codeDialogView.findViewById(R.id.code);
        progressBarContainer = codeDialogView.findViewById(R.id.progress_bar_container);
        pairingProgressBar = codeDialogView.findViewById(R.id.pairing_pb);
        progressBarMessage = codeDialogView.findViewById(R.id.pairing_pb_label);
        codeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == PAIR_CODE_SIZE) {
                    notPaired = true;
                    progressBarContainer.setVisibility(View.VISIBLE);
                    progressBarMessage.setText(R.string.finding_device);
                    pairWithCode(s.toString());
                } else {
                    if (progressBarContainer.getVisibility() == View.VISIBLE) {
                        progressBarContainer.setVisibility(View.GONE);
                    }
                }

            }
        });

        codeDialogBuilder.setView(codeDialogView);
        dialogC = codeDialogBuilder.create();
        dialogC.show();
    }

    private void pairWithCode(final String code) {
        Query mQuery = mFirebaseDatabase.getReference().child(DB_PATH_USERS)
                .orderByChild("pairCode").equalTo(code);
        mQuery.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() > 0) {
                    progressBarMessage.setText(R.string.pairing);
                    User senderUserData = dataSnapshot.getChildren().iterator().next().getValue(User.class);
                    if (senderUserData == null) {
                        throw new NullPointerException("SenderData is null");
                    }
                    senderPermPairCode = senderUserData.getPermPairCode();
                    deviceName = Build.MODEL;
                    senderDeviceName = senderUserData.getDeviceName();

                    DatabaseReference isPairingRef = mFirebaseDatabase.getReference(DB_PATH_IS_PAIRING).push();
                    Pairing pairing = new Pairing(code, userPermPairCode, deviceName);
                    isPairingRef.setValue(pairing).addOnCompleteListener(new OnCompleteListener<Void>() {

                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                startPairing(code);
                            } else {
                                progressBarContainer.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void startPairing(final String code) {
        pairedDevicesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() > 0) {
                    if (notPaired) {
                        notPaired = false;
                        DatabaseReference pairRef = mFirebaseDatabase.getReference(DB_PATH_PAIRED_DEVICES).push();
                        Device pairDevice = new Device(userTempKey, senderPermPairCode, code, deviceName, senderDeviceName, true);
                        pairRef.setValue(pairDevice).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    progressBarContainer.setVisibility(View.GONE);
                                    Toast.makeText(getApplicationContext(), "Pairing 100% done", Toast.LENGTH_LONG).show();
                                    dialogC.dismiss();
                                } else {
                                    progressBarContainer.setVisibility(View.GONE);
                                    Toast.makeText(getApplicationContext(), "Oops! something went wrong.", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                } else {
                    progressBarContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        mQuery = mFirebaseDatabase.getReference().child(DB_PATH_PAIRED_DEVICES)
                .orderByChild("pairCode").equalTo(code);
        mQuery.addValueEventListener(pairedDevicesListener);
    }

    private void detachPairedDevicesListener() {
        if (pairedDevicesListener != null) {
            //To allow user continue current pairing session in background, only detach listener when dialog is not being shown
            if (!dialogC.isShowing()) {
                mQuery.removeEventListener(pairedDevicesListener);
                pairedDevicesListener = null;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!userTempKey.equals("")) {
            retrieveHistory();
        }

        if (mAuthStateListener != null) {
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachHistoryItemListener();
        detachPairedDevicesListener();

        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        userTempKey = sharedPreferences.getString(TEMP_UID_KEY, "");
        isFirstRun = sharedPreferences.getBoolean(FIRST_RUN_KEY, true);
        detachHistoryItemListener();
        retrieveHistory();

    }

}
