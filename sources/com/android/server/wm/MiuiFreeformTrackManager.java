package com.android.server.wm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Handler;
import android.os.IBinder;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import com.android.server.DarkModeOneTrackHelper;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import com.miui.analytics.ITrackBinder;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class MiuiFreeformTrackManager {
    private static final String APP_ID = "31000000538";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String SERVER_CLASS_NAME = "com.miui.analytics.onetrack.TrackService";
    private static final String SERVER_PKG_NAME = "com.miui.analytics";
    private static final String TAG = "MiuiFreeformOneTrackManager";
    private Context mContext;
    private ITrackBinder mITrackBinder;
    private MiuiFreeFormGestureController mMiuiFreeFormGestureController;
    private ServiceConnection mServiceConnection;
    public final Object mFreeFormTrackLock = new Object();
    private Runnable mBindOneTrackService = new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.2
        @Override // java.lang.Runnable
        public void run() {
            MiuiFreeformTrackManager.this.bindOneTrackService();
        }
    };
    private Handler mHandler = new Handler(MiuiBgThread.get().getLooper());

    /* loaded from: classes.dex */
    public static final class CommonTrackConstants {
        public static final String DATA_VERSION = "22053100";
        public static final String DEVICE_TYPE_PAD = "pad";
        public static final String DEVICE_TYPE_PHONE = "手机";
        public static final String SCREEN_ORIENTATION_LANDSCAPE = "横屏";
        public static final String SCREEN_ORIENTATION_PORTRAIT = "竖屏";
        public static final String SCREEN_TYPE_INNER = "内屏";
        public static final String SCREEN_TYPE_NOTHING = "nothing";
        public static final String SCREEN_TYPE_OUTTER = "外屏";
    }

    /* loaded from: classes.dex */
    public static final class MiniWindowTrackConstants {
        public static final String ENTER_EVENT_NAME = "enter";
        public static final String ENTER_TIP = "621.2.0.1.14007";
        public static final String ENTER_WAY_NAME1 = "全屏应用_左下角无极挂起";
        public static final String ENTER_WAY_NAME2 = "全屏应用_右下角无极挂起";
        public static final String ENTER_WAY_NAME3 = "小窗_左下角无极缩放";
        public static final String ENTER_WAY_NAME4 = "小窗_右下角无极缩放";
        public static final String ENTER_WAY_NAME5 = "控制中心";
        public static final String ENTER_WAY_UNPIN = "贴边呼出";
        public static final String MOVE_EVENT_NAME = "move";
        public static final String MOVE_TIP = "621.2.0.1.14008";
        public static final String PINED_EVENT_NAME = "hide_window";
        public static final String PINED_EXIT_EVENT_NAME = "quit_hidden_window";
        public static final String PINED_EXIT_TIP = "621.2.1.1.21752";
        public static final String PINED_MOVE_EVENT_NAME = "move_hidden_window";
        public static final String PINED_MOVE_TIP = "621.2.1.1.21750";
        public static final String PINED_TIP = "621.2.1.1.21748";
        public static final String QUIT_EVENT_NAME = "quit";
        public static final String QUIT_TIP = "621.2.0.1.14009";
        public static final String QUIT_WAY_NAME1 = "全屏";
        public static final String QUIT_WAY_NAME2 = "小窗";
        public static final String QUIT_WAY_NAME3 = "其他";
        public static final String QUIT_WAY_NAME4 = "拖拽进入小窗";
        public static final String QUIT_WAY_PIN = "进入迷你窗贴边";
    }

    /* loaded from: classes.dex */
    public static final class MultiFreeformTrackConstants {
        public static final String MULTI_FREEFORM_ENTER_NAME = "enter";
        public static final String MULTI_FREEFORM_ENTER_TIP = "621.4.0.1.21745";
    }

    /* loaded from: classes.dex */
    public static final class SmallWindowTrackConstants {
        public static final String CLICK_EVENT_NAME = "click";
        public static final String CLICK_TIP = "621.1.1.1.14014";
        public static final String ENTER_EVENT_NAME = "enter";
        public static final String ENTER_TIP = "621.1.0.1.14010";
        public static final String ENTER_WAY_NAME1 = "全屏应用_无极缩放_左下角";
        public static final String ENTER_WAY_NAME2 = "全屏应用_无极缩放_右下角";
        public static final String ENTER_WAY_NAME3 = "迷你窗";
        public static final String ENTER_WAY_NAME4 = "其他";
        public static final String ENTER_WAY_NAME5 = "拖拽迷你窗进入小窗";
        public static final String ENTER_WAY_UNPIN = "贴边呼出";
        public static final String MOVE_EVENT_NAME = "move";
        public static final String MOVE_TIP = "621.1.0.1.14011";
        public static final String PINED_EVENT_NAME = "hide_window";
        public static final String PINED_EXIT_EVENT_NAME = "quit_hidden_window";
        public static final String PINED_EXIT_TIP = "621.1.2.1.21751";
        public static final String PINED_MOVE_EVENT_NAME = "move_hidden_window";
        public static final String PINED_MOVE_TIP = "621.1.2.1.21749";
        public static final String PINED_TIP = "621.1.2.1.21747";
        public static final String PINED_WAY_ENTER_RECENT_TASK = "小窗进入最近任务";
        public static final String PINED_WAY_SLIDE = "拖拽小窗";
        public static final String QUIT_EVENT_NAME = "quit";
        public static final String QUIT_TIP = "621.1.0.1.14013";
        public static final String QUIT_WAY_NAME1 = "上滑";
        public static final String QUIT_WAY_NAME2 = "横屏时全屏";
        public static final String QUIT_WAY_NAME3 = "竖屏时全屏";
        public static final String QUIT_WAY_NAME4 = "进入迷你窗";
        public static final String QUIT_WAY_NAME5 = "拖拽小窗至全屏";
        public static final String QUIT_WAY_NAME6 = "其他";
        public static final String QUIT_WAY_PIN = "进入小窗贴边";
        public static final String RESIZE_EVENT_NAME = "resize";
        public static final String RESIZE_TIP = "621.1.0.1.14012";
        public static final String STACK_RATIO1 = "手机比例";
        public static final String STACK_RATIO2 = "游戏/视频比例";
        public static final String STACK_RATIO3 = "4:3";
        public static final String STACK_RATIO4 = "3:4";
    }

    public MiuiFreeformTrackManager(Context context, MiuiFreeFormGestureController controller) {
        this.mMiuiFreeFormGestureController = controller;
        this.mContext = context;
        bindOneTrackService();
    }

    public void bindOneTrackService() {
        if (this.mContext == null) {
            Slog.d(TAG, "Context == null");
            return;
        }
        synchronized (this.mFreeFormTrackLock) {
            if (this.mITrackBinder != null) {
                Slog.d(TAG, "aready bound");
                return;
            }
            try {
                Intent intent = new Intent();
                intent.setClassName("com.miui.analytics", SERVER_CLASS_NAME);
                this.mServiceConnection = new ServiceConnection() { // from class: com.android.server.wm.MiuiFreeformTrackManager.1
                    @Override // android.content.ServiceConnection
                    public void onServiceConnected(ComponentName componentName, IBinder service) {
                        synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                            MiuiFreeformTrackManager.this.mITrackBinder = ITrackBinder.Stub.asInterface(service);
                        }
                        Slog.d(MiuiFreeformTrackManager.TAG, "BindOneTrackService Success");
                    }

                    @Override // android.content.ServiceConnection
                    public void onServiceDisconnected(ComponentName componentName) {
                        MiuiFreeformTrackManager miuiFreeformTrackManager = MiuiFreeformTrackManager.this;
                        miuiFreeformTrackManager.closeOneTrackService(miuiFreeformTrackManager.mContext, this);
                        MiuiFreeformTrackManager.this.mHandler.removeCallbacks(MiuiFreeformTrackManager.this.mBindOneTrackService);
                        MiuiFreeformTrackManager.this.mHandler.postDelayed(MiuiFreeformTrackManager.this.mBindOneTrackService, 200L);
                        Slog.d(MiuiFreeformTrackManager.TAG, "OneTrack Service Disconnected");
                    }
                };
                Slog.d(TAG, "Start Bind OneTrack Service");
                this.mContext.bindService(intent, this.mServiceConnection, 1);
            } catch (Exception e) {
                e.printStackTrace();
                Slog.e(TAG, "Bind OneTrack Service Exception");
            }
        }
    }

    public void closeOneTrackService(Context context, ServiceConnection serviceConnection) {
        if (context == null) {
            Slog.d(TAG, "unBindOneTrackService: mContext == null ");
            return;
        }
        synchronized (this.mFreeFormTrackLock) {
            if (this.mITrackBinder != null && serviceConnection != null) {
                context.unbindService(serviceConnection);
                Slog.d(TAG, "unBindOneTrackService success");
            } else {
                Slog.d(TAG, "unBindOneTrackService failed: mServiceConnection == null");
            }
            this.mITrackBinder = null;
        }
    }

    public void trackMiniWindowEnterWayEvent(final String enterWay, final Point leftTopPosition, final String packageName, final String applicationName, final int freeformAppCount) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.3
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", MiniWindowTrackConstants.ENTER_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "enter");
                            jsonData.put("mini_window_enter_way", enterWay);
                            jsonData.put("pixel_x_location", leftTopPosition.x);
                            jsonData.put("pixel_y_location", leftTopPosition.y);
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            jsonData.put("window_num", freeformAppCount);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackMiniWindowMoveEvent(final Point startPosition, final Point endPosition, final String packageName, final String applicationName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.4
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", MiniWindowTrackConstants.MOVE_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "move");
                            jsonData.put("move_from_pixel_x", startPosition.x);
                            jsonData.put("move_from_pixel_y", startPosition.y);
                            jsonData.put("move_to_pixel_x", endPosition.x);
                            jsonData.put("move_to_pixel_y", endPosition.y);
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackMiniWindowQuitEvent(final String quitWay, final Point leftTopPosition, final String packageName, final String applicationName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.5
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", MiniWindowTrackConstants.QUIT_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "quit");
                            jsonData.put("mini_window_quit_way", quitWay);
                            jsonData.put("pixel_x_location", leftTopPosition.x);
                            jsonData.put("pixel_y_location", leftTopPosition.y);
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackSmallWindowEnterWayEvent(final String enterWay, final String smallWindowRatio, final String packageName, final String applicationName, final int freeformAppCount) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.6
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", SmallWindowTrackConstants.ENTER_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "enter");
                            jsonData.put("small_window_enter_way", enterWay);
                            jsonData.put("small_window_ratio", smallWindowRatio);
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            jsonData.put("window_num", freeformAppCount);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackSmallWindowMoveEvent(final String packageName, final String applicationName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.7
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", SmallWindowTrackConstants.MOVE_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "move");
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackSmallWindowResizeEvent(final String smallWindowFromRatio, final String smallWindowToRatio, final String packageName, final String applicationName, long useDuration) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.8
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", SmallWindowTrackConstants.RESIZE_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, SmallWindowTrackConstants.RESIZE_EVENT_NAME);
                            jsonData.put("small_window_from_ratio", smallWindowFromRatio);
                            jsonData.put("small_window_to_ratio", smallWindowToRatio);
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackSmallWindowQuitEvent(final String quitWay, final String smallWindowRatio, final String packageName, final String applicationName, long useDuration) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.9
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", SmallWindowTrackConstants.QUIT_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "quit");
                            jsonData.put("small_window_quit_way", quitWay);
                            jsonData.put("small_window_ratio", smallWindowRatio);
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackClickSmallWindowEvent(final String packageName, final String applicationName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.10
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", SmallWindowTrackConstants.CLICK_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, SmallWindowTrackConstants.CLICK_EVENT_NAME);
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackSmallWindowPinedEvent(final String packageName, final String applicationName, final String pinedWay, final int posX, final int posY) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.11
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", SmallWindowTrackConstants.PINED_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "hide_window");
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            jsonData.put("small_window_hide_way", pinedWay);
                            jsonData.put("hide_window_x_coordinate", posX);
                            jsonData.put("hide_window_y_coordinate", posY);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackMiniWindowPinedEvent(final String packageName, final String applicationName, final int posX, final int posY) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.12
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", MiniWindowTrackConstants.PINED_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "hide_window");
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            jsonData.put("hide_window_x_coordinate", posX);
                            jsonData.put("hide_window_y_coordinate", posY);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackSmallWindowPinedMoveEvent(final String packageName, final String applicationName, final int posX, final int posY) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.13
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", SmallWindowTrackConstants.PINED_MOVE_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "move_hidden_window");
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            jsonData.put("hide_window_x_coordinate", posX);
                            jsonData.put("hide_window_y_coordinate", posY);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackMiniWindowPinedMoveEvent(final String packageName, final String applicationName, final int posX, final int posY) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.14
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", MiniWindowTrackConstants.PINED_MOVE_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "move_hidden_window");
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            jsonData.put("hide_window_x_coordinate", posX);
                            jsonData.put("hide_window_y_coordinate", posY);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackSmallWindowPinedQuitEvent(final String packageName, final String applicationName, final float pinedDuration) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.15
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", SmallWindowTrackConstants.PINED_EXIT_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "quit_hidden_window");
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            jsonData.put("use_duration", pinedDuration);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackMiniWindowPinedQuitEvent(final String packageName, final String applicationName, final float pinedDuration) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.16
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", MiniWindowTrackConstants.PINED_EXIT_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "quit_hidden_window");
                            jsonData.put(DarkModeOneTrackHelper.PARAM_VALUE_APP_PKG, packageName);
                            jsonData.put("app_display_name", applicationName);
                            jsonData.put("use_duration", pinedDuration);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void trackMultiFreeformEnterEvent(final int nums) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformTrackManager.17
            @Override // java.lang.Runnable
            public void run() {
                synchronized (MiuiFreeformTrackManager.this.mFreeFormTrackLock) {
                    try {
                        if (MiuiFreeformTrackManager.this.mITrackBinder != null) {
                            JSONObject jsonData = new JSONObject();
                            jsonData.put("tip", MultiFreeformTrackConstants.MULTI_FREEFORM_ENTER_TIP);
                            MiuiFreeformTrackManager.this.putCommomParam(jsonData);
                            jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "enter");
                            jsonData.put("window_num", nums);
                            MiuiFreeformTrackManager.this.mITrackBinder.trackEvent(MiuiFreeformTrackManager.APP_ID, "android", jsonData.toString(), 3);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void putCommomParam(JSONObject jsonData) throws JSONException {
        String str;
        String str2;
        String str3;
        if (MiuiMultiWindowUtils.isPadScreen(this.mContext)) {
            str = CommonTrackConstants.DEVICE_TYPE_PAD;
        } else {
            str = CommonTrackConstants.DEVICE_TYPE_PHONE;
        }
        jsonData.put("model_type", str);
        if (this.mMiuiFreeFormGestureController.mGestureListener.mIsPortrait) {
            str2 = CommonTrackConstants.SCREEN_ORIENTATION_PORTRAIT;
        } else {
            str2 = CommonTrackConstants.SCREEN_ORIENTATION_LANDSCAPE;
        }
        jsonData.put("screen_orientation", str2);
        if (MiuiMultiWindowUtils.isFoldInnerScreen(this.mContext)) {
            str3 = CommonTrackConstants.SCREEN_TYPE_INNER;
        } else if (MiuiMultiWindowUtils.isFoldOuterScreen(this.mContext)) {
            str3 = CommonTrackConstants.SCREEN_TYPE_OUTTER;
        } else {
            str3 = CommonTrackConstants.SCREEN_TYPE_NOTHING;
        }
        jsonData.put("screen_type", str3);
        jsonData.put("data_version", CommonTrackConstants.DATA_VERSION);
    }
}
