package com.example.bem.aero;

/**
 * Prandtl tip-loss correction (Glauert, <i>Aerodynamic Theory</i>):
 * <pre>
 * f = B (R - r) / (2 r sin phi) = B (1 - mu) / (2 mu sin phi)
 * F = (2/pi) arccos(e^-f)
 * </pre>
 * Only the tip correction is applied; no hub-loss model is included at this
 * service layer.
 */
public final class PrandtlTipLoss {

    private PrandtlTipLoss() {
    }

    /**
     * @param blades  number of blades B
     * @param rOverR  local dimensionless radius mu in (0, 1]
     * @param inflowRad inflow angle phi in radians
     */
    public static double factor(int blades, double rOverR, double inflowRad) {
        double sinPhi = Math.sin(inflowRad);
        if (sinPhi < 1e-6) {
            sinPhi = 1e-6;
        }
        double mu = Math.min(rOverR, 1.0);
        double f = blades * (1.0 - mu) / (2.0 * mu * sinPhi);
        // exp(-f) saturates quickly; arccos argument kept inside [-1, 1].
        double arg = Math.exp(-Math.max(-60.0, Math.min(60.0, f)));
        arg = Math.max(-1.0, Math.min(1.0, arg));
        double factor = 2.0 / Math.PI * Math.acos(arg);
        // At the very tip F tends toward 0; keep it finite and positive so the
        // momentum denominators cannot collapse to zero.
        return Math.max(factor, 1e-4);
    }
}
