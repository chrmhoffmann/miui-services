package com.android.server.am;

import java.lang.reflect.Field;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class BroadcastDispatcherFeatureImpl {
    public ArrayList<BroadcastRecord> getOrderedBroadcasts(BroadcastDispatcher dispatcher) {
        try {
            Field f = dispatcher.getClass().getDeclaredField("mOrderedBroadcasts");
            f.setAccessible(true);
            return (ArrayList) f.get(dispatcher);
        } catch (Exception e) {
            return null;
        }
    }
}
