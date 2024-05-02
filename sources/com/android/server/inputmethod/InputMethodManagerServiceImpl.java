package com.android.server.inputmethod;

import android.app.Notification;
import android.content.Context;
import android.graphics.Insets;
import android.graphics.drawable.Icon;
import android.inputmethodservice.InputMethodAnalyticsUtil;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodInfo;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.internal.view.IInputContext;
import com.android.server.LocalServices;
import com.android.server.inputmethod.InputMethodMenuController;
import com.android.server.inputmethod.InputMethodSubtypeSwitchingController;
import com.android.server.inputmethod.InputMethodUtils;
import com.android.server.policy.DisplayTurnoverManager;
import com.miui.base.MiuiStubRegistry;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.os.Build;
import miuix.appcompat.app.AlertDialog;
/* loaded from: classes.dex */
public class InputMethodManagerServiceImpl implements InputMethodManagerServiceStub {
    public static final boolean DEBUG = true;
    private static final int IME_LIST_VIEW_VISIBLE_NUMBER = 3;
    public static final String MIUIXPACKAGE = "miuix.stub";
    public static final String MIUI_HOME = "com.miui.home";
    public static final String TAG = "InputMethodManagerServiceImpl";
    private static final List<String> customizedInputMethodList;
    Context dialogContext;
    InputMethodBindingController mBindingController;
    Handler mHandler;
    private IBinder mMonitorBinder = null;
    private volatile int mSynergyOperate = 0;
    private volatile int mLastAcceptStatus = 0;
    public int noCustomizedCheckedItem = -1;
    public InputMethodInfo[] mNoCustomizedIms = null;
    public int[] mNoCustomizedSubtypeIds = null;
    private boolean mScreenStatus = false;
    private boolean isExpanded = false;
    private boolean mScreenOffLastTime = false;
    private int mSessionId = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<InputMethodManagerServiceImpl> {

        /* compiled from: InputMethodManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final InputMethodManagerServiceImpl INSTANCE = new InputMethodManagerServiceImpl();
        }

        public InputMethodManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public InputMethodManagerServiceImpl provideNewInstance() {
            return new InputMethodManagerServiceImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        customizedInputMethodList = arrayList;
        arrayList.add("com.iflytek.inputmethod.miui");
        arrayList.add("com.baidu.input_mi");
        arrayList.add("com.sohu.inputmethod.sogou.xiaomi");
        arrayList.add("com.android.cts.mockime");
    }

    public static InputMethodManagerServiceImpl getInstance() {
        return (InputMethodManagerServiceImpl) InputMethodManagerServiceStub.getInstance();
    }

    public void init(Handler handler, InputMethodBindingController bindingController) {
        this.mHandler = handler;
        this.mBindingController = bindingController;
    }

    public void enableSystemIMEsIfThereIsNoEnabledIME(List<InputMethodInfo> methodList, InputMethodUtils.InputMethodSettings settings) {
        if (Build.IS_CM_CUSTOMIZATION_TEST || methodList == null || settings == null) {
            return;
        }
        List<Pair<String, ArrayList<String>>> enabledInputMethodsList = settings.getEnabledInputMethodsAndSubtypeListLocked();
        InputMethodInfo systemInputMethod = null;
        for (int i = 0; i < methodList.size(); i++) {
            InputMethodInfo inputMethodInfo = methodList.get(i);
            if ((inputMethodInfo.getServiceInfo().applicationInfo.flags & 1) != 0) {
                systemInputMethod = inputMethodInfo;
            }
            if (enabledInputMethodsList != null) {
                for (Pair<String, ArrayList<String>> pair : enabledInputMethodsList) {
                    if (TextUtils.equals((CharSequence) pair.first, inputMethodInfo.getId())) {
                        return;
                    }
                }
                continue;
            }
        }
        if (systemInputMethod != null) {
            settings.appendAndPutEnabledInputMethodLocked(systemInputMethod.getId(), false);
        }
    }

    public void onSwitchIME(Context context, InputMethodInfo curInputMethodInfo, String lastInputMethodId, List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList, InputMethodUtils.InputMethodSettings inputMethodSettings, ArrayMap<String, InputMethodInfo> methodMap) {
        if (TextUtils.equals(curInputMethodInfo.getId(), lastInputMethodId)) {
            return;
        }
        InputMethodAnalyticsUtil.addNotificationPanelRecord(context, curInputMethodInfo.getPackageName());
    }

    public boolean onMiuiTransact(int code, Parcel data, Parcel reply, int flags, IInputContext inputContext, int imeWindowVis, Handler handler) {
        InputMethodManagerInternal service;
        try {
            switch (code) {
                case DisplayTurnoverManager.CODE_TURN_OFF_SUB_DISPLAY /* 16777211 */:
                    this.mSynergyOperate = data.readInt();
                    reply.writeNoException();
                    return true;
                case DisplayTurnoverManager.CODE_TURN_ON_SUB_DISPLAY /* 16777212 */:
                    this.mMonitorBinder = null;
                    reply.writeNoException();
                    return true;
                case 16777213:
                    this.mMonitorBinder = data.readStrongBinder();
                    reply.writeNoException();
                    return true;
                case 16777214:
                    if ((imeWindowVis & 2) != 0 && (service = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class)) != null) {
                        service.hideCurrentInputMethod(15);
                    }
                    String text = data.readString();
                    if (inputContext != null) {
                        commitTextForSynergy(inputContext, text, 1);
                    }
                    reply.writeNoException();
                    reply.writeInt(1);
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean notifyAcceptInput(int status, int displayId) {
        if (this.mMonitorBinder != null) {
            Parcel request = Parcel.obtain();
            this.mLastAcceptStatus = status;
            try {
                request.writeInterfaceToken("com.android.synergy.Callback");
                request.writeInt(status);
                request.writeInt(displayId);
                this.mMonitorBinder.transact(1, request, null, 1);
                return true;
            } catch (Exception e) {
                this.mMonitorBinder = null;
                e.printStackTrace();
                return false;
            } finally {
                request.recycle();
            }
        }
        return false;
    }

