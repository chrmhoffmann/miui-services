package miui.android.animation.utils;

import miui.android.animation.utils.LinkNode;
/* loaded from: classes.dex */
public class LinkNode<T extends LinkNode> {
    public T next;

    public void addToTail(T node) {
        for (LinkNode<T> head = this; head != node; head = head.next) {
            if (head.next == null) {
                head.next = node;
                return;
            }
        }
    }

    public T remove() {
        T node = this.next;
        this.next = null;
        return node;
    }

    public T destroy() {
        LinkNode<T> node;
        do {
            node = remove();
        } while (node != null);
        return null;
    }

    public int size() {
        int i = 0;
        for (LinkNode<T> head = this; head.next != null; head = head.next) {
            i++;
        }
        return i;
    }
}
