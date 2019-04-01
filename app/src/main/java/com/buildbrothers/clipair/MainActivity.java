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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import model.Device;
import model.History;
import model.Pairing;
import model.User;

import static com.buildbrothers.clipair.WelcomeActivity.ORIGIN_CODE_NAME;

public class MainActivity extends AppCompatActivity implements RecyclerViewClickListener {
    private static final int ORIGIN_CODE_MAIN = 2;
    private RecyclerView historyRecyclerView;
    private ProgressBar progressBar;
    private HistoryListAdapter mAdapter;
    private List<History> mHistoryArray = new ArrayList<>();

    private FloatingActionButton scannerBtn, codeBtn;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;

    private SharedPreferences preferences;
    private String userId;
    private String userPermPairCode;
    private boolean notPaired;
    private String token;

    public static final String TEMP_UID_KEY = "UID";
    public static final String PERM_PAIR_CODE = "PPC";
    private String senderPermPairCode;
    private String deviceName;
    private String senderDeviceName;
    private LinearLayout progressBarContainer;
    private ProgressBar pairingProgressBar;
    private TextView progressBarMessage;
    private AlertDialog dialogC;
    private SearchView searchView;

    private ValueEventListener historyItemListener;
    private ValueEventListener pairedDevicesListener;
    private Query mQuery;
    private AlertDialog dialogDetails;
    private History currentHistoryItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent sIntent = new Intent(this, ClipBoardService.class);
        this.startService(sIntent);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        userId = preferences.getString(TEMP_UID_KEY, "");
        userPermPairCode = preferences.getString(PERM_PAIR_CODE, "");

        checkIfFirstRun();

        initializeUI();

        //retrieveFirebaseId();


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
    }

    private void checkIfFirstRun() {
        boolean isFirstRun = preferences.getBoolean("firstRun", true);
        if (isFirstRun) {
            Intent welcomeIntent = new Intent(MainActivity.this, WelcomeActivity.class);
            startActivity(welcomeIntent);
            finish();
        }
    }

    /*private String retrieveFirebaseId() {
        FirebaseInstanceId.getInstance()
                .getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    return;
                }

                token = task.getResult().getToken();
            }
        });

        return token;
    } */

    private void initializeUI() {

        //Views for fab
        scannerBtn = findViewById(R.id.menu_item_qr);
        codeBtn = findViewById(R.id.menu_item_code);

        progressBar = findViewById(R.id.history_progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        historyRecyclerView = findViewById(R.id.history_recycler_view);
        mAdapter = new HistoryListAdapter(MainActivity.this, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, true);
        layoutManager.setStackFromEnd(true);
        historyRecyclerView.setLayoutManager(layoutManager);
        historyRecyclerView.setAdapter(mAdapter);


        //TODO Temp Firebase code Please clean-up before code upload
        /*DatabaseReference tempRef;
        tempRef = mFirebaseDatabase.getReference("pairedDevices");
        tempRef.setValue(new Device("33333", "67yyh", "dsds", "Sdsd")); */

    }

    private void retrieveHistory() {

        if (historyItemListener == null) {
            historyItemListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    mHistoryArray.clear();
                    for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                        History history = dataSnapshot1.getValue(History.class);
                        history.setPushKey(dataSnapshot1.getKey().toString());
                        mHistoryArray.add(history);
                    }

                    mAdapter.setHistoryItems(mHistoryArray);

                    if (progressBar.getVisibility() == View.VISIBLE) {
                        progressBar.setVisibility(View.GONE);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
        }

        myRef = mFirebaseDatabase.getReference("users/" + userId + "/clips");
        myRef.keepSynced(true);

        myRef.addValueEventListener(historyItemListener);
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
            //mAdapter.clear();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        final MenuItem searchItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.getFilter().filter(newText);
                return false;
            }
        });
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
            case R.id.account:
                Intent accountIntent = new Intent(MainActivity.this, AccountActivity.class);
                accountIntent.putExtra(ORIGIN_CODE_NAME, ORIGIN_CODE_MAIN);
                startActivity(accountIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRowClicked(final int position) {

        currentHistoryItem = mHistoryArray.get(position);

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_details, null);
        TextView mTextView = mView.findViewById(R.id.main_text);
        TextView dateTextView = mView.findViewById(R.id.details);
        ImageView share = mView.findViewById(R.id.btnShare);
        ImageView delete = mView.findViewById(R.id.btnDelete);

        dateTextView.setText("Created ");
        mTextView.setText(currentHistoryItem.getMainText());
        mBuilder.setView(mView);
        dialogDetails = mBuilder.create();
        dialogDetails.show();
        //dialog buttons control
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ShareActivity.class);
                intent.putExtra("body", currentHistoryItem.getMainText());
                startActivity(intent);
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference deleteRef = mFirebaseDatabase.getReference("users/" + userId + "/clips/" + currentHistoryItem.getPushKey());
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
                if (s.length() == 7) {
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

    /*private void tempRegistration() {
        final DatabaseReference myRef = mFirebaseDatabase.getReference("users").push();
        User user = new User(false, null, null, token);
        myRef.setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(TEMP_UID_KEY, myRef.getKey());
                    editor.putString(PERM_PAIR_CODE, token);
                    editor.apply();
                    Toast.makeText(getApplicationContext(), "ClipAir temp user created!", Toast.LENGTH_LONG).show();
                    userId = myRef.getKey();

                    retrieveHistory();
                }
            }
        });
    }  */

    private void pairWithCode(final String code) {
        Query mQuery = mFirebaseDatabase.getReference().child("users")
                .orderByChild("pairCode").equalTo(code);
        mQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() > 0) {
                    progressBarMessage.setText(R.string.pairing);
                    User senderUserData = dataSnapshot.getChildren().iterator().next().getValue(User.class);
                    senderPermPairCode = senderUserData.getPermPairCode();
                    deviceName = Build.MODEL;
                    senderDeviceName = senderUserData.getDeviceName();

                    DatabaseReference isPairingRef = mFirebaseDatabase.getReference("isPairing").push();
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
                        DatabaseReference pairRef = mFirebaseDatabase.getReference("pairedDevices").push();
                        Device pairDevice = new Device(userId, senderPermPairCode, code, deviceName, senderDeviceName, true);
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

        mQuery = mFirebaseDatabase.getReference().child("pairedDevices")
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
        if (!userId.equals("")) {
            retrieveHistory();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachHistoryItemListener();
        detachPairedDevicesListener();
    }
}
