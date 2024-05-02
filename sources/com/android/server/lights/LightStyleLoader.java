package com.android.server.lights;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import libcore.io.IoUtils;
import miui.util.FeatureParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
/* loaded from: classes.dex */
public class LightStyleLoader {
    private static final String BRIGHTNESS_MODE = "brightnessMode";
    private static final String COLOR_ARGB = "colorARGB";
    private static final boolean DEBUG = LightsService.DEBUG;
    private static final int DEFAULT_DELAY = 2000;
    private static final int DEFAULT_INTERVAL = 100;
    private static final String FLASH_MODE = "flashMode";
    private static final int GAME_EFFECT_ONMS = 4899;
    private static final String LIGHTSTATE = "lightstate";
    private static final String LIGHTS_PATH_DIR = "/product/etc/lights/";
    private static final int LIGHT_COLOR = 0;
    private static final int LIGHT_FREQ = 1;
    private static final int MAXDELAY = 300000;
    private static final String OFFMS = "offMS";
    private static final String ONMS = "onMS";
    private static final String TAG = "LightsService";
    private final Context mContext;
    private SparseArray mStyleArray = new SparseArray();
    private boolean mSupportColorEffect = FeatureParser.getBoolean("support_led_colorful_effect", false);
    private final Resources resources;

    public LightStyleLoader(Context context) {
        this.resources = context.getResources();
        this.mContext = context;
        this.mStyleArray.append(0, "lightstyle_default");
        this.mStyleArray.append(1, "lightstyle_phone");
        this.mStyleArray.append(2, "lightstyle_game");
        this.mStyleArray.append(3, "lightstyle_music");
        this.mStyleArray.append(4, "lightstyle_alarm");
        this.mStyleArray.append(5, "lightstyle_expand");
        this.mStyleArray.append(6, "lightstyle_luckymoney");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public List<LightState> getLightStyle(int styleType) {
        int colorARGB;
        List<LightState> ls_list = new ArrayList<>();
        InputStream is = null;
        try {
            if (styleType >= 0) {
                if (this.mStyleArray.get(styleType) != null) {
                    if (DEBUG) {
                        Slog.d(TAG, "parse style: " + styleType + " id: " + this.mStyleArray.get(styleType));
                    }
                    int flashMode = 1;
                    int offMS = 100;
                    int onMS = 100;
                    int brightnessMode = 0;
                    if (styleType == 1) {
                        int customOnMS = getCustomLight(1, 100, styleType);
                        if (customOnMS != 100) {
                            int customColorARGB = getCustomLight(0, -1, styleType);
                            ls_list.add(new LightState(customColorARGB, 1, customOnMS, DEFAULT_DELAY, 0));
                            return ls_list;
                        }
                        colorARGB = -1;
                    } else {
                        colorARGB = -1;
                    }
                    if (styleType == 2 && this.mSupportColorEffect) {
                        ls_list.add(new LightState(1, 1, GAME_EFFECT_ONMS, 100, 0));
                        return ls_list;
                    }
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    is = new BufferedInputStream(new FileInputStream(LIGHTS_PATH_DIR + this.mStyleArray.get(styleType) + ".xml"), 4096);
                    Document doc = builder.parse(is);
                    NodeList lightList = doc.getElementsByTagName(LIGHTSTATE);
                    int i = 0;
                    while (i < lightList.getLength()) {
                        NodeList childNodes = lightList.item(i).getChildNodes();
                        int j = 0;
                        while (true) {
                            Document doc2 = doc;
                            if (j < childNodes.getLength()) {
                                NodeList lightList2 = lightList;
                                if (childNodes.item(j).getNodeType() == 1) {
                                    String nodeName = childNodes.item(j).getNodeName();
                                    char c = 65535;
                                    switch (nodeName.hashCode()) {
                                        case -1147460173:
                                            if (nodeName.equals(FLASH_MODE)) {
                                                c = 1;
                                                break;
                                            }
                                            break;
                                        case 3414981:
                                            if (nodeName.equals(ONMS)) {
                                                c = 3;
                                                break;
                                            }
                                            break;
                                        case 105650005:
                                            if (nodeName.equals(OFFMS)) {
                                                c = 2;
                                                break;
                                            }
                                            break;
                                        case 1980337839:
                                            if (nodeName.equals(COLOR_ARGB)) {
                                                c = 0;
                                                break;
                                            }
                                            break;
                                        case 1984317844:
                                            if (nodeName.equals(BRIGHTNESS_MODE)) {
                                                c = 4;
                                                break;
                                            }
                                            break;
                                    }
                                    switch (c) {
                                        case 0:
                                            colorARGB = Color.parseColor(childNodes.item(j).getFirstChild().getNodeValue());
                                            break;
                                        case 1:
                                            flashMode = Integer.parseInt(childNodes.item(j).getFirstChild().getNodeValue());
                                            break;
                                        case 2:
                                            offMS = Integer.parseInt(childNodes.item(j).getFirstChild().getNodeValue());
                                            break;
                                        case 3:
                                            onMS = Integer.parseInt(childNodes.item(j).getFirstChild().getNodeValue());
                                            break;
                                        case 4:
                                            brightnessMode = Integer.parseInt(childNodes.item(j).getFirstChild().getNodeValue());
                                            break;
                                    }
                                }
                                j++;
                                doc = doc2;
                                lightList = lightList2;
                            } else {
                                NodeList lightList3 = lightList;
                                ls_list.add(new LightState(colorARGB, flashMode, onMS, offMS, brightnessMode));
                                i++;
                                doc = doc2;
                                lightList = lightList3;
                            }
                        }
                    }
                    return ls_list;
                }
            }
            return ls_list;
        } catch (Exception e) {
            Slog.e(TAG, "can't find xml file : " + this.mStyleArray.get(styleType) + ".xml--Please check the path to confirm : " + LIGHTS_PATH_DIR + " -- " + e.toString());
            e.printStackTrace();
            return ls_list;
        } finally {
            IoUtils.closeQuietly(is);
        }
    }

    private int getCustomLight(int attrs, int original, int style) {
        String jsonText = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "breathing_light", -2);
        if (TextUtils.isEmpty(jsonText)) {
            return original;
        }
        try {
            JSONArray jsonArray = new JSONArray(jsonText);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject logObject = jsonArray.getJSONObject(i);
                if (logObject.getInt("light") == style) {
                    switch (attrs) {
                        case 0:
                            return logObject.getInt("color");
                        case 1:
                            return logObject.getInt(ONMS);
                        default:
                            Slog.i(TAG, "un know attrs:" + attrs);
                            break;
                    }
                }
            }
        } catch (JSONException e) {
            Slog.i(TAG, "Light jsonArray error", e);
        }
        return original;
    }
}
