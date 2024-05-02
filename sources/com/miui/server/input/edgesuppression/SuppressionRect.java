package com.miui.server.input.edgesuppression;

import java.util.ArrayList;
/* loaded from: classes.dex */
public class SuppressionRect {
    private static final int NODE_DEFAULT = 0;
    private static final int TIME_DEFAULT = 0;
    private int bottomRightX;
    private int bottomRightY;
    private int position;
    private int topLeftX;
    private int topLeftY;
    private int type;
    private ArrayList list = new ArrayList();
    private int time = 0;
    private int node = 0;

    public SuppressionRect() {
    }

    public SuppressionRect(int type, int position) {
        this.type = type;
        this.position = position;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getTopLeftX() {
        return this.topLeftX;
    }

    public void setTopLeftX(int topLeftX) {
        this.topLeftX = topLeftX;
    }

    public int getTopLeftY() {
        return this.topLeftY;
    }

    public void setTopLeftY(int topLeftY) {
        this.topLeftY = topLeftY;
    }

    public int getBottomRightX() {
        return this.bottomRightX;
    }

    public void setBottomRightX(int bottomRightX) {
        this.bottomRightX = bottomRightX;
    }

    public int getBottomRightY() {
        return this.bottomRightY;
    }

    public void setBottomRightY(int bottomRightY) {
        this.bottomRightY = bottomRightY;
    }

    public int getTime() {
        return this.time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getNode() {
        return this.node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public void setValue(int type, int position, int topLeftX, int topLeftY, int bottomRightX, int bottomRightY) {
        this.type = type;
        this.position = position;
        this.topLeftX = topLeftX;
        this.topLeftY = topLeftY;
        this.bottomRightX = bottomRightX;
        this.bottomRightY = bottomRightY;
        this.time = 0;
        this.node = 0;
    }

    public void setEmpty() {
        setTopLeftX(0);
        setBottomRightY(0);
        setBottomRightX(0);
        setBottomRightY(0);
    }

    public String toString() {
        return "SuppressionRect{list=" + this.list + ", type=" + this.type + ", position=" + this.position + ", topLeftX=" + this.topLeftX + ", topLeftY=" + this.topLeftY + ", bottomRightX=" + this.bottomRightX + ", bottomRightY=" + this.bottomRightY + ", time=" + this.time + ", node=" + this.node + '}';
    }

    public ArrayList<Integer> getList() {
        if (this.list.size() != 0) {
            this.list.clear();
        }
        this.list.add(Integer.valueOf(this.type));
        this.list.add(Integer.valueOf(this.position));
        this.list.add(Integer.valueOf(this.topLeftX));
        this.list.add(Integer.valueOf(this.topLeftY));
        this.list.add(Integer.valueOf(this.bottomRightX));
        this.list.add(Integer.valueOf(this.bottomRightY));
        this.list.add(Integer.valueOf(this.time));
        this.list.add(Integer.valueOf(this.node));
        return this.list;
    }

    /* loaded from: classes.dex */
    public static class LeftTopHalfRect extends SuppressionRect {
        public LeftTopHalfRect(int type, int heightOfScreen, int widthOfRect) {
            setType(type);
            setPosition(2);
            setTopLeftX(0);
            setTopLeftY(0);
            setBottomRightX(widthOfRect);
            setBottomRightY(heightOfScreen / 2);
        }
    }

    /* loaded from: classes.dex */
    public static class RightTopHalfRect extends SuppressionRect {
        public RightTopHalfRect(int type, int heightOfScreen, int widthOfScreen, int widthOfRect) {
            setType(type);
            setPosition(3);
            setTopLeftX(widthOfScreen - widthOfRect);
            setTopLeftY(0);
            setBottomRightX(widthOfScreen);
            setBottomRightY(heightOfScreen / 2);
        }
    }

    /* loaded from: classes.dex */
    public static class LeftBottomHalfRect extends SuppressionRect {
        public LeftBottomHalfRect(int type, int heightOfScreen, int widthOfRect) {
            setType(type);
            setPosition(2);
            setTopLeftX(0);
            setTopLeftY(heightOfScreen / 2);
            setBottomRightX(widthOfRect);
            setBottomRightY(heightOfScreen);
        }
    }

    /* loaded from: classes.dex */
    public static class RightBottomHalfRect extends SuppressionRect {
        public RightBottomHalfRect(int type, int heightOfScreen, int widthOfScreen, int widthOfRect) {
            setType(type);
            setPosition(3);
            setTopLeftX(widthOfScreen - widthOfRect);
            setTopLeftY(heightOfScreen / 2);
            setBottomRightX(widthOfScreen);
            setBottomRightY(heightOfScreen);
        }
    }

    /* loaded from: classes.dex */
    public static class TopLeftHalfRect extends SuppressionRect {
        public TopLeftHalfRect(int type, int widthOfScreen, int widthOfRect) {
            setType(type);
            setPosition(0);
            setTopLeftX(0);
            setTopLeftY(0);
            setBottomRightX(widthOfScreen / 2);
            setBottomRightY(widthOfRect);
        }
    }

    /* loaded from: classes.dex */
    public static class TopRightHalfRect extends SuppressionRect {
        public TopRightHalfRect(int type, int widthOfScreen, int widthOfRect) {
            setType(type);
            setPosition(0);
            setTopLeftX(widthOfScreen / 2);
            setTopLeftY(0);
            setBottomRightX(widthOfScreen);
            setBottomRightY(widthOfRect);
        }
    }

    /* loaded from: classes.dex */
    public static class BottomLeftHalfRect extends SuppressionRect {
        public BottomLeftHalfRect(int type, int heightOfScreen, int widthOfScreen, int widthOfRect) {
            setType(type);
            setPosition(1);
            setTopLeftX(0);
            setTopLeftY(heightOfScreen - widthOfRect);
            setBottomRightX(widthOfScreen / 2);
            setBottomRightY(heightOfScreen);
        }
    }

    /* loaded from: classes.dex */
    public static class BottomRightHalfRect extends SuppressionRect {
        public BottomRightHalfRect(int type, int heightOfScreen, int widthOfScreen, int widthOfRect) {
            setType(type);
            setPosition(1);
            setTopLeftX(widthOfScreen / 2);
            setTopLeftY(heightOfScreen - widthOfRect);
            setBottomRightX(widthOfScreen);
            setBottomRightY(heightOfScreen);
        }
    }

    /* loaded from: classes.dex */
    public static class TopRect extends SuppressionRect {
        public TopRect(boolean isHorizontal, int type, int widthOfScreen, int widthOfRect) {
            setType(type);
            setPosition(0);
            if (isHorizontal) {
                setTopLeftX(0);
                setTopLeftY(0);
                setBottomRightX(widthOfScreen);
                setBottomRightY(widthOfRect);
                return;
            }
            setTopLeftX(0);
            setTopLeftY(0);
            setBottomRightX(0);
            setBottomRightY(0);
        }
    }

    /* loaded from: classes.dex */
    public static class BottomRect extends SuppressionRect {
        public BottomRect(boolean isHorizontal, int type, int heightOfScreen, int widthOfScreen, int widthOfRect) {
            setType(type);
            setPosition(1);
            if (isHorizontal) {
                setTopLeftX(0);
                setTopLeftY(heightOfScreen - widthOfRect);
                setBottomRightX(widthOfScreen);
                setBottomRightY(heightOfScreen);
                return;
            }
            setTopLeftX(0);
            setTopLeftY(0);
            setBottomRightX(0);
            setBottomRightY(0);
        }
    }

    /* loaded from: classes.dex */
    public static class LeftRect extends SuppressionRect {
        public LeftRect(int type, int heightOfScreen, int widthOfRect) {
            setType(type);
            setPosition(2);
            setTopLeftX(0);
            setTopLeftY(0);
            setBottomRightX(widthOfRect);
            setBottomRightY(heightOfScreen);
        }
    }

    /* loaded from: classes.dex */
    public static class RightRect extends SuppressionRect {
        public RightRect(int type, int heightOfScreen, int widthOfScreen, int widthOfRect) {
            setType(type);
            setPosition(3);
            setTopLeftX(widthOfScreen - widthOfRect);
            setTopLeftY(0);
            setBottomRightX(widthOfScreen);
            setBottomRightY(heightOfScreen);
        }
    }
}
