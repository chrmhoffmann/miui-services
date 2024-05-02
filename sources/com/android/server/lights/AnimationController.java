package com.android.server.lights;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.view.WindowManager;
import com.android.server.lights.view.MessageView;
import com.android.server.lights.view.MusicView;
/* loaded from: classes.dex */
public class AnimationController {
    public static final int ANIMATION_TYPE_START_MESSAGE = 1;
    public static final int ANIMATION_TYPE_START_MUSIC = 2;
    public static final int ANIMATION_TYPE_STOP_MESSAGE = 3;
    public static final int ANIMATION_TYPE_STOP_MUSIC = 4;
    public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_MUSIC = 2;
    private boolean mAddedMessageAnimationView = false;
    private boolean mAddedMusicAnimationView = false;
    private Context mContext;
    private Handler mHandler;
    private MessageView mMessageView;
    private MusicView mMusicView;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private WindowManager mWindowManager;

    public AnimationController(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new H(looper);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mMessageView = new MessageView(this.mContext);
        this.mMusicView = new MusicView(this.mContext);
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mWakeLock = powerManager.newWakeLock(1, "u2-animation");
    }

    public void startAnimation(int type) {
        this.mHandler.sendEmptyMessage(type);
    }

    public void stopAnimation(int type) {
        this.mHandler.sendEmptyMessage(type);
    }

    public boolean isAnimationRunning(int type) {
        MessageView messageView;
        MusicView musicView;
        if (type == 2 && (musicView = this.mMusicView) != null) {
            return musicView.isAnimationRunning();
        }
        if (type == 1 && (messageView = this.mMessageView) != null) {
            return messageView.isAnimationRunning();
        }
        return false;
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            AnimationController.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case 1:
                    case 2:
                        AnimationController.this.mWakeLock.acquire(30000L);
                        if (AnimationController.this.mAddedMessageAnimationView) {
                            AnimationController.this.mWindowManager.removeView(AnimationController.this.mMessageView);
                            AnimationController.this.mAddedMessageAnimationView = false;
                        }
                        if (AnimationController.this.mAddedMusicAnimationView) {
                            AnimationController.this.mWindowManager.removeView(AnimationController.this.mMusicView);
                            AnimationController.this.mAddedMusicAnimationView = false;
                        }
                        WindowManager.LayoutParams layoutParams = AnimationController.this.getWindowParam();
                        if (msg.what == 2) {
                            AnimationController.this.mWindowManager.addView(AnimationController.this.mMusicView, layoutParams);
                            AnimationController.this.mMusicView.startAnimation();
                            AnimationController.this.mAddedMusicAnimationView = true;
                            return;
                        } else if (msg.what == 1) {
                            AnimationController.this.mWindowManager.addView(AnimationController.this.mMessageView, layoutParams);
                            AnimationController.this.mMessageView.startAnimation();
                            AnimationController.this.mAddedMessageAnimationView = true;
                            return;
                        } else {
                            return;
                        }
                    case 3:
                    case 4:
                        if (AnimationController.this.mAddedMessageAnimationView && msg.what == 3) {
                            AnimationController.this.mMessageView.stopAnimation();
                            AnimationController.this.mWindowManager.removeView(AnimationController.this.mMessageView);
                            AnimationController.this.mAddedMessageAnimationView = false;
                        }
                        if (AnimationController.this.mAddedMusicAnimationView && msg.what == 4) {
                            AnimationController.this.mMusicView.stopAnimation();
                            AnimationController.this.mWindowManager.removeView(AnimationController.this.mMusicView);
                            AnimationController.this.mAddedMusicAnimationView = false;
                        }
                        if (AnimationController.this.mWakeLock.isHeld()) {
                            AnimationController.this.mWakeLock.release();
                            return;
                        }
                        return;
                    default:
                        return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public WindowManager.LayoutParams getWindowParam() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2060, 17448, -3);
        lp.setTitle("U2-Animation");
        lp.screenOrientation = 1;
        lp.windowAnimations = 0;
        lp.x = 0;
        lp.y = 0;
        lp.width = 2048;
        lp.height = 2250;
        return lp;
    }
}
