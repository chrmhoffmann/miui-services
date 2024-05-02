package miui.android.animation.physics;
/* loaded from: classes.dex */
public interface PhysicsOperator {
    void getParameters(float[] fArr, double[] dArr);

    double updateVelocity(double d, double d2, double d3, double d4, double... dArr);
}