    public boolean synergyOperate() {
        return this.mMonitorBinder != null && this.mSynergyOperate == 1;
    }

    public void sendKeyboardCaps() {
        Slog.d(TAG, "Send caps event from keyboard.");
        isPad();
    }

    public boolean isPad() {
        return Build.IS_TABLET;
    }

    private static boolean isCustomizedInputMethod(String inputMethodId) {
        int endIndex;
        String inputMethodName = "";
        if (!TextUtils.isEmpty(inputMethodId) && (endIndex = inputMethodId.indexOf(47)) > 0) {
            inputMethodName = inputMethodId.substring(0, endIndex);
        }
        return customizedInputMethodList.contains(inputMethodName);
    }

    public void initNoCustomizedInputMethodData(List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> noCustomizedInputMethodList, String lastInputMethodId, int lastInputMethodSubtypeId, int NOT_A_SUBTYPE_ID) {
        int subtypeId;
        Slog.v(TAG, "initNoCustomizedInputMethodData  noCustomizedInputMethodList:" + noCustomizedInputMethodList);
        this.noCustomizedCheckedItem = -1;
        int N = noCustomizedInputMethodList.size();
        this.mNoCustomizedIms = new InputMethodInfo[N];
        this.mNoCustomizedSubtypeIds = new int[N];
        for (int i = 0; i < N; i++) {
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem item = noCustomizedInputMethodList.get(i);
            this.mNoCustomizedIms[i] = item.mImi;
            this.mNoCustomizedSubtypeIds[i] = item.mSubtypeId;
            if (this.mNoCustomizedIms[i].getId().equals(lastInputMethodId) && ((subtypeId = this.mNoCustomizedSubtypeIds[i]) == NOT_A_SUBTYPE_ID || ((lastInputMethodSubtypeId == NOT_A_SUBTYPE_ID && subtypeId == 0) || subtypeId == lastInputMethodSubtypeId))) {
                this.noCustomizedCheckedItem = i;
            }
        }
    }

    public List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> filterNotCustomziedInputMethodList(List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList) {
        Slog.v(TAG, "filterNotCustomziedInputMethodList.");
        List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> noCustomizedInputMethodList = new ArrayList<>();
        Iterator<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> iterator = imList.iterator();
        while (iterator.hasNext()) {
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem item = iterator.next();
            if (!isCustomizedInputMethod(item.mImi.getId())) {
                noCustomizedInputMethodList.add(item);
                iterator.remove();
            }
        }
        return noCustomizedInputMethodList;
    }

