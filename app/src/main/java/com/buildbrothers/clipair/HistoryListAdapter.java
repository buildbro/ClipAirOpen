package com.buildbrothers.clipair;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import model.History;

public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.HistoryViewHolder> {

    private List<History> mHistoryList;
    private LayoutInflater mInflater;
    private RecyclerViewClickListener listener;

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView mainText;
        public HistoryViewHolder(View itemView, final RecyclerViewClickListener listener) {
            super(itemView);
            mainText = itemView.findViewById(R.id.text);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRowClicked(getAdapterPosition());
                    }
                }
            });
        }
    }

    public HistoryListAdapter(Context context, RecyclerViewClickListener listener) {
        mInflater = LayoutInflater.from(context);
        this.listener = listener;

    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = mInflater.inflate(R.layout.main_list_item, viewGroup, false);
        return new HistoryViewHolder(itemView, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder historyViewHolder, int i) {
        if (mHistoryList != null) {
            History currentHistory = mHistoryList.get(i);
            historyViewHolder.mainText.setText(currentHistory.getMainText());
        }

    }

    @Override
    public int getItemCount() {
        if (mHistoryList != null) {
            return mHistoryList.size();
        } else {
            return 0;
        }
    }

    void setHistoryItems(List<History> historyItems) {
        mHistoryList = historyItems;
        notifyDataSetChanged();
    }

    public List<History> getHistoryItems() {
        return mHistoryList;
    }


}
