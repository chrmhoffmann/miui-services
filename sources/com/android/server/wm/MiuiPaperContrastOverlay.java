package com.android.server.wm;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BLASTBufferQueue;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.devicestate.DeviceStateManager;
import android.hardware.display.DisplayManagerInternal;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.IDisplayWindowListener;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import com.android.server.LocalServices;
import com.android.server.wm.MiuiPaperContrastOverlay;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import miui.android.animation.controller.AnimState;
/* loaded from: classes.dex */
public class MiuiPaperContrastOverlay {
    private static final boolean DEBUG = true;
    public static final boolean IS_FOLDABLE_DEVICE;
    private static final String TAG = "MiuiPaperContrastOverlay";
    private static volatile MiuiPaperContrastOverlay mContrastOverlay;
    private SurfaceControl mBLASTSurfaceControl;
    private BLASTBufferQueue mBlastBufferQueue;
    private Context mContext;
    private boolean mCreatedResources;
    private float mDarkAlpha;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private ShortBuffer mIndexBuffer;
    private final Configuration mLastConfiguration;
    private int mLastDisplayHeight;
    private int mLastDisplayWidth;
    private int mLastLevel;
    private int mLastRandSeed;
    private boolean mNeedShowPaperSurface;
    private boolean mPrepared;
    private int mProgram;
    private int mRandSeed;
    private IntBuffer mRandomTexBuffer;
    private Surface mSurface;
    private SurfaceControl mSurfaceControl;
    private int mSurfaceHeight;
    private PaperSurfaceLayout mSurfaceLayout;
    private SurfaceSession mSurfaceSession;
    private int mSurfaceWidth;
    private FloatBuffer mTextureBuffer;
    private FloatBuffer mVertexBuffer;
    private WindowManagerService mWms;
    private int samplerLocA;
    private int[] mTextureId = new int[1];
    private float mPaperAlpha = 0.06666667f;
    private boolean mDarkMode = false;
    private boolean mPaperMode = false;
    private int mPaperNoiseLevel = 17;
    private IDisplayWindowListener mDisplayWindowListener = new AnonymousClass2();
    private final DisplayManagerInternal mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
    private boolean mFoldDeviceReady = false;

    static {
        boolean z = false;
        if (SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2) {
            z = true;
        }
        IS_FOLDABLE_DEVICE = z;
    }

    public static MiuiPaperContrastOverlay getInstance(WindowManagerService service, Context context) {
        if (mContrastOverlay == null) {
            synchronized (MiuiPaperContrastOverlay.class) {
                if (mContrastOverlay == null) {
                    mContrastOverlay = new MiuiPaperContrastOverlay(service, context);
                }
            }
        }
        return mContrastOverlay;
    }

    private MiuiPaperContrastOverlay(WindowManagerService service, Context context) {
        this.mWms = service;
        this.mContext = context;
        updateDisplaySize();
        this.mLastConfiguration = new Configuration(this.mContext.getResources().getConfiguration());
        this.mWms.registerDisplayWindowListener(this.mDisplayWindowListener);
        if (IS_FOLDABLE_DEVICE) {
            ((DeviceStateManager) this.mContext.getSystemService(DeviceStateManager.class)).registerCallback(this.mContext.getMainExecutor(), new AnonymousClass1());
        }
        createSurface();
    }

    /* renamed from: com.android.server.wm.MiuiPaperContrastOverlay$1 */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements DeviceStateManager.DeviceStateCallback {
        AnonymousClass1() {
            MiuiPaperContrastOverlay.this = this$0;
        }

        public void onSupportedStatesChanged(int[] supportedStates) {
        }

        public void onBaseStateChanged(int state) {
        }

        public void onStateChanged(int state) {
            if (state != 0 && MiuiPaperContrastOverlay.this.mNeedShowPaperSurface && MiuiPaperContrastOverlay.this.mFoldDeviceReady) {
                MiuiPaperContrastOverlay.this.mWms.mH.post(new Runnable() { // from class: com.android.server.wm.MiuiPaperContrastOverlay$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiPaperContrastOverlay.AnonymousClass1.this.m1821x628c16cf();
                    }
                });
            }
        }