    public void generateView(boolean isNotContainsCustomizedInputMethod, List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList, AlertDialog.Builder builder, final InputMethodMenuController.ImeSubtypeListAdapter noCustomziedImeListAdapter, final InputMethodMenuController.ImeSubtypeListAdapter customziedImeListAdapter, Context context) {
        Slog.v(TAG, "Start genertating customized view.");
        this.dialogContext = context;
        if (imList.size() == 0) {
            return;
        }
        LayoutInflater inflater = (LayoutInflater) this.dialogContext.getSystemService(LayoutInflater.class);
        int customViewId = this.dialogContext.getResources().getIdentifier("input_method_switch_no_customized_list_view", "layout", MIUIXPACKAGE);
        View customView = inflater.inflate(customViewId, (ViewGroup) null);
        if (customView == null) {
            return;
        }
        int noCustomizedListViewId = this.dialogContext.getResources().getIdentifier("noCustomizedListView", "id", MIUIXPACKAGE);
        final ListView noCustomListView = (ListView) customView.findViewById(noCustomizedListViewId);
        if (noCustomListView == null) {
            return;
        }
        int indicatorId = this.dialogContext.getResources().getIdentifier("indicator", "id", MIUIXPACKAGE);
        final ImageView indicator = (ImageView) customView.findViewById(indicatorId);
        int noCustomizedListViewTitleId = this.dialogContext.getResources().getIdentifier("noCustomizedListViewTitle", "id", MIUIXPACKAGE);
        View noCustomizedListViewTitle = customView.findViewById(noCustomizedListViewTitleId);
        this.isExpanded = customziedImeListAdapter.mCheckedItem == -1;
        final int openIndicatorDrawableId = this.dialogContext.getResources().getIdentifier("listview_open_indicator", "drawable", MIUIXPACKAGE);
        final int closeIndicatorDrawableId = this.dialogContext.getResources().getIdentifier("listview_close_indicator", "drawable", MIUIXPACKAGE);
        if (!this.isExpanded) {
            noCustomListView.setVisibility(8);
        } else {
            noCustomListView.setVisibility(0);
            indicator.setImageResource(openIndicatorDrawableId);
            setNoCustomizedInputMethodListViewHeight(noCustomListView, noCustomziedImeListAdapter);
        }
        noCustomListView.setAdapter((ListAdapter) noCustomziedImeListAdapter);
        noCustomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.android.server.inputmethod.InputMethodManagerServiceImpl.1
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                customziedImeListAdapter.mCheckedItem = -1;
                customziedImeListAdapter.notifyDataSetChanged();
                noCustomziedImeListAdapter.mCheckedItem = position;
                noCustomziedImeListAdapter.notifyDataSetChanged();
            }
        });
        noCustomizedListViewTitle.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.inputmethod.InputMethodManagerServiceImpl.2
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                if (!InputMethodManagerServiceImpl.this.isExpanded) {
                    indicator.setImageResource(openIndicatorDrawableId);
                    noCustomListView.setVisibility(0);
                    InputMethodManagerServiceImpl.setNoCustomizedInputMethodListViewHeight(noCustomListView, noCustomziedImeListAdapter);
                    InputMethodManagerServiceImpl.this.isExpanded = true;
                    return;
                }
                indicator.setImageResource(closeIndicatorDrawableId);
                noCustomListView.setVisibility(8);
                InputMethodManagerServiceImpl.this.isExpanded = false;
            }
        });
        builder.setView(customView);
        if (isNotContainsCustomizedInputMethod) {
            Slog.v(TAG, "There's no customized ime currently, so customized ime view will hidden");
            int dividerLineId = this.dialogContext.getResources().getIdentifier("dividerLine", "id", MIUIXPACKAGE);
            View dividerLine = customView.findViewById(dividerLineId);
            if (dividerLine != null) {
                noCustomizedListViewTitle.setVisibility(8);
                dividerLine.setVisibility(8);
            }
        }
    }

    public void setDialogImmersive(AlertDialog dialog) {
        if (isPad()) {
            return;
        }
        Window w = dialog.getWindow();
        w.addFlags(-2147481344);
        w.getAttributes().setFitInsetsSides(0);
        final View decorView = w.getDecorView();
        clearFitSystemWindow(decorView);
        decorView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() { // from class: com.android.server.inputmethod.InputMethodManagerServiceImpl.3
            @Override // android.view.View.OnApplyWindowInsetsListener
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                Insets gestureInsets = insets.getInsets(WindowInsets.Type.mandatorySystemGestures());
                int mHeight = gestureInsets.bottom;
                if (mHeight > 0) {
                    View view = decorView;
                    view.setPaddingRelative(view.getPaddingStart(), decorView.getPaddingTop(), decorView.getPaddingEnd(), decorView.getPaddingBottom() + mHeight);
                }
                decorView.setOnApplyWindowInsetsListener(null);
                return WindowInsets.CONSUMED;
            }
        });
    }

    private static void clearFitSystemWindow(View view) {
        if (view != null) {
            view.setFitsSystemWindows(false);
            if (view instanceof ViewGroup) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                    clearFitSystemWindow(((ViewGroup) view).getChildAt(i));
                }
            }
        }
    }

    public static void setNoCustomizedInputMethodListViewHeight(ListView noCustomListView, InputMethodMenuController.ImeSubtypeListAdapter adapter) {
        int height;
        int childrenCount = adapter.getCount();
        View childView = adapter.getView(0, (View) null, noCustomListView);
        childView.measure(0, 0);
        int childHeight = childView.getMeasuredHeight();
        if (childrenCount < 3) {
            height = childHeight * childrenCount;
        } else {
            height = childHeight * 3;
        }
        ViewGroup.LayoutParams layoutParams = noCustomListView.getLayoutParams();
        layoutParams.height = height;
        noCustomListView.setLayoutParams(layoutParams);
    }

    public void updateItemView(View view, CharSequence imeName, CharSequence subtypeName, int position, int checkedItem, ViewGroup parent) {
        int firstTextViewId = this.dialogContext.getResources().getIdentifier("title", "id", MIUIXPACKAGE);
        int secondTextViewId = this.dialogContext.getResources().getIdentifier("summary", "id", MIUIXPACKAGE);
        TextView firstTextView = (TextView) view.findViewById(firstTextViewId);
        TextView secondTextView = (TextView) view.findViewById(secondTextViewId);
        if (TextUtils.isEmpty(subtypeName)) {
            firstTextView.setText(imeName);
            secondTextView.setVisibility(8);
        } else {
            firstTextView.setText(imeName);
            secondTextView.setText(subtypeName);
            secondTextView.setVisibility(0);
        }
        int radioButtonId = this.dialogContext.getResources().getIdentifier("radio", "id", MIUIXPACKAGE);
        RadioButton radioButton = (RadioButton) view.findViewById(radioButtonId);
        radioButton.setChecked(position == checkedItem);
        ((ListView) parent).setChoiceMode(0);
        if (position == checkedItem) {
            view.setActivated(true);
        } else {
            view.setActivated(false);
        }
    }

    public int getNoCustomizedCheckedItem() {
        return this.noCustomizedCheckedItem;
    }

    public InputMethodInfo[] getNoCustomizedIms() {
        return this.mNoCustomizedIms;
    }

    public int[] getNoCustomizedSubtypeIds() {
        return this.mNoCustomizedSubtypeIds;
    }

    public void removeCustomTitle(AlertDialog.Builder dialogBuilder, View switchingDialogTitleView) {
        dialogBuilder.setCustomTitle((View) null);
        dialogBuilder.setTitle(17041558);
    }

    public void dismissWithoutAnimation(AlertDialog alertDialog) {
        try {
            Method method = alertDialog.getClass().getDeclaredMethod("dismissWithoutAnimation", new Class[0]);
            method.invoke(alertDialog, new Object[0]);
        } catch (Exception e) {
            Slog.e(TAG, "Reflect dismissWithoutAnimation exception!");
        }
    }

    public Notification buildImeNotification(Notification.Builder imeSwitcherNotification, Context context) {
        Notification notification = imeSwitcherNotification.build();
        notification.extras.putParcelable("miui.appIcon", Icon.createWithResource(context, 285737189));
        return notification;
    }

    public boolean shouldClearShowForcedFlag(Context context, int uid) {
        String[] packages;
        boolean result = false;
        if (context == null || (packages = context.getPackageManager().getPackagesForUid(uid)) == null || packages.length == 0) {
            return false;
        }
        for (String packageName : packages) {
            result |= MIUI_HOME.equals(packageName);
        }
        return result;
    }

    public boolean isInputMethodWindowInvisibleByScreenOn(int imeWindowVis) {
        boolean z = this.mScreenStatus;
        if (z && this.mScreenOffLastTime && imeWindowVis == 0) {
            this.mScreenOffLastTime = false;
            return true;
        }
        if (z) {
            this.mScreenOffLastTime = false;
        }
        return false;
    }

    public void setScreenOff(boolean inputMethodWindowDisplayed) {
        this.mScreenOffLastTime = inputMethodWindowDisplayed;
    }

    public void setScreenStatus(boolean screenStatus) {
        this.mScreenStatus = screenStatus;
    }

    public void updateSessionId(int sessionId) {
        if (!synergyOperate()) {
            return;
        }
        this.mSessionId = sessionId;
    }

    public void commitTextForSynergy(IInputContext inputContext, String text, int newCursorPosition) {
        try {
            Class<?> clazz = inputContext.getClass();
            Method declaredMethod = clazz.getDeclaredMethod("commitTextForSynergy", String.class, Integer.TYPE);
            declaredMethod.invoke(inputContext, text, Integer.valueOf(newCursorPosition));
        } catch (Exception e) {
            Slog.e(TAG, "Reflect commitTextForSynergy exception!");
        }
    }
}
