package com.android.server.lights;

import android.media.audiofx.Visualizer;
import com.android.internal.util.ArrayUtils;
/* loaded from: classes.dex */
public class VisualizerHolder {
    int lastMax = -1;
    OnDataCaptureListener onDataCaptureListener;
    private Visualizer visualizer;
    private static final VisualizerHolder ourInstance = new VisualizerHolder();
    private static final String[] mMusicWhiteList = {"com.miui.player:remote", "com.netease.cloudmusic:play", "com.tencent.qqmusic:QQPlayerService", "fm.xiami.main.player", "com.kugou.android.support", "cn.kuwo.player:service", "com.tencent.blackkey:player", "cmccwm.mobilemusic", "com.ting.mp3.android", "com.kugou.android.support.ktvapp", "com.blueocean.musicplayer", "com.kugou.android.ringtone:player", "com.shoujiduoduo.dj", "com.changba", "com.shoujiduoduo.ringtone", "com.hiby.music", "com.miui.player:remote", "com.google.android.music:main", "com.tencent.ibg.joox", "com.skysoft.kkbox.android", "com.sofeh.android.musicstudio3", "com.gamestar.perfectpiano", "com.opalastudios.pads", "com.magix.android.mmjam", "com.musicplayer.playermusic:main", "com.gaana", "com.maxmpz.audioplayer", "com.melodis.midomiMusicIdentifier.freemium", "com.mixvibes.remixlive", "com.starmakerinteractive.starmaker", "com.smule.singandroid", "com.djit.apps.stream", "tunein.service", "com.shazam.android", "com.jangomobile.android", "com.pandoralite", "com.tube.hqmusic", "com.amazon.avod.thirdpartyclient", "com.atmusic.app", "com.rubycell.pianisthd", "com.agminstruments.drumpadmachine", "com.playermusic.musicplayerapp", "com.famousbluemedia.piano", "com.apple.android.music", "mb32r.musica.gratis.music.player.free.download", "com.famousbluemedia.yokee", "com.ss.android.ugc.trill"};

    /* loaded from: classes.dex */
    public interface OnDataCaptureListener {
        void onFrequencyCapture(int i, float[] fArr);
    }

    private VisualizerHolder() {
    }

    public static VisualizerHolder getInstance() {
        return ourInstance;
    }

    private Visualizer getVisualizer() {
        if (this.visualizer == null) {
            try {
                Visualizer visualizer = new Visualizer(0);
                this.visualizer = visualizer;
                visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return this.visualizer;
    }

    public void setOnDataCaptureListener(final OnDataCaptureListener onDataCaptureListener) {
        this.onDataCaptureListener = onDataCaptureListener;
        if (getVisualizer() == null) {
            return;
        }
        getVisualizer().setDataCaptureListener(new Visualizer.OnDataCaptureListener() { // from class: com.android.server.lights.VisualizerHolder.1
            @Override // android.media.audiofx.Visualizer.OnDataCaptureListener
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            }

            @Override // android.media.audiofx.Visualizer.OnDataCaptureListener
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                if (ArrayUtils.isEmpty(fft)) {
                    return;
                }
                float[] magnitudes = new float[fft.length / 2];
                int max = 0;
                for (int i = 0; i < magnitudes.length; i++) {
                    magnitudes[i] = (float) Math.hypot(fft[i * 2], fft[(i * 2) + 1]);
                    if (magnitudes[max] < magnitudes[i]) {
                        max = i;
                    }
                }
                if (VisualizerHolder.this.lastMax == max) {
                    return;
                }
                VisualizerHolder.this.lastMax = max;
                onDataCaptureListener.onFrequencyCapture((max * samplingRate) / fft.length, magnitudes);
            }
        }, Visualizer.getMaxCaptureRate() / 2, true, true);
        getVisualizer().setEnabled(true);
    }

    public void release() {
        Visualizer visualizer = this.visualizer;
        if (visualizer != null) {
            visualizer.setEnabled(false);
            this.visualizer.release();
        }
        this.onDataCaptureListener = null;
        this.visualizer = null;
    }

    public boolean isAllowed(String process_name) {
        return ArrayUtils.contains(mMusicWhiteList, process_name);
    }
}
