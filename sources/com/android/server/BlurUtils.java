package com.android.server;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.lang.reflect.Array;
/* loaded from: classes.dex */
public class BlurUtils {
    public static Bitmap addBlackBoard(Bitmap bmp, int color) {
        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        Bitmap newBitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(newBitmap);
        canvas.drawBitmap(bmp, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, paint);
        canvas.drawColor(color);
        return newBitmap;
    }

    public static Bitmap blurImage(Context context, Bitmap input, float radius) {
        Bitmap tempInput = Bitmap.createScaledBitmap(input, input.getWidth() / 4, input.getHeight() / 4, false);
        Bitmap result = tempInput.copy(tempInput.getConfig(), true);
        RenderScript rsScript = RenderScript.create(context);
        if (rsScript == null) {
            return null;
        }
        Allocation alloc = Allocation.createFromBitmap(rsScript, tempInput, Allocation.MipmapControl.MIPMAP_NONE, 1);
        Allocation outAlloc = Allocation.createTyped(rsScript, alloc.getType());
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript));
        blur.setRadius(radius);
        blur.setInput(alloc);
        blur.forEach(outAlloc);
        outAlloc.copyTo(result);
        rsScript.destroy();
        return Bitmap.createScaledBitmap(result, input.getWidth(), input.getHeight(), false);
    }

    public static Bitmap stackBlur(Bitmap sentBitmap, int radius) {
        int i;
        int i2;
        int p = radius;
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        if (p < 1) {
            return null;
        }
        int w = bitmap.getWidth();
        int p2 = bitmap.getHeight();
        int[] pix = new int[w * p2];
        String str = " ";
        Log.e("pix", w + str + p2 + str + pix.length);
        bitmap.getPixels(pix, 0, w, 0, 0, w, p2);
        int wm = w - 1;
        int hm = p2 - 1;
        int wh = w * p2;
        int div = p + p + 1;
        int[] r = new int[wh];
        int[] g = new int[wh];
        int[] b = new int[wh];
        int[] vmin = new int[Math.max(w, p2)];
        int divsum = (div + 1) >> 1;
        int bsum = divsum * divsum;
        int[] dv = new int[bsum * 256];
        int i3 = 0;
        while (true) {
            int wh2 = wh;
            if (i3 >= bsum * 256) {
                break;
            }
            dv[i3] = i3 / bsum;
            i3++;
            wh = wh2;
        }
        int yi = 0;
        int yw = 0;
        int[][] stack = (int[][]) Array.newInstance(int.class, div, 3);
        int r1 = p + 1;
        int y = 0;
        while (y < p2) {
            int h = 0;
            int rsum = 0;
            int boutsum = 0;
            int goutsum = 0;
            int routsum = 0;
            int binsum = 0;
            int ginsum = 0;
            int rinsum = 0;
            int divsum2 = bsum;
            int i4 = 0;
            Bitmap bitmap2 = bitmap;
            int p3 = -p;
            int bsum2 = 0;
            while (p3 <= p) {
                String str2 = str;
                int i5 = h;
                int h2 = p2;
                int h3 = Math.max(p3, i5);
                int p4 = pix[yi + Math.min(wm, h3)];
                int[] sir = stack[p3 + p];
                sir[i5] = (p4 & 16711680) >> 16;
                sir[1] = (p4 & 65280) >> 8;
                sir[2] = p4 & 255;
                int rbs = r1 - Math.abs(p3);
                rsum += sir[0] * rbs;
                i4 += sir[1] * rbs;
                bsum2 += sir[2] * rbs;
                if (p3 > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
                p3++;
                p2 = h2;
                str = str2;
                h = 0;
            }
            int h4 = p2;
            String str3 = str;
            int stackpointer = radius;
            int x = 0;
            while (x < w) {
                r[yi] = dv[rsum];
                g[yi] = dv[i4];
                b[yi] = dv[bsum2];
                int rsum2 = rsum - routsum;
                int gsum = i4 - goutsum;
                int bsum3 = bsum2 - boutsum;
                int stackstart = (stackpointer - p) + div;
                int[] sir2 = stack[stackstart % div];
                int routsum2 = routsum - sir2[0];
                int goutsum2 = goutsum - sir2[1];
                int boutsum2 = boutsum - sir2[2];
                if (y != 0) {
                    i2 = p3;
                } else {
                    i2 = p3;
                    int i6 = x + p + 1;
                    vmin[x] = Math.min(i6, wm);
                }
                int i7 = vmin[x];
                int p5 = pix[yw + i7];
                sir2[0] = (p5 & 16711680) >> 16;
                sir2[1] = (p5 & 65280) >> 8;
                int wm2 = wm;
                int wm3 = p5 & 255;
                sir2[2] = wm3;
                int rinsum2 = rinsum + sir2[0];
                int ginsum2 = ginsum + sir2[1];
                int binsum2 = binsum + sir2[2];
                rsum = rsum2 + rinsum2;
                i4 = gsum + ginsum2;
                bsum2 = bsum3 + binsum2;
                stackpointer = (stackpointer + 1) % div;
                int[] sir3 = stack[stackpointer % div];
                routsum = routsum2 + sir3[0];
                goutsum = goutsum2 + sir3[1];
                boutsum = boutsum2 + sir3[2];
                rinsum = rinsum2 - sir3[0];
                ginsum = ginsum2 - sir3[1];
                binsum = binsum2 - sir3[2];
                yi++;
                x++;
                wm = wm2;
                p3 = i2;
            }
            yw += w;
            y++;
            p2 = h4;
            bitmap = bitmap2;
            bsum = divsum2;
            str = str3;
        }
        Bitmap bitmap3 = bitmap;
        int stackstart2 = p2;
        String str4 = str;
        int x2 = 0;
        int h5 = y;
        while (x2 < w) {
            int bsum4 = 0;
            int gsum2 = 0;
            int rsum3 = 0;
            int yp = (-p) * w;
            int yp2 = -p;
            int boutsum3 = 0;
            int y2 = yp2;
            int yp3 = yp;
            int rinsum3 = 0;
            int ginsum3 = 0;
            int binsum3 = 0;
            int routsum3 = 0;
            int goutsum3 = 0;
            while (y2 <= p) {
                int[] vmin2 = vmin;
                int yi2 = Math.max(0, yp3) + x2;
                int[] sir4 = stack[y2 + p];
                sir4[0] = r[yi2];
                sir4[1] = g[yi2];
                sir4[2] = b[yi2];
                int rbs2 = r1 - Math.abs(y2);
                rsum3 += r[yi2] * rbs2;
                gsum2 += g[yi2] * rbs2;
                bsum4 += b[yi2] * rbs2;
                if (y2 > 0) {
                    rinsum3 += sir4[0];
                    ginsum3 += sir4[1];
                    binsum3 += sir4[2];
                } else {
                    routsum3 += sir4[0];
                    goutsum3 += sir4[1];
                    boutsum3 += sir4[2];
                }
                if (y2 < hm) {
                    yp3 += w;
                }
                y2++;
                vmin = vmin2;
            }
            int[] vmin3 = vmin;
            int yi3 = x2;
            int stackpointer2 = radius;
            int i8 = boutsum3;
            int boutsum4 = yi3;
            int yi4 = 0;
            int y3 = i8;
            while (true) {
                int i9 = y2;
                i = stackstart2;
                if (yi4 < i) {
                    pix[boutsum4] = (pix[boutsum4] & (-16777216)) | (dv[rsum3] << 16) | (dv[gsum2] << 8) | dv[bsum4];
                    int rsum4 = rsum3 - routsum3;
                    int gsum3 = gsum2 - goutsum3;
                    int bsum5 = bsum4 - y3;
                    int stackstart3 = (stackpointer2 - p) + div;
                    int[] sir5 = stack[stackstart3 % div];
                    int routsum4 = routsum3 - sir5[0];
                    int goutsum4 = goutsum3 - sir5[1];
                    int boutsum5 = y3 - sir5[2];
                    if (x2 == 0) {
                        vmin3[yi4] = Math.min(yi4 + r1, hm) * w;
                    }
                    int p6 = vmin3[yi4] + x2;
                    sir5[0] = r[p6];
                    sir5[1] = g[p6];
                    sir5[2] = b[p6];
                    int rinsum4 = rinsum3 + sir5[0];
                    int ginsum4 = ginsum3 + sir5[1];
                    int binsum4 = binsum3 + sir5[2];
                    rsum3 = rsum4 + rinsum4;
                    gsum2 = gsum3 + ginsum4;
                    bsum4 = bsum5 + binsum4;
                    stackpointer2 = (stackpointer2 + 1) % div;
                    int[] sir6 = stack[stackpointer2];
                    routsum3 = routsum4 + sir6[0];
                    goutsum3 = goutsum4 + sir6[1];
                    y3 = boutsum5 + sir6[2];
                    rinsum3 = rinsum4 - sir6[0];
                    ginsum3 = ginsum4 - sir6[1];
                    binsum3 = binsum4 - sir6[2];
                    boutsum4 += w;
                    yi4++;
                    p = radius;
                    stackstart2 = i;
                    y2 = i9;
                }
            }
            x2++;
            p = radius;
            stackstart2 = i;
            h5 = yi4;
            vmin = vmin3;
        }
        int y4 = stackstart2;
        Log.e("pix", w + str4 + y4 + str4 + pix.length);
        bitmap3.setPixels(pix, 0, w, 0, 0, w, y4);
        return bitmap3;
    }
}
