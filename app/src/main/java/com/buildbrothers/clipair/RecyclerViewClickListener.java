package com.buildbrothers.clipair;

import android.view.View;

/**
 * Created by EA on 6/5/2018.
 */

public interface RecyclerViewClickListener {

    void onRowClicked(int position);
    void onViewClicked(View v, int position);
}
