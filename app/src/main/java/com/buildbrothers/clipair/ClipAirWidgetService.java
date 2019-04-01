package com.buildbrothers.clipair;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class ClipAirWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetDataProvider(this, intent);
    }
}
