package miui.android.animation.physics;
/* loaded from: classes.dex */
public class FrictionOperator implements PhysicsOperator {
    @Override // miui.android.animation.physics.PhysicsOperator
    public void getParameters(float[] factors, double[] params) {
        double friction = factors[0];
        params[0] = 1.0d - Math.pow(2.718281828459045d, (-4.199999809265137d) * friction);
    }

    @Override // miui.android.animation.physics.PhysicsOperator
    public double updateVelocity(double velocity, double p0, double p1, double deltaT, double... factors) {
        return Math.pow(1.0d - p0, deltaT) * velocity;
    }
}