        /* renamed from: lambda$onStateChanged$0$com-android-server-wm-MiuiPaperContrastOverlay$1 */
        public /* synthetic */ void m1821x628c16cf() {
            MiuiPaperContrastOverlay.this.showPaperModeSurface();
        }
    }

    public void changeShowLayerStatus(boolean needShow) {
        this.mNeedShowPaperSurface = needShow;
    }

    public void changeDeviceReady() {
        this.mFoldDeviceReady = true;
    }

    public void updateDisplaySize() {
        int i;
        int i2;
        this.mLastDisplayWidth = this.mSurfaceWidth;
        this.mLastDisplayHeight = this.mSurfaceHeight;
        Point displaySize = new Point();
        this.mWms.getBaseDisplaySize(0, displaySize);
        if (displaySize.x != 0 && displaySize.y != 0) {
            this.mSurfaceWidth = displaySize.x;
            this.mSurfaceHeight = displaySize.y;
        } else {
            DisplayInfo defaultInfo = this.mWms.getDefaultDisplayContentLocked().getDisplayInfo();
            if (defaultInfo.logicalWidth > defaultInfo.logicalHeight) {
                i = defaultInfo.logicalHeight;
            } else {
                i = defaultInfo.logicalWidth;
            }
            this.mSurfaceWidth = i;
            if (defaultInfo.logicalWidth > defaultInfo.logicalHeight) {
                i2 = defaultInfo.logicalWidth;
            } else {
                i2 = defaultInfo.logicalHeight;
            }
            this.mSurfaceHeight = i2;
        }
        Slog.d(TAG, "Update surface (" + displaySize.x + ", " + displaySize.y + ") from (" + this.mLastDisplayWidth + ", " + this.mLastDisplayHeight + ") to (" + this.mSurfaceWidth + "," + this.mSurfaceHeight + ") on thread: " + Thread.currentThread().getName() + ", " + this.mFoldDeviceReady);
    }

    private void clearEglAndSurface() {
        Slog.i(TAG, "clearEglAndSurface, egl display is " + this.mEglDisplay + ", egl context is " + this.mEglContext);
        detachEglContext();
        destroyRandomTexture();
        destroyGLShaders();
        EGLContext eGLContext = this.mEglContext;
        if (eGLContext != null) {
            EGL14.eglDestroyContext(this.mEglDisplay, eGLContext);
            this.mEglContext = null;
        }
        EGLDisplay eGLDisplay = this.mEglDisplay;
        if (eGLDisplay != null) {
            EGL14.eglTerminate(eGLDisplay);
            this.mEglDisplay = null;
        }
        dismiss();
    }

    public boolean isSizeChangeHappened() {
        int i = this.mLastDisplayHeight;
        return ((i == this.mSurfaceHeight && this.mLastDisplayWidth == this.mSurfaceWidth) || i == 0) ? false : true;
    }

    /* renamed from: com.android.server.wm.MiuiPaperContrastOverlay$2 */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends IDisplayWindowListener.Stub {
        AnonymousClass2() {
            MiuiPaperContrastOverlay.this = this$0;
        }

        public void onDisplayConfigurationChanged(final int displayId, final Configuration newConfig) {
            if (MiuiPaperContrastOverlay.this.mContext.getDisplayId() == displayId || !MiuiPaperContrastOverlay.this.mFoldDeviceReady) {
                MiuiPaperContrastOverlay.this.mFoldDeviceReady = true;
                MiuiPaperContrastOverlay.this.mWms.mH.post(new Runnable() { // from class: com.android.server.wm.MiuiPaperContrastOverlay$2$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiPaperContrastOverlay.AnonymousClass2.this.m1822x2816de4b(newConfig, displayId);
                    }
                });
            }
        }

        /* renamed from: lambda$onDisplayConfigurationChanged$0$com-android-server-wm-MiuiPaperContrastOverlay$2 */
        public /* synthetic */ void m1822x2816de4b(Configuration newConfig, int displayId) {
            MiuiPaperContrastOverlay.this.updateDisplaySize();
            int diff = newConfig.diff(MiuiPaperContrastOverlay.this.mLastConfiguration);
            boolean isChanged = MiuiPaperContrastOverlay.this.isSizeChangeHappened();
            Rect lastBounds = MiuiPaperContrastOverlay.this.mLastConfiguration.windowConfiguration.getBounds();
            Rect newBounds = newConfig.windowConfiguration.getBounds();
            Slog.i(MiuiPaperContrastOverlay.TAG, "Config size change is " + isChanged + ", from [" + lastBounds + ", " + MiuiPaperContrastOverlay.this.mLastConfiguration.windowConfiguration.getRotation() + "] to [" + newBounds + ", " + newConfig.windowConfiguration.getRotation() + "], " + (diff & 1024) + ", " + displayId);
            if (((diff & 128) == 0 && isChanged) || (lastBounds.height() != newBounds.width() && lastBounds.width() != newBounds.height() && (diff & 1024) != 0 && !newBounds.equals(lastBounds))) {
                MiuiPaperContrastOverlay.this.destroySurface();
                if (MiuiPaperContrastOverlay.this.mNeedShowPaperSurface) {
                    MiuiPaperContrastOverlay.this.createSurface();
                    MiuiPaperContrastOverlay.this.showPaperModeSurface();
                }
            }
            MiuiPaperContrastOverlay.this.mLastConfiguration.setTo(newConfig);
        }

        public void onDisplayAdded(int displayId) {
        }

        public void onDisplayRemoved(int displayId) {
        }

        public void onFixedRotationStarted(int displayId, int newRotation) {
        }

        public void onFixedRotationFinished(int displayId) {
        }

        public void onKeepClearAreasChanged(int displayId, List<Rect> restricted, List<Rect> unrestricted) {
        }
    }

    public void showPaperModeSurface() {
        if (IS_FOLDABLE_DEVICE && !this.mFoldDeviceReady) {
            Slog.i(TAG, "The fold device is not ready, paper-mode return.");
            return;
        }
        if (isSizeChangeHappened()) {
            destroySurface();
        }
        if (this.mSurfaceControl == null) {
            createSurface();
        }
        int level = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_texture_eyecare_level", MiuiSettings.ScreenEffect.DEFAULT_TEXTURE_EYECARE_LEVEL, -2);
        this.mPaperMode = true;
        this.mPaperNoiseLevel = level;
        this.mPaperAlpha = level / 255.0f;
        Slog.d(TAG, "Paper-mode surface params. Alpha: " + this.mPaperAlpha + ", Level from " + this.mLastLevel + " to " + level);
        if (level != this.mLastLevel) {
            showSurface();
        }
        this.mLastLevel = level;
    }

    public void hidePaperModeSurface() {
        this.mPaperMode = false;
        this.mLastLevel = 0;
        this.mRandSeed = 0;
        if (!this.mDarkMode) {
            Slog.d(TAG, "Hide paper-mode surface. Dark-mode is disable.");
            ((SurfaceControl.Transaction) this.mWms.mTransactionFactory.get()).hide(this.mSurfaceControl).apply();
            Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_paper_layer_show", 0, -2);
            return;
        }
        drawSurface(this.mDarkAlpha);
    }

    private void showSurface() {
        if (!this.mPrepared) {
            Slog.d(TAG, "Surface is not prepared ready.");
        } else if (!attachEglContext()) {
            Slog.d(TAG, "attachEglContext failed");
        } else {
            float f = this.mDarkAlpha;
            int i = this.mPaperNoiseLevel;
            if (f > i / 255.0f && this.mDarkMode && this.mPaperMode) {
                this.mRandSeed = i;
                drawSurface(f);
            } else if (this.mPaperMode) {
                this.mRandSeed = i;
                drawSurface(this.mPaperAlpha);
            } else if (this.mDarkMode) {
                this.mRandSeed = 0;
                drawSurface(f);
            } else {
                this.mRandSeed = 0;
                drawSurface(MiuiFreeformPinManagerService.EDGE_AREA);
            }
            SurfaceControl.Transaction t = (SurfaceControl.Transaction) this.mWms.mTransactionFactory.get();
            InputMonitor.setTrustedOverlayInputInfo(this.mSurfaceControl, t, this.mWms.getDefaultDisplayContentLocked().getDisplayId(), TAG);
            InputMonitor.setTrustedOverlayInputInfo(this.mBLASTSurfaceControl, t, this.mWms.getDefaultDisplayContentLocked().getDisplayId(), TAG);
            t.show(this.mSurfaceControl).apply();
            Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_paper_layer_show", 1, -2);
        }
    }

    private void drawSurface(float alpha) {
        Slog.d(TAG, "Draw surface, alpha is " + alpha + " and randSeed is " + this.mRandSeed);
        updateGLShaders(alpha);
        if (checkGlErrors("updateGLShaders")) {
            Slog.d(TAG, "Draw surface updateGLShaders failed");
            detachEglContext();
            this.mDarkMode = false;
            this.mPaperMode = false;
            dismiss();
            return;
        }
        drawFrame(alpha);
        if (checkGlErrors("drawFrame")) {
            Slog.d(TAG, "Draw frame failed");
            detachEglContext();
            this.mDarkMode = false;
            this.mPaperMode = false;
            dismiss();
            return;
        }
        EGL14.eglSwapBuffers(this.mEglDisplay, this.mEglSurface);
    }

    public void createSurface() {
        if (this.mSurfaceSession == null) {
            this.mSurfaceSession = new SurfaceSession();
        }
        Slog.d(TAG, "Create surface size: " + this.mSurfaceWidth + ", " + this.mSurfaceHeight + ", " + this.mSurfaceControl);
        if (this.mSurfaceControl == null) {
            try {
                SurfaceControl.Builder builder = new SurfaceControl.Builder(this.mSurfaceSession).setName(TAG);
                builder.setName(TAG).setFormat(1).setBufferSize(this.mSurfaceWidth, this.mSurfaceHeight).setContainerLayer().setParent(this.mWms.getDefaultDisplayContentLocked().getSurfaceControl()).build();
                this.mSurfaceControl = builder.build();
            } catch (Surface.OutOfResourcesException ex) {
                Slog.e(TAG, "Unable to create surface.", ex);
            }
            SurfaceControl.Transaction t = (SurfaceControl.Transaction) this.mWms.mTransactionFactory.get();
            t.setWindowCrop(this.mSurfaceControl, this.mSurfaceWidth, this.mSurfaceHeight);
            t.setLayer(this.mSurfaceControl, AnimState.VIEW_SIZE);
            t.show(this.mSurfaceControl);
            SurfaceControl.Builder b = new SurfaceControl.Builder().setName(TAG).setParent(this.mSurfaceControl).setHidden(false).setBLASTLayer();
            this.mBLASTSurfaceControl = b.build();
            BLASTBufferQueue bLASTBufferQueue = new BLASTBufferQueue(TAG, this.mBLASTSurfaceControl, this.mSurfaceWidth, this.mSurfaceHeight, -3);
            this.mBlastBufferQueue = bLASTBufferQueue;
            this.mSurface = bLASTBufferQueue.createSurface();
            PaperSurfaceLayout paperSurfaceLayout = new PaperSurfaceLayout(this.mDisplayManagerInternal, this.mSurfaceControl);
            this.mSurfaceLayout = paperSurfaceLayout;
            paperSurfaceLayout.onDisplayTransaction(t);
            t.apply();
        }
        updateDisplaySize();
        initEgl();
    }

    private void initEgl() {
        if (!createEglContext() || !createEglSurface()) {
            Slog.d(TAG, "createEglContext failed");
            dismiss();
        } else if (!attachEglContext()) {
            Slog.d(TAG, "attachEglContext failed");
        } else {
            try {
                if (!initGLShaders() || checkGlErrors("initEgl")) {
                    detachEglContext();
                    dismiss();
                    return;
                }
                detachEglContext();
                this.mCreatedResources = true;
                this.mPrepared = true;
            } finally {
                detachEglContext();
            }
        }
    }

    private int loadShader(int type) {
        String source;
        if (type == 35633) {
            source = "#version 300 es                                              \nlayout(location = 0) in vec4 a_position;                   \nlayout(location = 1) in vec2 a_texCoord;                   \nout vec2 v_texCoord;                                       \nvoid main()                                                \n{                                                          \n    gl_Position = a_position;                              \n    v_texCoord = a_texCoord;                               \n}                                                          \n";
        } else {
            source = "#version 300 es                                              \nprecision mediump float;                                   \nin vec2 v_texCoord;                                        \nuniform float s_Alpha;                                     \nlayout(location = 0) out vec4 outColor;                    \nuniform sampler2D s_RandTex;                               \nvoid main()                                                \n{                                                          \n    outColor.r =  texture(s_RandTex, v_texCoord).x;        \n    outColor.g =  texture(s_RandTex, v_texCoord).x;        \n    outColor.b =  1.1f * texture(s_RandTex, v_texCoord).x; \n    outColor.a =  s_Alpha; \n}                                                          \n";
        }
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, 35713, compiled, 0);
        if (compiled[0] == 0) {
            Slog.e(TAG, "Could not compile shader: " + shader + ", " + type + ":");
            Slog.e(TAG, GLES20.glGetShaderSource(shader));
            Slog.e(TAG, GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        checkGlErrors("loadShader");
        return shader;
    }

    private boolean initGLShaders() {
        int vshader = loadShader(35633);
        int fshader = loadShader(35632);
        GLES20.glReleaseShaderCompiler();
        if (vshader == 0 || fshader == 0) {
            return false;
        }
        int glCreateProgram = GLES20.glCreateProgram();
        this.mProgram = glCreateProgram;
        GLES20.glAttachShader(glCreateProgram, vshader);
        GLES20.glAttachShader(this.mProgram, fshader);
        GLES20.glDeleteShader(vshader);
        GLES20.glDeleteShader(fshader);
        GLES20.glLinkProgram(this.mProgram);
        GLES20.glUseProgram(this.mProgram);
        checkGlErrors("glUseProgram");
        Slog.d(TAG, "initGLShader start");
        float[] vCoord = {-1.0f, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA, -1.0f, -1.0f, MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, -1.0f, MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA};
        float[] tCoord = {MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, 1.0f, 1.0f, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA};
        short[] indices = {0, 1, 2, 0, 2, 3};
        FloatBuffer put = ByteBuffer.allocateDirect(vCoord.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vCoord);
        this.mVertexBuffer = put;
        put.position(0);
        FloatBuffer put2 = ByteBuffer.allocateDirect(tCoord.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(tCoord);
        this.mTextureBuffer = put2;
        put2.position(0);
        ShortBuffer put3 = ByteBuffer.allocateDirect(indices.length * 4).order(ByteOrder.nativeOrder()).asShortBuffer().put(indices);
        this.mIndexBuffer = put3;
        put3.position(0);
        requestRandomBuffer();
        GLES20.glGenTextures(1, this.mTextureId, 0);
        GLES20.glBindTexture(3553, this.mTextureId[0]);
        GLES20.glPixelStorei(3317, 1);
        GLES20.glTexParameteri(3553, 10241, 9728);
        GLES20.glTexParameteri(3553, 10240, 9728);
        GLES20.glTexParameteri(3553, 10242, 10497);
        GLES20.glTexParameteri(3553, 10243, 10497);
        GLES20.glViewport(0, 0, this.mSurfaceWidth, this.mSurfaceHeight);
        return true;
    }

    private void requestRandomBuffer() {
        this.mRandomTexBuffer = null;
        this.mRandomTexBuffer = ByteBuffer.allocateDirect(this.mSurfaceWidth * this.mSurfaceHeight * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        Slog.d(TAG, "Request random buffer (" + this.mSurfaceWidth + ", " + this.mSurfaceHeight + "), pixels: " + this.mRandomTexBuffer.limit());
        this.mRandomTexBuffer.position(0);
    }

    private void destroyRandomTexture() {
        GLES20.glDeleteTextures(1, this.mTextureId, 0);
        this.mTextureBuffer = null;
        this.mVertexBuffer = null;
        this.mIndexBuffer = null;
        this.mRandomTexBuffer = null;
        checkGlErrors("glDeleteTextures");
    }

    private void createRandTexture() {
        Slog.d(TAG, "createRandTexture randSeed: " + this.mRandSeed + ", lastRandSeed: " + this.mLastRandSeed);
        int i = this.mRandSeed;
        if (i != 0) {
            long begin = System.currentTimeMillis();
            Slog.i(TAG, "createRandTexture begin, size (" + this.mSurfaceWidth + "," + this.mSurfaceHeight + "), pixels: " + this.mRandomTexBuffer.limit());
            if (this.mSurfaceWidth * this.mSurfaceHeight > this.mRandomTexBuffer.limit()) {
                requestRandomBuffer();
            }
            for (int i2 = 0; i2 < this.mSurfaceWidth * this.mSurfaceHeight; i2++) {
                this.mRandomTexBuffer.put(i2, ThreadLocalRandom.current().nextInt(this.mRandSeed));
            }
            long end = System.currentTimeMillis();
            Slog.i(TAG, "createRandTexture end, duration: " + (end - begin));
        } else if (i == 0 && this.mLastRandSeed != 0) {
            for (int i3 = 0; i3 < this.mSurfaceWidth * this.mSurfaceHeight; i3++) {
                this.mRandomTexBuffer.put(i3, 0);
            }
        }
        updateDisplaySize();
        this.mLastRandSeed = this.mRandSeed;
        GLES20.glBindTexture(3553, this.mTextureId[0]);
        GLES20.glTexImage2D(3553, 0, 6408, this.mSurfaceWidth, this.mSurfaceHeight, 0, 6408, 5121, this.mRandomTexBuffer);
        checkGlErrors("createRandTexture");
    }

    private void updateGLShaders(float alpha) {
        Slog.d(TAG, "updateGLShaders start");
        createRandTexture();
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, this.mTextureId[0]);
        int samplerLocR = GLES20.glGetUniformLocation(this.mProgram, "s_RandTex");
        this.samplerLocA = GLES20.glGetUniformLocation(this.mProgram, "s_Alpha");
        GLES20.glUniform1i(samplerLocR, 0);
        GLES20.glUniform1f(this.samplerLocA, alpha);
    }

    private void destroyGLShaders() {
        GLES20.glDeleteProgram(this.mProgram);
        checkGlErrors("glDeleteProgram");
    }

    private void drawFrame(float alpha) {
        GLES20.glClearColor(MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, alpha);
        GLES20.glViewport(0, 0, this.mSurfaceWidth, this.mSurfaceHeight);
        GLES20.glVertexAttrib1f(this.samplerLocA, alpha);
        GLES20.glVertexAttribPointer(0, 3, 5126, false, 12, (Buffer) this.mVertexBuffer);
        GLES20.glVertexAttribPointer(1, 2, 5126, false, 8, (Buffer) this.mTextureBuffer);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);
        GLES20.glClear(16640);
        GLES20.glDrawElements(4, 6, 5123, this.mIndexBuffer);
    }

    private void dismiss() {
        Slog.d(TAG, "dismiss " + this.mPrepared);
        if (this.mPrepared) {
            dismissResources();
            destroySurface();
            this.mPrepared = false;
        }
    }

    public void destroySurface() {
        Slog.d(TAG, "destroySurface surfaceControl: " + (this.mSurfaceControl != null));
        if (this.mSurfaceControl != null) {
            Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_paper_layer_show", 0, -2);
            this.mRandSeed = 0;
            this.mLastLevel = 0;
            this.mSurfaceLayout.dispose();
            this.mSurfaceLayout = null;
            ((SurfaceControl.Transaction) this.mWms.mTransactionFactory.get()).remove(this.mSurfaceControl).apply();
            Surface surface = this.mSurface;
            if (surface != null) {
                surface.release();
                this.mSurface = null;
            }
            SurfaceControl surfaceControl = this.mBLASTSurfaceControl;
            if (surfaceControl != null) {
                surfaceControl.release();
                this.mBLASTSurfaceControl = null;
                this.mBlastBufferQueue.destroy();
                this.mBlastBufferQueue = null;
            }
            this.mSurfaceControl = null;
        }
    }

    private void dismissResources() {
        Slog.d(TAG, "dismissResources");
        if (this.mCreatedResources) {
            attachEglContext();
            try {
                destroyRandomTexture();
                destroyGLShaders();
                destroyEglSurface();
                detachEglContext();
                GLES20.glFlush();
                this.mCreatedResources = false;
            } catch (Throwable th) {
                detachEglContext();
                throw th;
            }
        }
    }

    private boolean createEglContext() {
        if (this.mEglDisplay == null) {
            EGLDisplay eglGetDisplay = EGL14.eglGetDisplay(0);
            this.mEglDisplay = eglGetDisplay;
            if (eglGetDisplay == EGL14.EGL_NO_DISPLAY) {
                logEglError("eglGetDisplay");
                return false;
            }
            int[] version = new int[2];
            if (!EGL14.eglInitialize(this.mEglDisplay, version, 0, version, 1)) {
                this.mEglDisplay = null;
                logEglError("eglInitialize");
                return false;
            }
        }
        if (this.mEglConfig == null) {
            int[] eglConfigAttribList = {12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 8, 12344};
            int[] numEglConfigs = new int[1];
            EGLConfig[] eglConfigs = new EGLConfig[1];
            if (!EGL14.eglChooseConfig(this.mEglDisplay, eglConfigAttribList, 0, eglConfigs, 0, eglConfigs.length, numEglConfigs, 0)) {
                logEglError("eglChooseConfig");
                return false;
            } else if (numEglConfigs[0] <= 0) {
                Slog.e(TAG, "no valid config found");
                return false;
            } else {
                this.mEglConfig = eglConfigs[0];
            }
        }
        EGLContext eGLContext = this.mEglContext;
        if (eGLContext != null) {
            EGL14.eglDestroyContext(this.mEglDisplay, eGLContext);
            this.mEglContext = null;
        }
        if (this.mEglContext == null) {
            int[] eglContextAttribList = {12440, 2, 12344};
            EGLContext eglCreateContext = EGL14.eglCreateContext(this.mEglDisplay, this.mEglConfig, EGL14.EGL_NO_CONTEXT, eglContextAttribList, 0);
            this.mEglContext = eglCreateContext;
            if (eglCreateContext == null) {
                logEglError("eglCreateContext");
                return false;
            }
        }
        return true;
    }

    private boolean createEglSurface() {
        EGLSurface eGLSurface = this.mEglSurface;
        if (eGLSurface != null) {
            EGL14.eglDestroySurface(this.mEglDisplay, eGLSurface);
            this.mEglSurface = null;
        }
        if (this.mEglSurface == null) {
            int[] eglSurfaceAttribList = {12344};
            EGLSurface eglCreateWindowSurface = EGL14.eglCreateWindowSurface(this.mEglDisplay, this.mEglConfig, this.mSurface, eglSurfaceAttribList, 0);
            this.mEglSurface = eglCreateWindowSurface;
            if (eglCreateWindowSurface == null) {
                logEglError("eglCreateWindowSurface");
                return false;
            }
        }
        return true;
    }

    private void destroyEglSurface() {
        EGLSurface eGLSurface = this.mEglSurface;
        if (eGLSurface != null) {
            if (!EGL14.eglDestroySurface(this.mEglDisplay, eGLSurface)) {
                logEglError("eglDestroySurface");
            }
            this.mEglSurface = null;
        }
    }

    private boolean attachEglContext() {
        EGLContext eGLContext;
        EGLSurface eGLSurface = this.mEglSurface;
        if (eGLSurface == null || (eGLContext = this.mEglContext) == null) {
            return false;
        }
        if (!EGL14.eglMakeCurrent(this.mEglDisplay, eGLSurface, eGLSurface, eGLContext)) {
            logEglError("eglMakeCurrent");
            return false;
        }
        return true;
    }

    private void detachEglContext() {
        EGLDisplay eGLDisplay = this.mEglDisplay;
        if (eGLDisplay != null) {
            EGL14.eglMakeCurrent(eGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        }
    }

    private static void logEglError(String func) {
        Slog.e(TAG, func + " failed: error " + EGL14.eglGetError(), new Throwable());
    }

    private static boolean checkGlErrors(String func) {
        return checkGlErrors(func, true);
    }

    private static boolean checkGlErrors(String func, boolean log) {
        boolean hadError = false;
        while (true) {
            int error = GLES20.glGetError();
            if (error != 0) {
                if (log) {
                    Slog.e(TAG, func + " failed: error " + error, new Throwable());
                }
                hadError = true;
            } else {
                return hadError;
            }
        }
    }

    /* loaded from: classes.dex */
    public final class PaperSurfaceLayout implements DisplayManagerInternal.DisplayTransactionListener {
        private final DisplayManagerInternal mDisplayManagerInternal;
        private SurfaceControl mSurfaceControl;

        public PaperSurfaceLayout(DisplayManagerInternal displayManagerInternal, SurfaceControl surfaceControl) {
            MiuiPaperContrastOverlay.this = r1;
            this.mDisplayManagerInternal = displayManagerInternal;
            this.mSurfaceControl = surfaceControl;
            displayManagerInternal.registerDisplayTransactionListener(this);
        }

        public void dispose() {
            synchronized (this) {
                this.mSurfaceControl = null;
            }
            this.mDisplayManagerInternal.unregisterDisplayTransactionListener(this);
        }

        public void onDisplayTransaction(SurfaceControl.Transaction t) {
            synchronized (this) {
                if (this.mSurfaceControl == null) {
                    return;
                }
                DisplayInfo displayInfo = this.mDisplayManagerInternal.getDisplayInfo(0);
                if (displayInfo == null) {
                    return;
                }
                switch (displayInfo.rotation) {
                    case 0:
                        t.setPosition(this.mSurfaceControl, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA);
                        t.setMatrix(this.mSurfaceControl, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
                        break;
                    case 1:
                        t.setPosition(this.mSurfaceControl, MiuiFreeformPinManagerService.EDGE_AREA, displayInfo.logicalHeight);
                        t.setMatrix(this.mSurfaceControl, MiuiFreeformPinManagerService.EDGE_AREA, -1.0f, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA);
                        break;
                    case 2:
                        t.setPosition(this.mSurfaceControl, displayInfo.logicalWidth, displayInfo.logicalHeight);
                        t.setMatrix(this.mSurfaceControl, -1.0f, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, -1.0f);
                        break;
                    case 3:
                        t.setPosition(this.mSurfaceControl, displayInfo.logicalWidth, MiuiFreeformPinManagerService.EDGE_AREA);
                        t.setMatrix(this.mSurfaceControl, MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, -1.0f, MiuiFreeformPinManagerService.EDGE_AREA);
                        break;
                }
            }
        }
    }
}
