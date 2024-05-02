package miui.android.animation.physics;
/* loaded from: classes.dex */
public class AccelerateOperator implements PhysicsOperator {
    @Override // miui.android.animation.physics.PhysicsOperator
    public void getParameters(float[] factors, double[] params) {
        double acc = factors[0];
        params[0] = 1000.0d * acc;
    }

    @Override // miui.android.animation.physics.PhysicsOperator
    public double updateVelocity(double velocity, double p0, double p1, double deltaT, double... factors) {
        return (p0 * deltaT) + velocity;
    }
}
