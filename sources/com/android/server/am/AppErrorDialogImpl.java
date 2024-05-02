package com.android.server.am;

import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.miui.base.MiuiStubRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
/* loaded from: classes.dex */
public class AppErrorDialogImpl extends AppErrorDialogStub {
    private ApplicationErrorReport.CrashInfo mCrashInfo;
    private String mCrashPackage;
    private AppErrorDialog mDialog;
    private final AtomicBoolean mReported = new AtomicBoolean(false);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppErrorDialogImpl> {

        /* compiled from: AppErrorDialogImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppErrorDialogImpl INSTANCE = new AppErrorDialogImpl();
        }

        public AppErrorDialogImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AppErrorDialogImpl provideNewInstance() {
            return new AppErrorDialogImpl();
        }
    }

    void onInit(AppErrorDialog dialog, Message forceQuitMsg, final Message reportMsg) {
        this.mDialog = dialog;
        final Context context = dialog.getContext();
        Resources res = context.getResources();
        String dialogBody = res.getString(286196188) + "\n\n";
        String dialogDumpToPreview = res.getString(286196189);
        SpannableStringBuilder textBuilder = new SpannableStringBuilder(dialogBody + dialogDumpToPreview);
        ClickableSpan span = new ClickableSpan() { // from class: com.android.server.am.AppErrorDialogImpl.1
            @Override // android.text.style.ClickableSpan
            public void onClick(View widget) {
                AppErrorDialogImpl.this.onReportClicked(context, reportMsg, true);
            }
        };
        textBuilder.setSpan(span, dialogBody.length(), dialogBody.length() + dialogDumpToPreview.length(), 33);
        dialog.setMessage(textBuilder);
        dialog.setCancelable(false);
        dialog.setButton(-1, context.getResources().getText(286195752), new DialogInterface.OnClickListener() { // from class: com.android.server.am.AppErrorDialogImpl$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                AppErrorDialogImpl.this.m209lambda$onInit$0$comandroidserveramAppErrorDialogImpl(context, reportMsg, dialogInterface, i);
            }
        });
        dialog.setButton(-2, context.getResources().getText(17039360), forceQuitMsg);
    }

    /* renamed from: lambda$onInit$0$com-android-server-am-AppErrorDialogImpl */
    public /* synthetic */ void m209lambda$onInit$0$comandroidserveramAppErrorDialogImpl(Context context, Message reportMsg, DialogInterface dialog1, int which) {
        onReportClicked(context, reportMsg, false);
    }

    void setCrashInfo(String crashPkg, ApplicationErrorReport.CrashInfo crashInfo) {
        this.mCrashPackage = crashPkg;
        this.mCrashInfo = crashInfo;
    }

    boolean onCreate() {
        int msgViewId = this.mDialog.getContext().getResources().getIdentifier("message", "id", InputMethodManagerServiceImpl.MIUIXPACKAGE);
        TextView msgView = (TextView) this.mDialog.findViewById(msgViewId);
        if (msgView != null) {
            msgView.setMovementMethod(LinkMovementMethod.getInstance());
            return true;
        }
        return true;
    }

    public void onReportClicked(Context ctx, Message reportMsg, boolean jumpToPreview) {
        if (!this.mReported.compareAndSet(false, true)) {
            return;
        }
        if (jumpToPreview) {
            MiuiErrorReport.startFcPreviewActivity(ctx, this.mCrashPackage, this.mCrashInfo);
        }
        reportMsg.sendToTarget();
    }
}
