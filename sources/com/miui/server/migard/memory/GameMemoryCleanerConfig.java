package com.miui.server.migard.memory;

import android.text.TextUtils;
import com.android.server.am.GameProcessCompactor;
import com.android.server.am.GameProcessKiller;
import com.android.server.am.IGameProcessAction;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.miui.server.migard.utils.FileUtils;
import com.miui.server.migard.utils.LogUtils;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class GameMemoryCleanerConfig {
    private static final String CLEAN_FIRST_DELAY = "clean-first-delay";
    private static final String CLEAN_PERIOD = "mem-clean-period";
    private static final String COMPACTOR_CONFIGS = "compactor-configs";
    private static final String CONFIG_PATH = "/vendor/etc/game_memory_cleaner.cfg";
    private static final int DEFAULT_CLEAN_FIRST_DELAY = -1;
    private static final int DEFAULT_CLEAN_PERIOD = 0;
    private static final int DEFAULT_RECLAIM_MEMORY_PCT = 100;
    private static final String GAME_LIST = "game-list";
    private static final String KILLER_CONFIGS = "killer-configs";
    private static final String RECLAIM_MEMORY_PCT = "reclaim_memory_percent";
    private static final String TAG = GameMemoryCleanerConfig.class.getSimpleName();
    private static GameMemoryCleanerConfig sInstance = new GameMemoryCleanerConfig();
    private GameConfig mCommonConfig = null;
    private Map<String, GameConfig> mConfigMap = new HashMap();
    private GameConfig mActiveConfig = null;
    private List<String> mGameList = new ArrayList();
    private List<String> mUserProtectList = new ArrayList();
    private List<String> mKillerCommonWhiteList = new ArrayList();
    private List<String> mCompactorCommonWhiteList = new ArrayList();
    private List<String> mPowerWhiteList = new ArrayList();

    public static GameMemoryCleanerConfig getInstance() {
        return sInstance;
    }

    private GameMemoryCleanerConfig() {
        initMMSWhiteList();
    }

    private void initMMSWhiteList() {
        this.mPowerWhiteList.add(InputMethodManagerServiceImpl.MIUI_HOME);
        this.mPowerWhiteList.add("com.android.systemui");
        this.mPowerWhiteList.add("com.xiaomi.xmsf");
        this.mPowerWhiteList.add("com.miui.securitycore");
        this.mPowerWhiteList.add("com.miui.hybrid");
        this.mPowerWhiteList.add("com.android.providers.media.module");
        this.mPowerWhiteList.add("com.google.android.providers.media.module");
        this.mPowerWhiteList.add("com.miui.mishare.connectivity");
        this.mPowerWhiteList.add("com.lbe.security.miui");
        this.mPowerWhiteList.add("com.xiaomi.metoknlp");
        this.mPowerWhiteList.add("com.miui.cloudbackup");
    }

    private boolean parseConfig(String jsonString, GameConfig config, boolean cloud) {
        JSONArray tmpArray;
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            if (!cloud && jsonObject.has(GAME_LIST) && (tmpArray = jsonObject.optJSONArray(GAME_LIST)) != null) {
                for (int i = 0; i < tmpArray.length(); i++) {
                    if (!tmpArray.isNull(i) && !this.mGameList.contains(tmpArray.getString(i))) {
                        this.mGameList.add(tmpArray.getString(i));
                    }
                }
            }
            parseGameConfig(jsonObject, config);
            return true;
        } catch (JSONException e) {
            LogUtils.e(TAG, "parse config failed", e);
            return false;
        }
    }

    private void parseGameConfig(JSONObject json, GameConfig config) throws JSONException {
        if (json.has(CLEAN_PERIOD)) {
            config.mCleanPeriod = json.optInt(CLEAN_PERIOD, 0);
        }
        if (json.has(CLEAN_FIRST_DELAY)) {
            config.mCleanFirstDelay = json.optInt(CLEAN_FIRST_DELAY, -1);
        }
        if (json.has(KILLER_CONFIGS)) {
            parseKillerConfigs(json.optJSONArray(KILLER_CONFIGS), config);
        }
        if (json.has(COMPACTOR_CONFIGS)) {
            parseCompactorConfigs(json.optJSONArray(COMPACTOR_CONFIGS), config);
        }
        if (json.has(RECLAIM_MEMORY_PCT)) {
            config.mReclaimMemoryPercent = json.optInt(RECLAIM_MEMORY_PCT, 100);
        }
    }

    private void parseKillerConfigs(JSONArray array, GameConfig config) throws JSONException {
        int N;
        if (array == null || (N = array.length()) == 0) {
            return;
        }
        for (int i = 0; i < N; i++) {
            try {
                JSONObject jsonObject = array.optJSONObject(i);
                if (jsonObject != null) {
                    IGameProcessAction.IGameProcessActionConfig cfg = new GameProcessKiller.GameProcessKillerConfig();
                    cfg.initFromJSON(jsonObject);
                    config.mActionConfigs.add(cfg);
                }
            } catch (JSONException e) {
                throw e;
            }
        }
    }

    private void parseCompactorConfigs(JSONArray array, GameConfig config) throws JSONException {
        int N;
        if (array == null || (N = array.length()) == 0) {
            return;
        }
        for (int i = 0; i < N; i++) {
            try {
                JSONObject jsonObject = array.optJSONObject(i);
                if (jsonObject != null) {
                    IGameProcessAction.IGameProcessActionConfig cfg = new GameProcessCompactor.GameProcessCompactorConfig();
                    cfg.initFromJSON(jsonObject);
                    config.mActionConfigs.add(cfg);
                }
            } catch (JSONException e) {
                throw e;
            }
        }
    }

    public boolean hasForegroundClean() {
        GameConfig gameConfig = this.mActiveConfig;
        return gameConfig != null && gameConfig.mCleanFirstDelay >= 0;
    }

    public void configFromFile() {
        String jsonString;
        if (new File(CONFIG_PATH).exists() && (jsonString = FileUtils.readFromSys(CONFIG_PATH)) != null && jsonString.length() > 0) {
            this.mGameList.clear();
            GameConfig config = new GameConfig();
            if (parseConfig(jsonString, config, false)) {
                this.mCommonConfig = config;
            }
        }
    }

    public void configFromCloudControl(String game, String jsonString) {
        if (game != null && !TextUtils.isEmpty(game) && jsonString != null && !TextUtils.isEmpty(jsonString)) {
            GameConfig config = new GameConfig();
            if (parseConfig(jsonString, config, true)) {
                this.mConfigMap.put(game, config);
            }
        }
    }

    public List<String> getGameList() {
        return this.mGameList;
    }

    public void addGameList(List<String> list, boolean append) {
        if (append) {
            this.mGameList.removeAll(list);
        } else {
            this.mGameList.clear();
        }
        this.mGameList.addAll(list);
    }

    public int getCleanPeriod() {
        GameConfig gameConfig = this.mActiveConfig;
        if (gameConfig != null) {
            return gameConfig.mCleanPeriod;
        }
        return 0;
    }

    public int getCleanFirstDelay() {
        GameConfig gameConfig = this.mActiveConfig;
        if (gameConfig != null) {
            return gameConfig.mCleanFirstDelay;
        }
        return -1;
    }

    public void applyGameConfig(String game) {
        if (game == null || TextUtils.isEmpty(game)) {
            this.mActiveConfig = null;
        } else if (this.mConfigMap.containsKey(game)) {
            this.mActiveConfig = this.mConfigMap.get(game);
        } else {
            this.mActiveConfig = this.mCommonConfig;
        }
    }

    public List<IGameProcessAction.IGameProcessActionConfig> getActionConfigs() {
        GameConfig gameConfig = this.mActiveConfig;
        if (gameConfig != null) {
            List<IGameProcessAction.IGameProcessActionConfig> actionConfigs = gameConfig.mActionConfigs;
            Collections.sort(actionConfigs, new Comparator<IGameProcessAction.IGameProcessActionConfig>() { // from class: com.miui.server.migard.memory.GameMemoryCleanerConfig.1
                public int compare(IGameProcessAction.IGameProcessActionConfig o1, IGameProcessAction.IGameProcessActionConfig o2) {
                    return o1.getPrio() - o2.getPrio();
                }
            });
            return actionConfigs;
        }
        return new ArrayList();
    }

    public int getReclaimMemoryPercent() {
        GameConfig gameConfig = this.mActiveConfig;
        if (gameConfig != null) {
            return gameConfig.mReclaimMemoryPercent;
        }
        return 100;
    }

    public void updatePowerWhiteList(String pkgList) {
        if (!TextUtils.isEmpty(pkgList)) {
            String[] pkgArray = pkgList.split(",");
            for (String packageName : pkgArray) {
                this.mPowerWhiteList.add(packageName);
            }
        }
    }

    public List<String> getPowerWhiteList() {
        return this.mPowerWhiteList;
    }

    public void addUserProtectList(List<String> list, boolean append) {
        LogUtils.d(TAG, "add user protect list: " + list);
        if (append) {
            this.mUserProtectList.removeAll(list);
        } else {
            this.mUserProtectList.clear();
        }
        this.mUserProtectList.addAll(list);
    }

    public List<String> getUserProtectList() {
        return this.mUserProtectList;
    }

    public void removeUserProtectList(List<String> list) {
        LogUtils.d(TAG, "remove user protect list: " + list);
        this.mUserProtectList.removeAll(list);
    }

    public void addKillerCommonWhilteList(List<String> list) {
        this.mKillerCommonWhiteList.clear();
        this.mKillerCommonWhiteList.addAll(list);
    }

    public void addCompactorCommonWhiteList(List<String> list) {
        this.mCompactorCommonWhiteList.clear();
        this.mCompactorCommonWhiteList.addAll(list);
    }

    public List<String> getKillerCommonWhilteList() {
        return this.mKillerCommonWhiteList;
    }

    public List<String> getCompactorCommonWhiteList() {
        return this.mCompactorCommonWhiteList;
    }

    public void dump(PrintWriter pw) {
        pw.println("game memory cleaner config: ");
        if (this.mCommonConfig != null) {
            pw.println("common config: ");
            pw.println(this.mCommonConfig);
        }
        for (Map.Entry<String, GameConfig> c : this.mConfigMap.entrySet()) {
            pw.println("game: " + c.getKey());
            pw.println(c.getValue());
        }
        pw.println("");
        pw.println("user protect list:");
        pw.println(this.mUserProtectList);
        pw.println("");
        pw.println("smartpower white list:");
        pw.println(this.mPowerWhiteList);
        pw.println("");
        pw.println("common kill white list:");
        pw.println(this.mKillerCommonWhiteList);
        pw.println("");
        pw.println("common compact white list:");
        pw.println(this.mCompactorCommonWhiteList);
        pw.println("");
    }

    /* loaded from: classes.dex */
    public class GameConfig {
        List<IGameProcessAction.IGameProcessActionConfig> mActionConfigs = new ArrayList();
        int mReclaimMemoryPercent = 100;
        int mCleanPeriod = 0;
        int mCleanFirstDelay = -1;

        GameConfig() {
            GameMemoryCleanerConfig.this = r1;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("clean memory percent: ").append(this.mReclaimMemoryPercent).append("\n");
            for (IGameProcessAction.IGameProcessActionConfig cfg : this.mActionConfigs) {
                sb.append(cfg);
                sb.append("\n");
            }
            sb.append("\n");
            sb.append("first clean delay: ").append(this.mCleanFirstDelay).append("\n");
            sb.append("clean periodic: ").append(this.mCleanPeriod).append("\n");
            return sb.toString();
        }
    }
}
