package com.android.server.location.gnss.exp;

import android.location.ILocationListener;
import android.location.LocationRequest;
import android.location.util.identity.CallerIdentity;
/* loaded from: classes.dex */
public class GnssRequsetBean {
    CallerIdentity identity;
    ILocationListener listener;
    LocationRequest locationRequest;
    int permissionLevel;
    String provider;
    boolean removeByOpt;
}
