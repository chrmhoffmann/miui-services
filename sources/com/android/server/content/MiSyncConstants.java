package com.android.server.content;
/* loaded from: classes.dex */
public final class MiSyncConstants {
    private MiSyncConstants() {
    }

    /* loaded from: classes.dex */
    public static class Config {
        public static final long SYNC_DELAY_ON_ROOM_FORBIDDEN = 30000;
        public static final String XIAOMI_ACCOUNT_TYPE = "com.xiaomi";

        private Config() {
        }
    }

    /* loaded from: classes.dex */
    public static class Strategy {
        public static final String EXTRA_KEY_BATTERY_CHARGING = "key_battery_charging";
        public static final String EXTRA_KEY_INTERACTIVE = "key_interactive";
        public static final String EXTRA_KEY_LAST_SCREEN_OFF_TIME = "key_last_screen_off_time";
        public static final String EXTRA_KEY_NUM_SYNCS = "key_num_syncs";

        private Strategy() {
        }
    }
}
