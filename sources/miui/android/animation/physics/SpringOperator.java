package miui.android.animation.physics;
/* loaded from: classes.dex */
public class SpringOperator implements PhysicsOperator {
    double[] params;

    public SpringOperator() {
    }

    @Deprecated
    public SpringOperator(float damp, float response) {
        double[] dArr = new double[2];
        this.params = dArr;
        getParameters(new float[]{damp, response}, dArr);
    }

    @Deprecated
    public double updateVelocity(double velocity, float deltaT, float... factors) {
        if (this.params == null) {
            return velocity;
        }
        double[] doubleFactors = new double[factors.length];
        for (int i = 0; i < factors.length; i++) {
            doubleFactors[i] = factors[i];
        }
        double[] dArr = this.params;
        return updateVelocity(velocity, dArr[0], dArr[1], deltaT, doubleFactors);
    }

    @Override // miui.android.animation.physics.PhysicsOperator
    public void getParameters(float[] factors, double[] params) {
        double damp = factors[0];
        double response = factors[1];
        params[0] = Math.pow(6.283185307179586d / response, 2.0d);
        params[1] = Math.min((12.566370614359172d * damp) / response, 60.0d);
    }

    @Override // miui.android.animation.physics.PhysicsOperator
    public double updateVelocity(double velocity, double p0, double p1, double deltaT, double... factors) {
        double targetValue = factors[0];
        double curValue = factors[1];
        return ((float) ((targetValue - curValue) * p0 * deltaT)) + (velocity * (1.0d - (p1 * deltaT)));
    }
}
