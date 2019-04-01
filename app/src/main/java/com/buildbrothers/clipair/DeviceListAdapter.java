package com.buildbrothers.clipair;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import model.Device;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private List<Device> mDeviceList;
    private LayoutInflater mInflater;
    private RecyclerViewClickListener listener;

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView;
        ImageView iconImageView, optionsImageView;
        public DeviceViewHolder(View itemView, final RecyclerViewClickListener listener) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.device_name);
            iconImageView = itemView.findViewById(R.id.device_icon);
            optionsImageView = itemView.findViewById(R.id.options);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRowClicked(getAdapterPosition());
                    }
                }
            });

            optionsImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onViewClicked(v, getAdapterPosition());
                    }
                }
            });
        }
    }

    public DeviceListAdapter(Context context, RecyclerViewClickListener listener) {
        mInflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = mInflater.inflate(R.layout.device_list_item, viewGroup, false);
        return new DeviceViewHolder(itemView, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder deviceViewHolder, int i) {
        if (mDeviceList != null) {
            Device currentDevice = mDeviceList.get(i);
            deviceViewHolder.nameTextView.setText(currentDevice.getDeviceName2());
        }
    }

    @Override
    public int getItemCount() {
        if (mDeviceList != null) {
            return mDeviceList.size();
        } else {
            return 0;
        }
    }

    public void setDeviceItems(List<Device> deviceItems) {
        mDeviceList = deviceItems;
    }

    public List<Device> getDeviceItems() {
        return mDeviceList;
    }
}
