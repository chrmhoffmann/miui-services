package com.android.server.content;

import android.accounts.Account;
import android.content.SyncResult;
import android.content.SyncStatusInfo;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.location.gnss.map.AmapExtraCommand;
import com.android.server.wm.MiuiSizeCompatService;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
/* loaded from: classes.dex */
public class SyncStorageEngineStubImpl implements SyncStorageEngineStub {
    private static final String MI_PAUSE_FILE_NAME = "mi_pause.xml";
    private static final String MI_STRATEGY_FILE_NAME = "mi_strategy.xml";
    private static final String TAG = "SyncManager";
    private static final String TAG_FILE = "SyncManagerFile";
    private static SparseArray<Map<String, MiSyncPauseImpl>> mMiSyncPause = new SparseArray<>();
    private static SparseArray<Map<String, MiSyncStrategyImpl>> mMiSyncStrategy = new SparseArray<>();
    private static AtomicFile mMiPauseFile = null;
    private static AtomicFile mMiStrategyFile = null;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SyncStorageEngineStubImpl> {

        /* compiled from: SyncStorageEngineStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SyncStorageEngineStubImpl INSTANCE = new SyncStorageEngineStubImpl();
        }

        public SyncStorageEngineStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public SyncStorageEngineStubImpl provideNewInstance() {
            return new SyncStorageEngineStubImpl();
        }
    }

    public void initAndReadAndWriteLocked(File syncDir) {
        mMiPauseFile = new AtomicFile(new File(syncDir, MI_PAUSE_FILE_NAME));
        mMiStrategyFile = new AtomicFile(new File(syncDir, MI_STRATEGY_FILE_NAME));
        readAndWriteLocked();
    }

    private static void readAndWriteLocked() {
        readLocked();
        writeLocked();
    }

    private static void readLocked() {
        readMiPauseLocked();
        readMiStrategyLocked();
    }

    private static void writeLocked() {
        writeMiPauseLocked();
        writeMiStrategyLocked();
    }

    private static void clear() {
        mMiSyncPause.clear();
        mMiSyncStrategy.clear();
    }

    public void clearAndReadAndWriteLocked() {
        clear();
        readAndWriteLocked();
    }

    public void doDatabaseCleanupLocked(Account[] accounts, int uid) {
        doMiPauseCleanUpLocked(accounts, uid);
        doMiStrategyCleanUpLocked(accounts, uid);
        writeLocked();
    }

    private static void readMiPauseLocked() {
        int version;
        FileInputStream fis = null;
        try {
            try {
                FileInputStream fis2 = mMiPauseFile.openRead();
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Slog.v(TAG_FILE, "Reading " + mMiPauseFile.getBaseFile());
                }
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis2, StandardCharsets.UTF_8.name());
                int eventType = parser.getEventType();
                while (eventType != 2 && eventType != 1) {
                    eventType = parser.next();
                }
                if (eventType == 1) {
                    Slog.i(TAG, "No initial mi pause");
                    if (fis2 == null) {
                        return;
                    }
                    try {
                        fis2.close();
                        return;
                    } catch (IOException e) {
                        return;
                    }
                }
                String tagName = parser.getName();
                if (MiSyncPauseImpl.XML_FILE_NAME.equalsIgnoreCase(tagName)) {
                    String versionString = parser.getAttributeValue(null, AmapExtraCommand.VERSION_KEY);
                    if (versionString == null) {
                        version = 0;
                    } else {
                        try {
                            version = Integer.parseInt(versionString);
                        } catch (NumberFormatException e2) {
                            version = 0;
                        }
                    }
                    if (version >= 1) {
                        int eventType2 = parser.next();
                        do {
                            if (eventType2 == 2) {
                                MiSyncPauseImpl miSyncPause = MiSyncPauseImpl.readFromXML(parser);
                                setMiPauseInternalLocked(miSyncPause);
                            }
                            eventType2 = parser.next();
                        } while (eventType2 != 1);
                    }
                }
                if (fis2 == null) {
                    return;
                }
                try {
                    fis2.close();
                } catch (IOException e3) {
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        fis.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (IOException e5) {
            if (0 == 0) {
                Slog.i(TAG, "No initial mi pause");
            } else {
                Slog.w(TAG, "Error reading mi pause", e5);
            }
            if (0 == 0) {
                return;
            }
            try {
                fis.close();
            } catch (IOException e6) {
            }
        } catch (XmlPullParserException e7) {
            Slog.w(TAG, "Error reading mi pause", e7);
            if (0 == 0) {
                return;
            }
            try {
                fis.close();
            } catch (IOException e8) {
            }
        }
    }

    private static void readMiStrategyLocked() {
        int version;
        FileInputStream fis = null;
        try {
            try {
                FileInputStream fis2 = mMiStrategyFile.openRead();
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Slog.v(TAG_FILE, "Reading " + mMiStrategyFile.getBaseFile());
                }
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis2, StandardCharsets.UTF_8.name());
                int eventType = parser.getEventType();
                while (eventType != 2 && eventType != 1) {
                    eventType = parser.next();
                }
                if (eventType == 1) {
                    Slog.i(TAG, "No initial mi strategy");
                    if (fis2 == null) {
                        return;
                    }
                    try {
                        fis2.close();
                        return;
                    } catch (IOException e) {
                        return;
                    }
                }
                String tagName = parser.getName();
                if (MiSyncStrategyImpl.XML_FILE_NAME.equalsIgnoreCase(tagName)) {
                    String versionString = parser.getAttributeValue(null, AmapExtraCommand.VERSION_KEY);
                    if (versionString == null) {
                        version = 0;
                    } else {
                        try {
                            version = Integer.parseInt(versionString);
                        } catch (NumberFormatException e2) {
                            version = 0;
                        }
                    }
                    if (version >= 1) {
                        int eventType2 = parser.next();
                        do {
                            if (eventType2 == 2) {
                                MiSyncStrategyImpl miSyncStrategy = MiSyncStrategyImpl.readFromXML(parser);
                                setMiStrategyInternalLocked(miSyncStrategy);
                            }
                            eventType2 = parser.next();
                        } while (eventType2 != 1);
                    }
                }
                if (fis2 == null) {
                    return;
                }
                try {
                    fis2.close();
                } catch (IOException e3) {
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        fis.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (IOException e5) {
            if (0 == 0) {
                Slog.i(TAG, "No initial mi strategy");
            } else {
                Slog.w(TAG, "Error reading mi strategy", e5);
            }
            if (0 == 0) {
                return;
            }
            try {
                fis.close();
            } catch (IOException e6) {
            }
        } catch (XmlPullParserException e7) {
            Slog.w(TAG, "Error reading mi strategy", e7);
            if (0 == 0) {
                return;
            }
            try {
                fis.close();
            } catch (IOException e8) {
            }
        }
    }

    private static void writeMiPauseLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Writing new " + mMiPauseFile.getBaseFile());
        }
        FileOutputStream fos = null;
        try {
            fos = mMiPauseFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.setFeature(MiuiSizeCompatService.FAST_XML, true);
            out.startTag(null, MiSyncPauseImpl.XML_FILE_NAME);
            out.attribute(null, AmapExtraCommand.VERSION_KEY, Integer.toString(1));
            int m = mMiSyncPause.size();
            for (int i = 0; i < m; i++) {
                Map<String, MiSyncPauseImpl> map = mMiSyncPause.valueAt(i);
                Collection<MiSyncPauseImpl> values = map.values();
                for (MiSyncPauseImpl item : values) {
                    item.writeToXML(out);
                }
            }
            out.endTag(null, MiSyncPauseImpl.XML_FILE_NAME);
            out.endDocument();
            mMiPauseFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w(TAG, "Error writing mi pause", e1);
            if (fos != null) {
                mMiPauseFile.failWrite(fos);
            }
        }
    }

    private static void writeMiStrategyLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Writing new " + mMiStrategyFile.getBaseFile());
        }
        FileOutputStream fos = null;
        try {
            fos = mMiStrategyFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.setFeature(MiuiSizeCompatService.FAST_XML, true);
            out.startTag(null, MiSyncStrategyImpl.XML_FILE_NAME);
            out.attribute(null, AmapExtraCommand.VERSION_KEY, Integer.toString(1));
            int m = mMiSyncStrategy.size();
            for (int i = 0; i < m; i++) {
                Map<String, MiSyncStrategyImpl> map = mMiSyncStrategy.valueAt(i);
                Collection<MiSyncStrategyImpl> values = map.values();
                for (MiSyncStrategyImpl item : values) {
                    item.writeToXML(out);
                }
            }
            out.endTag(null, MiSyncStrategyImpl.XML_FILE_NAME);
            out.endDocument();
            mMiStrategyFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w(TAG, "Error writing mi strategy", e1);
            if (fos != null) {
                mMiStrategyFile.failWrite(fos);
            }
        }
    }

    private static void setMiPauseInternalLocked(MiSyncPauseImpl miSyncPause) {
        if (miSyncPause == null) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "setMiPauseInternalLocked: miSyncPause is null");
                return;
            }
            return;
        }
        setMiPauseInternalLocked(miSyncPause.getAccountName(), miSyncPause.getPauseEndTime(), miSyncPause.getUid());
    }

    private static void setMiPauseInternalLocked(String accountName, long pauseTimeMillis, int uid) {
        if (TextUtils.isEmpty(accountName)) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "setMiPauseInternalLocked: accountName is null");
                return;
            }
            return;
        }
        MiSyncPauseImpl item = getOrCreateMiSyncPauseLocked(accountName, uid);
        item.setPauseToTime(pauseTimeMillis);
    }

    private static void setMiStrategyInternalLocked(MiSyncStrategyImpl miSyncStrategy) {
        if (miSyncStrategy == null) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "setMiStrategyInternalLocked: miSyncStrategy is null");
                return;
            }
            return;
        }
        setMiStrategyInternalLocked(miSyncStrategy.getAccountName(), miSyncStrategy.getStrategy(), miSyncStrategy.getUid());
    }

    private static void setMiStrategyInternalLocked(String accountName, int strategy, int uid) {
        if (TextUtils.isEmpty(accountName)) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "setMiStrategyInternalLocked: accountName is null");
                return;
            }
            return;
        }
        MiSyncStrategyImpl item = getOrCreateMiSyncStrategyLocked(accountName, uid);
        item.setStrategy(strategy);
    }

    private static void doMiPauseCleanUpLocked(Account[] runningAccounts, int uid) {
        Map<String, MiSyncPauseImpl> map = mMiSyncPause.get(uid);
        if (map == null) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "doMiPauseCleanUpLocked: map is null");
                return;
            }
            return;
        }
        List<String> removing = getRemovingAccounts(runningAccounts, map.keySet());
        for (String accountName : removing) {
            map.remove(accountName);
        }
    }

    private static void doMiStrategyCleanUpLocked(Account[] runningAccounts, int uid) {
        Map<String, MiSyncStrategyImpl> map = mMiSyncStrategy.get(uid);
        if (map == null) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "doMiStrategyCleanUpLocked: map is null");
                return;
            }
            return;
        }
        List<String> removing = getRemovingAccounts(runningAccounts, map.keySet());
        for (String accountName : removing) {
            map.remove(accountName);
        }
    }

    private static List<String> getRemovingAccounts(Account[] runningAccounts, Set<String> currentAccountNameSet) {
        List<String> removing = new ArrayList<>();
        if (currentAccountNameSet == null) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "getRemovingAccounts: Argument is null");
            }
            return removing;
        }
        if (runningAccounts == null) {
            runningAccounts = new Account[0];
        }
        for (String accountName : currentAccountNameSet) {
            if (!containsXiaomiAccountName(runningAccounts, accountName)) {
                removing.add(accountName);
            }
        }
        return removing;
    }

    private static boolean containsXiaomiAccountName(Account[] accounts, String accountName) {
        if (accounts == null) {
            return false;
        }
        for (Account account : accounts) {
            if (MiSyncUtils.checkAccount(account) && TextUtils.equals(account.name, accountName)) {
                return true;
            }
        }
        return false;
    }

    public void setMiSyncPauseToTimeLocked(Account account, long pauseTimeMillis, int uid) {
        if (!MiSyncUtils.checkAccount(account)) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "setMiSyncPauseToTimeLocked: account is null");
                return;
            }
            return;
        }
        MiSyncPauseImpl miSyncPause = getOrCreateMiSyncPauseLocked(account.name, uid);
        if (miSyncPause.getPauseEndTime() == pauseTimeMillis) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "setMiSyncPauseTimeLocked: pause time is not changed");
                return;
            }
            return;
        }
        miSyncPause.setPauseToTime(pauseTimeMillis);
        writeMiPauseLocked();
    }

    public void setMiSyncStrategyLocked(Account account, int strategy, int uid) {
        if (!MiSyncUtils.checkAccount(account)) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "setMiSyncStrategyLocked: account is null");
                return;
            }
            return;
        }
        MiSyncStrategyImpl miSyncStrategy = getOrCreateMiSyncStrategyLocked(account.name, uid);
        if (miSyncStrategy.getStrategy() == strategy) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "setMiSyncPauseTimeLocked: strategy is not changed");
                return;
            }
            return;
        }
        miSyncStrategy.setStrategy(strategy);
        writeMiStrategyLocked();
    }

    public long getMiSyncPauseToTimeLocked(Account account, int uid) {
        if (!MiSyncUtils.checkAccount(account)) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "getMiSyncPauseToTimeLocked: not xiaomi account");
                return 0L;
            }
            return 0L;
        }
        MiSyncPauseImpl miSyncPause = getOrCreateMiSyncPauseLocked(account.name, uid);
        return miSyncPause.getPauseEndTime();
    }

    public int getMiSyncStrategyLocked(Account account, int uid) {
        if (!MiSyncUtils.checkAccount(account)) {
            if (Log.isLoggable(TAG, 2)) {
                Slog.v(TAG, "getMiSyncStrategyLocked: not xiaomi account");
                return 1;
            }
            return 1;
        }
        MiSyncStrategyImpl miSyncStrategy = getOrCreateMiSyncStrategyLocked(account.name, uid);
        return miSyncStrategy.getStrategy();
    }

    public MiSyncPause getMiSyncPauseLocked(String accountName, int uid) {
        return getOrCreateMiSyncPauseLocked(accountName, uid);
    }

    public MiSyncStrategy getMiSyncStrategyLocked(String accountName, int uid) {
        return getOrCreateMiSyncStrategyLocked(accountName, uid);
    }

    private static MiSyncPauseImpl getOrCreateMiSyncPauseLocked(String accountName, int uid) {
        Map<String, MiSyncPauseImpl> map = mMiSyncPause.get(uid);
        if (map == null) {
            map = new HashMap();
            mMiSyncPause.put(uid, map);
        }
        MiSyncPauseImpl item = null;
        if (accountName == null) {
            accountName = "";
        }
        if (map.containsKey(accountName)) {
            MiSyncPauseImpl item2 = map.get(accountName);
            item = item2;
        }
        if (item == null) {
            MiSyncPauseImpl item3 = new MiSyncPauseImpl(uid, accountName);
            map.put(accountName, item3);
            return item3;
        }
        return item;
    }

    private static MiSyncStrategyImpl getOrCreateMiSyncStrategyLocked(String accountName, int uid) {
        Map<String, MiSyncStrategyImpl> map = mMiSyncStrategy.get(uid);
        if (map == null) {
            map = new HashMap();
            mMiSyncStrategy.put(uid, map);
        }
        MiSyncStrategyImpl item = null;
        if (accountName == null) {
            accountName = "";
        }
        if (map.containsKey(accountName)) {
            MiSyncStrategyImpl item2 = map.get(accountName);
            item = item2;
        }
        if (item == null) {
            MiSyncStrategyImpl item3 = new MiSyncStrategyImpl(uid, accountName);
            map.put(accountName, item3);
            return item3;
        }
        return item;
    }

    public void updateResultStatusLocked(SyncStatusInfo syncStatusInfo, String lastSyncMessage, SyncResult syncResult) {
        MiSyncResultStatusAdapter.updateResultStatus(syncStatusInfo, lastSyncMessage, syncResult);
    }
}
