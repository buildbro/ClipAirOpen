package com.buildbrothers.clipair;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Device;
import model.Pairing;
import model.SentItem;
import utils.PairCodeGenerator;

import static com.buildbrothers.clipair.MainActivity.TEMP_UID_KEY;

public class ShareActivity extends AppCompatActivity implements RecyclerViewClickListener {

    private String pairCode;

    private TextView shareMessage_textView;
    private ImageView qr_imageView;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference rootRef, pairedRef;
    private SharedPreferences preferences;
    private String userId;
    private String deviceName;
    private boolean notPaired;

    private Query mQuery;
    private Query pairedQuery;

    private RecyclerView deviceRecyclerView;
    private DeviceListAdapter deviceListAdapter;
    private List<Device> mDeviceArray = new ArrayList<>();
    private String mainText;

    private ValueEventListener deviceListItemListener;
    private ValueEventListener pairingItemListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.setTitle("Share");

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        userId = preferences.getString(TEMP_UID_KEY, "");

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        rootRef = mFirebaseDatabase.getReference("users");
        initializeUI();

        Intent intent = getIntent();
        if (intent != null) {
            mainText = intent.getStringExtra("body");
        }

        PairCodeGenerator pairCodeGenerator = new PairCodeGenerator(7, new SecureRandom());
        pairCode = pairCodeGenerator.nextString();

    }

    private void initializeUI() {
        shareMessage_textView = findViewById(R.id.share_message);
        qr_imageView = findViewById(R.id.qr_code);

        deviceRecyclerView = findViewById(R.id.device_recycler_view);
        deviceListAdapter = new DeviceListAdapter(getApplication(), this);
    }

    private void generateQR() {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(pairCode, BarcodeFormat.QR_CODE, 300, 300);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qr_imageView.setImageBitmap(bitmap);

            //Not really a part of QR but lets set the pairCode to our shareMessage TextView here
            shareMessage_textView.setText(getText(R.string.share_message) + " " + pairCode);

            notPaired = true;
            updateUserPairCode();
            startPairing();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void updateUserPairCode() {
        deviceName = Build.MODEL;
        Map<String, Object> userUpdateMap = new HashMap<>();
        userUpdateMap.put("pairCode", pairCode);
        userUpdateMap.put("deviceName", deviceName);
        rootRef.child(userId).updateChildren(userUpdateMap);
    }

    private void retrievePairedDevices() {
        deviceListItemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mDeviceArray.clear();
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    Device device = dataSnapshot1.getValue(Device.class);
                    device.setPushKey(dataSnapshot1.getKey());
                    mDeviceArray.add(device);
                }
                LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, true);
                layoutManager.setStackFromEnd(true);
                deviceRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                deviceRecyclerView.setAdapter(deviceListAdapter);
                ViewCompat.setNestedScrollingEnabled(deviceRecyclerView, false);
                deviceListAdapter.setDeviceItems(mDeviceArray);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        pairedQuery = mFirebaseDatabase.getReference("pairedDevices")
                .orderByChild("userId").equalTo(userId);
        pairedQuery.addValueEventListener(deviceListItemListener);
    }

    private void detachDeviceItemListener() {
        if (deviceListItemListener !=null) {
            pairedQuery.removeEventListener(deviceListItemListener);
            deviceListItemListener = null;
        }
    }

    private void  startPairing() {
        pairingItemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() > 0) {
                    if (notPaired) {
                        notPaired = false;
                        Pairing pairing = dataSnapshot.getChildren().iterator().next().getValue(Pairing.class);
                        Toast.makeText(getApplicationContext(), "Yo hoo!!", Toast.LENGTH_LONG).show();
                        String permPairCode = pairing.getPermPairCode();
                        String receiverDevice = pairing.getDeviceName();
                        addNewDevice(permPairCode, receiverDevice);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mQuery = mFirebaseDatabase.getReference().child("isPairing")
                .orderByChild("pairCode").equalTo(pairCode);
        mQuery.addValueEventListener(pairingItemListener);
    }

    private void detachPairingItemListener() {
        if (pairingItemListener != null) {
            mQuery.removeEventListener(pairingItemListener);
            pairingItemListener = null;
        }
    }

    private void addNewDevice(String permPairCode, String receiverDeviceName) {
        final DatabaseReference newDeviceRef = mFirebaseDatabase.getReference().child("pairedDevices").push();
        Device newDevice = new Device(userId, permPairCode, pairCode, deviceName, receiverDeviceName, true);
        newDeviceRef.setValue(newDevice).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {

                    Toast.makeText(getApplicationContext(), "Device Added!!", Toast.LENGTH_LONG).show();

                    String newDeviceKey = newDeviceRef.getKey();
                    sendHistoryItem(newDeviceKey, mainText);
                }
            }
        });
    }

    private void sendHistoryItem(String pairKey, String text) {
        SentItem sentItem = new SentItem(text, 1);
        DatabaseReference sendableRef = mFirebaseDatabase.getReference("sentItems/" + pairKey).push();
        sendableRef.setValue(sentItem);
    }

    @Override
    public void onRowClicked(int position) {
        Device currentDevice = mDeviceArray.get(position);
        sendHistoryItem(currentDevice.getPushKey(), mainText);

    }

    @Override
    public void onViewClicked(View v, final int position) {
        if (v.getId() == R.id.options) {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            MenuInflater menuInflater = popupMenu.getMenuInflater();
            menuInflater.inflate(R.menu.device_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.remove_device) {
                        Device currentDevice = mDeviceArray.get(position);
                        String currentPushKey = currentDevice.getPushKey();
                        DatabaseReference deleteRef = mFirebaseDatabase.getReference("pairedDevice/" + currentPushKey);
                        deleteRef.removeValue();
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        retrievePairedDevices();
        generateQR();
    }

    @Override
    protected void onPause() {
        super.onPause();
        detachDeviceItemListener();
        detachPairingItemListener();
    }
}
