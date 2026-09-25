package com.example.bem.aero;

/**
 * Inflow geometry at one blade element.
 *
 * <p>For an axial-flow wind turbine the inflow angle follows from the velocity
 * triangle (Burton et al., <i>Wind Energy Handbook</i>, Eq. 3.55):
 * <pre>
 *            U (1 - a)
 * tan phi = -----------------
 *           omega r (1 + a')
 * </pre>
 * and the local speed ratio is lambda_r = lambda (r/R), so
 * {@code tan phi = (1 - a) / (lambda_r (1 + a'))}. The angle of attack is
 * {@code alpha = phi - theta}.
 */
public final class FlowAngles {

    private FlowAngles() {
    }

    /**
     * Inflow angle in radians, always kept inside (0, pi/2).
     *
     * @param a          axial induction factor
     * @param aPrime     tangential induction factor
     * @param localSpeedRatio lambda * rOverR, strictly positive
     */
    public static double inflowAngleRad(double a, double aPrime, double localSpeedRatio) {
        double axial = 1.0 - a;
        if (axial < 1e-6) {
            axial = 1e-6;
        }
        double tangential = localSpeedRatio * (1.0 + aPrime);
        if (tangential < 1e-6) {
            tangential = 1e-6;
        }
        return Math.atan2(axial, tangential);
    }

    /** Angle of attack in radians. */
    public static double angleOfAttackRad(double inflowRad, double twistRad) {
        return inflowRad - twistRad;
    }
}
