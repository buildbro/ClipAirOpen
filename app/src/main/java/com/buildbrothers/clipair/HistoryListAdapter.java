package com.buildbrothers.clipair;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.github.thunder413.datetimeutils.DateTimeStyle;
import com.github.thunder413.datetimeutils.DateTimeUnits;
import com.github.thunder413.datetimeutils.DateTimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import model.History;

public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.HistoryViewHolder> implements Filterable {

    private List<History> mHistoryList; //cached copy
    private List<History> mFilteredHistoryList = new ArrayList<>();
    private LayoutInflater mInflater;
    private RecyclerViewClickListener listener;
    private Context mContext;

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView mainText, timeTextView;
        public HistoryViewHolder(View itemView, final RecyclerViewClickListener listener) {
            super(itemView);
            mainText = itemView.findViewById(R.id.text);
            timeTextView = itemView.findViewById(R.id.time_posted);

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
        this.mContext = context;

    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = mInflater.inflate(R.layout.main_list_item, viewGroup, false);
        return new HistoryViewHolder(itemView, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder historyViewHolder, int i) {
        if (mFilteredHistoryList != null) {
            History currentHistory = mFilteredHistoryList.get(i);
            historyViewHolder.mainText.setText(currentHistory.getMainText());

            if (currentHistory.getTimePosted() != null) {
                Date datePosted = new Date(currentHistory.getTimePostedLong());
                Date dateNow = new Date();
                int timeDiff = DateTimeUtils.getDateDiff(dateNow, datePosted, DateTimeUnits.HOURS);

                if (timeDiff < 12) {
                    historyViewHolder.timeTextView.setText(DateTimeUtils.getTimeAgo(mContext, datePosted, DateTimeStyle.AGO_SHORT_STRING));
                } else {
                    historyViewHolder.timeTextView.setText(DateTimeUtils.formatWithStyle(datePosted, DateTimeStyle.MEDIUM));
                }
            }
        }

    }

    @Override
    public int getItemCount() {
        if (mFilteredHistoryList != null) {
            return mFilteredHistoryList.size();
        } else {
            return 0;
        }
    }

    @Override
    public long getItemId(int position) {
        if (position < mFilteredHistoryList.size()) {
            return mFilteredHistoryList.get(position).getTimePostedLong();
        }
        return  RecyclerView.NO_ID;
    }

    void setHistoryItems(List<History> historyItems) {
        mHistoryList = historyItems;
        mFilteredHistoryList.clear();
        mFilteredHistoryList.addAll(mHistoryList);
        notifyDataSetChanged();
    }

    public List<History> getHistoryItems() {
        return mFilteredHistoryList;
    }

    public void clear() {
        int size = mFilteredHistoryList.size();
        mFilteredHistoryList.clear();
        notifyItemRangeRemoved(0, size);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString();
                List<History> filteredList = new ArrayList<>();
                if (charString.isEmpty()) {
                    filteredList = mHistoryList;
                } else {
                    for (History row : mHistoryList) {
                        if (row.getMainText().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredHistoryList = (ArrayList<History>) results.values;
                notifyDataSetChanged();
            }
        };
    }

}
