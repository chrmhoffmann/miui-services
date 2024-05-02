package com.miui.server.rescue;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Slog;
import com.google.android.collect.Lists;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class ContactsHelper {

    /* loaded from: classes.dex */
    public class ContactsBean {
        private String name;
        private ArrayList<String> numList;

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ArrayList<String> getNumList() {
            return this.numList;
        }

        public void setNumList(ArrayList<String> numList) {
            this.numList = numList;
        }

        public ContactsBean() {
            ContactsHelper.this = this$0;
            this.numList = Lists.newArrayList();
        }

        public ContactsBean(String displayName, ArrayList<String> numList) {
            ContactsHelper.this = this$0;
            this.name = displayName;
            if (numList == null) {
                this.numList = Lists.newArrayList();
            } else {
                this.numList = numList;
            }
        }

        public String toString() {
            return "ContactsBean [name=" + this.name + ", numList=" + this.numList + "]";
        }
    }

    public ArrayList<ContactsBean> getContactsList(Context context) {
        ArrayList<ContactsBean> contactsList = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor == null || cursor.getCount() <= 0) {
            return contactsList;
        }
        while (cursor.moveToNext()) {
            ContactsBean bean = new ContactsBean();
            String name = cursor.getString(cursor.getColumnIndex("display_name"));
            if (name != null) {
                wirteNumbers(resolver, name, bean);
                contactsList.add(bean);
            }
        }
        cursor.close();
        return contactsList;
    }

    private void wirteNumbers(ContentResolver contentResolver, String name, ContactsBean bean) {
        if (name == null || TextUtils.isEmpty(name)) {
            return;
        }
        try {
            Cursor dataCursor = contentResolver.query(ContactsContract.Data.CONTENT_URI, new String[]{"data1"}, "display_name= ? ", new String[]{name}, null);
            if (dataCursor != null && dataCursor.getCount() > 0) {
                bean.setName(name);
                while (dataCursor.moveToNext()) {
                    String number = dataCursor.getString(dataCursor.getColumnIndex("data1"));
                    if (number != null && !TextUtils.isEmpty(number)) {
                        bean.getNumList().add(number);
                    }
                }
                dataCursor.close();
            }
        } catch (Exception e) {
            e.fillInStackTrace();
            Slog.e("BrokenScreenRescueService", "stackTrace: ", e);
        }
    }
}
