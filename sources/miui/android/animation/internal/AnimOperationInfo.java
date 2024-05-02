package miui.android.animation.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import miui.android.animation.IAnimTarget;
import miui.android.animation.ValueTarget;
import miui.android.animation.property.FloatProperty;
/* loaded from: classes.dex */
class AnimOperationInfo {
    public final byte op;
    public final List<FloatProperty> propList;
    public volatile long sendTime;
    public final IAnimTarget target;
    public int usedCount = 0;

    public AnimOperationInfo(IAnimTarget target, byte op, String[] names, FloatProperty[] properties) {
        this.op = op;
        this.target = target;
        if (names == null || !(target instanceof ValueTarget)) {
            if (properties != null) {
                this.propList = Arrays.asList(properties);
                return;
            } else {
                this.propList = null;
                return;
            }
        }
        ValueTarget vt = (ValueTarget) target;
        this.propList = new ArrayList();
        for (String name : names) {
            this.propList.add(vt.getFloatProperty(name));
        }
    }

    public boolean isUsed() {
        List<FloatProperty> list = this.propList;
        int size = list == null ? 0 : list.size();
        int i = this.usedCount;
        if (size == 0) {
            if (i <= 0) {
                return false;
            }
        } else if (i != size) {
            return false;
        }
        return true;
    }

    public String toString() {
        StringBuilder append = new StringBuilder().append("AnimOperationInfo{target=").append(this.target).append(", op=").append((int) this.op).append(", propList=");
        List<FloatProperty> list = this.propList;
        return append.append(list != null ? Arrays.toString(list.toArray()) : null).append('}').toString();
    }
}
