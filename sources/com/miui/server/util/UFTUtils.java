package com.miui.server.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import com.miui.server.AccessController;
import java.util.HashSet;
import java.util.Set;
/* loaded from: classes.dex */
public class UFTUtils {
    private static final Set<String> THIRDPARTY_APP_WHITESET;

    static {
        HashSet hashSet = new HashSet();
        THIRDPARTY_APP_WHITESET = hashSet;
        hashSet.add("com.tencent.mm");
        hashSet.add("com.ss.android.ugc.aweme");
        hashSet.add("com.eg.android.AlipayGphone");
        hashSet.add("com.taobao.taobao");
        hashSet.add("com.xunmeng.pinduoduo");
        hashSet.add("com.tencent.mobileqq");
        hashSet.add("com.ss.android.article.news");
        hashSet.add("com.sina.weibo");
        hashSet.add("com.xunmeng.pinduoduo");
        hashSet.add("tv.danmaku.bili");
        hashSet.add("com.jingdong.app.mall");
        hashSet.add("com.tencent.tmgp.sgame");
        hashSet.add("com.autonavi.minimap");
        hashSet.add("com.tencent.mtt");
        hashSet.add("com.ss.android.ugc.aweme.lite");
        hashSet.add("com.smile.gifmaker");
        hashSet.add("com.kuaishou.nebula");
        hashSet.add("com.netease.cloudmusic");
        hashSet.add("com.sankuai.meituan");
        hashSet.add("com.alibaba.android.rimet");
        hashSet.add("com.tencent.qqmusic");
        hashSet.add("com.tencent.qqlive");
        hashSet.add("com.taobao.idlefish");
        hashSet.add("com.zhihu.android");
        hashSet.add("com.baidu.searchbox");
        hashSet.add("com.UCMobile");
        hashSet.add("com.quark.browser");
        hashSet.add("com.tencent.wework");
        hashSet.add("com.baidu.BaiduMap");
        hashSet.add("com.ss.android.article.lite");
        hashSet.add("com.kugou.android");
        hashSet.add("com.coolapk.market");
        hashSet.add("com.baidu.tieba");
        hashSet.add("com.cainiao.wireless");
        hashSet.add("com.qiyi.video");
        hashSet.add("me.ele");
        hashSet.add("com.dragon.read");
        hashSet.add("com.xingin.xhs");
        hashSet.add("com.taobao.litetao");
        hashSet.add("com.baidu.netdisk");
        hashSet.add("com.tencent.tmgp.pubgmhd");
        hashSet.add("com.unionpay");
        hashSet.add("com.ximalaya.ting.android");
        hashSet.add("com.tencent.android.qqdownloader");
        hashSet.add("com.sankuai.meituan.takeoutnew");
        hashSet.add("com.duowan.kiwi");
        hashSet.add("com.ss.android.article.video");
        hashSet.add("com.tencent.jkchess");
        hashSet.add("com.youku.phone");
        hashSet.add("com.snda.wifilocating");
        hashSet.add("com.miui.notes");
        hashSet.add("com.miui.weather2");
        hashSet.add(AccessController.PACKAGE_GALLERY);
        hashSet.add("com.android.calendar");
        hashSet.add("com.android.fileexplorer");
        hashSet.add("com.miui.calculator");
        hashSet.add("com.android.deskclock");
        hashSet.add("com.android.soundrecorder");
        hashSet.add("com.miui.compass");
        hashSet.add("com.miui.player");
        hashSet.add("com.xiaomi.shop");
    }

    public static boolean isSupport(String pkg, Context context) {
        if (TextUtils.isEmpty(pkg)) {
            return false;
        }
        return isSystemApp(pkg, context) || isSupportedPartyApp(pkg);
    }

    private static boolean isSystemApp(String pkg, Context context) {
        try {
            return context.getPackageManager().getApplicationInfo(pkg, 0).isSystemApp();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isSupportedPartyApp(String pkg) {
        if (THIRDPARTY_APP_WHITESET.contains(pkg)) {
            return true;
        }
        return false;
    }
}
