package com.android.server;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.content.SyncManagerStubImpl;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public class MiuiAppUsageStats {
    private static final int ACTIVITY_DESTROYED = 24;
    private static final int ACTIVITY_STOPPED = 23;
    private static final boolean DEBUG = false;
    private static final int STAT_TYPE_DAY = 0;
    private static final int STAT_TYPE_HOUR = 1;
    private static final String TAG = "MiuiAppUsageStats";
    private static long INTERVAL_HOUR = SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED;
    private static final List<String> APP_STAT_TO_SETTINGS = new ArrayList<String>() { // from class: com.android.server.MiuiAppUsageStats.1
        {
            add("com.miui.touchassistant");
            add("com.xiaomi.misettings");
        }
    };
    private static final List<String> SPECIAL_APP_LIST = new ArrayList<String>() { // from class: com.android.server.MiuiAppUsageStats.2
        {
            add("com.android.systemui");
            add("com.android.settings:remote");
            add("com.android.nfc");
            add("com.android.provision");
            add(InputMethodManagerServiceImpl.MIUI_HOME);
            add("com.mi.android.globallauncher");
        }
    };

    public ArrayList<String> getTop3Apps(Context ctx, long start, long end) {
        ArrayMap<String, AppUsageStats> ret = new ArrayMap<>();
        List<AppUsageStats> list = new ArrayList<>();
        ArrayList<String> result = new ArrayList<>();
        aggregateUsageStatsByEvent(ctx, getEventStats(ctx, start, end), start, end, ret);
        filterUsageEventResult(ctx, start, end, ret, list);
        Collections.sort(list);
        for (int i = 0; i < list.size() && i < 3; i++) {
            String value = list.get(i).getPkgName() + "=" + (list.get(i).getTotalForegroundTime() / 1000);
            result.add(value);
        }
        return result;
    }

    public void filterUsageEventResult(Context ctx, long start, long end, ArrayMap<String, AppUsageStats> result, List<AppUsageStats> list) {
        if (ctx != null && result != null) {
            String[] keys = new String[result.keySet().size()];
            result.keySet().toArray(keys);
            for (String key : keys) {
                AppUsageStats stat = result.get(key);
                if (stat != null) {
                    if (stat.getLastUsageTime() >= start && stat.getLastUsageTime() <= end) {
                        String pkgName = stat.getPkgName();
                        if (!stat.isValid()) {
                            Slog.e(TAG, "filterUsageEventResult()......Skip, invalid stats. pkgName=" + pkgName);
                            result.remove(key);
                        } else if (isSpecialApp(pkgName)) {
                            result.remove(key);
                        } else {
                            list.add(stat);
                        }
                    }
                    Slog.e(TAG, "Wow! We filter out it again? pkgName=" + stat.getPkgName());
                    result.remove(key);
                }
            }
        }
    }

    private boolean isSpecialApp(String pkgName) {
        if (SPECIAL_APP_LIST.contains(pkgName)) {
            return true;
        }
        return false;
    }

    private UsageEvents getEventStats(Context ctx, long start, long end) {
        UsageStatsManager manager = (UsageStatsManager) ctx.getSystemService("usagestats");
        if (manager != null) {
            UsageEvents events = manager.queryEvents(start, end);
            return events;
        }
        Slog.e(TAG, "getEventStats()......manager is null!");
        return null;
    }

    private boolean valid(UsageEvents.Event event) {
        return event.getEventType() == 1 || event.getEventType() == 2;
    }

    private boolean vaildStopEvent(UsageEvents.Event event) {
        return event.getEventType() == 2 || event.getEventType() == 23 || event.getEventType() == 24;
    }

    public boolean validStartEvent(UsageEvents.Event event) {
        return event.getEventType() == 1;
    }

    private void aggregateEventByPackage(UsageEvents events, ArrayMap<String, List<UsageEvents.Event>> aggregateEvents) {
        if (events == null || aggregateEvents == null) {
            return;
        }
        while (events.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            if (events.getNextEvent(event) && valid(event)) {
                if (APP_STAT_TO_SETTINGS.contains(event.getPackageName())) {
                    List<UsageEvents.Event> list = aggregateEvents.get("com.android.settings");
                    if (list == null) {
                        list = new ArrayList();
                    }
                    list.add(event);
                    aggregateEvents.put("com.android.settings", list);
                } else {
                    boolean exist = aggregateEvents.containsKey(event.getPackageName());
                    List<UsageEvents.Event> eventList = exist ? aggregateEvents.get(event.getPackageName()) : new ArrayList<>();
                    eventList.add(event);
                    if (!exist) {
                        aggregateEvents.put(event.getPackageName(), eventList);
                    }
                }
            }
        }
    }

    private long handleCrossUsage(String pkgName, long start, long end, int statType) {
        long maxDiff;
        long diff = end - start;
        switch (statType) {
            case 0:
                maxDiff = INTERVAL_HOUR * 4;
                break;
            case 1:
                maxDiff = INTERVAL_HOUR;
                break;
            default:
                maxDiff = 0;
                break;
        }
        return diff > maxDiff ? 0L : diff;
    }

    /* JADX WARN: Removed duplicated region for block: B:36:0x00f3  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void guess(android.content.Context r25, long r26, long r28, com.android.server.MiuiAppUsageStats.AppUsageStats r30) {
        /*
            Method dump skipped, instructions count: 328
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.MiuiAppUsageStats.guess(android.content.Context, long, long, com.android.server.MiuiAppUsageStats$AppUsageStats):void");
    }

    private boolean checkStopped(UsageEvents usageEvents, String pkgName) {
        if (usageEvents == null || TextUtils.isEmpty(pkgName)) {
            Slog.e(TAG, "checkStopped()......return since invalid params.");
            return false;
        }
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            if (usageEvents.getNextEvent(event) && pkgName.equals(event.getPackageName())) {
                return vaildStopEvent(event);
            }
        }
        return false;
    }

    private boolean aggregate(Context ctx, List<UsageEvents.Event> events, long statBegin, long statEnd, AppUsageStats stat) {
        long diff;
        long timeStamp;
        if (events == null || events.isEmpty() || stat == null) {
            Slog.e(TAG, "aggregate()......Fail since invalid params.");
            return false;
        }
        int statType = statEnd - statBegin > INTERVAL_HOUR ? 0 : 1;
        String pkgName = stat.getPkgName();
        long start = 0;
        for (int idx = 0; idx < events.size(); idx++) {
            UsageEvents.Event event = events.get(idx);
            if (!TextUtils.equals(event.getPackageName(), pkgName)) {
                Slog.w(TAG, "Ops! Fail to aggregate due to different package. event.pkgName=" + event.getPackageName() + ", stat.pkgName=" + pkgName);
            }
            int eventType = event.getEventType();
            switch (eventType) {
                case 1:
                    start = event.getTimeStamp();
                    stat.increaseForegroundCount();
                    if (stat.firstForeGroundTime == 0 || stat.firstForeGroundTime > start) {
                        stat.firstForeGroundTime = start;
                        break;
                    } else {
                        break;
                    }
                    break;
                case 2:
                    if (start <= 0 && idx > 0) {
                        Slog.e(TAG, "aggregate()...start <= 0, This is not the first MOVE_TO_BACKGROUND." + pkgName);
                        break;
                    } else {
                        long timeStamp2 = event.getTimeStamp();
                        if (start <= 0) {
                            diff = handleCrossUsage(pkgName, statBegin, timeStamp2, statType);
                            timeStamp = timeStamp2;
                        } else {
                            timeStamp = timeStamp2;
                            diff = timeStamp - start;
                        }
                        start = 0;
                        if (diff <= 0) {
                            Slog.e(TAG, "aggregate()...Skip this aggregate, diff is invalid diff= " + diff);
                            break;
                        } else {
                            stat.addForegroundTime(diff);
                            stat.updateLastUsageTime(timeStamp);
                            if (stat.lastBackGroundTime == 0 || stat.lastBackGroundTime < timeStamp) {
                                stat.lastBackGroundTime = timeStamp;
                                break;
                            } else {
                                break;
                            }
                        }
                    }
                    break;
                default:
                    Slog.e(TAG, "Ops! Invalid eventType for aggregate. pkgName=" + stat.getPkgName() + ", eventType=" + eventType + ",start=" + start);
                    break;
            }
        }
        int idx2 = (start > 0L ? 1 : (start == 0L ? 0 : -1));
        if (idx2 > 0) {
            guess(ctx, start, statEnd, stat);
        }
        return true;
    }

    private void aggregateUsageStatsByEvent(Context ctx, UsageEvents events, long start, long end, ArrayMap<String, AppUsageStats> result) {
        if (events == null || result == null || ctx == null) {
            return;
        }
        ArrayMap<String, List<UsageEvents.Event>> aggregateEvents = new ArrayMap<>();
        aggregateEventByPackage(events, aggregateEvents);
        Set<String> keys = aggregateEvents.keySet();
        for (String key : keys) {
            AppUsageStats stat = new AppUsageStats(key);
            if (aggregate(ctx, aggregateEvents.get(key), start, end, stat)) {
                result.put(key, stat);
            }
        }
    }

    /* loaded from: classes.dex */
    public class AppUsageStats implements Comparable<AppUsageStats> {
        private String pkgName;
        private long totalForegroundTime = 0;
        private long lastUsageTime = 0;
        private int foregroundCount = 0;
        public long firstForeGroundTime = 0;
        public long lastBackGroundTime = 0;

        public AppUsageStats(String pkgName) {
            MiuiAppUsageStats.this = this$0;
            this.pkgName = pkgName;
        }

        public long getTotalForegroundTime() {
            return this.totalForegroundTime;
        }

        public long getLastUsageTime() {
            return this.lastUsageTime;
        }

        public int getForegroundCount() {
            return this.foregroundCount;
        }

        public void setTotalForegroundTime(long totalForegroundTime) {
            this.totalForegroundTime = totalForegroundTime;
        }

        public void setLastUsageTime(long lastUsageTime) {
            this.lastUsageTime = lastUsageTime;
        }

        public void setForegroundCount(int foregroundCount) {
            this.foregroundCount = foregroundCount;
        }

        public void addForegroundTime(long foregroundTime) {
            this.totalForegroundTime += foregroundTime;
        }

        public void minusForegroundTime(long foregroundTime) {
            this.totalForegroundTime -= foregroundTime;
        }

        public void increaseForegroundCount() {
            this.foregroundCount++;
        }

        public String getPkgName() {
            return this.pkgName;
        }

        public void setPkgName(String pkgName) {
            this.pkgName = pkgName;
        }

        public void updateLastUsageTime(long time) {
            if (time > this.lastUsageTime) {
                this.lastUsageTime = time;
            }
        }

        public boolean isValid() {
            return this.totalForegroundTime > 0;
        }

        public int compareTo(AppUsageStats data) {
            return Long.compare(data.totalForegroundTime, this.totalForegroundTime);
        }
    }
}
