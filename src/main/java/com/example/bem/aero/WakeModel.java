package com.example.bem.aero;

/**
 * Axial momentum relation with the Buhl (2005) high-thrust correction.
 *
 * <p>For moderate induction the thrust coefficient is the simple
 * momentum-theory result
 * <pre>
 * C_T = 4 F a (1 - a)
 * </pre>
 * which becomes unstable past a = 1/2 (the turbulent-wake / vortex-ring
 * state). Above the switch point a_c = 0.4 the empirical Buhl parabola is
 * used instead:
 * <pre>
 * C_T = 8/9 + (4F - 40/9) a + (50/9 - 4F) a^2
 * </pre>
 * The two branches match in <em>value and slope</em> at a_c = 0.4 for any F
 * (Buhl 2005, NREL/SR-500-36830). Given C_T from the local loads, the
 * corresponding a is obtained analytically rather than by division through
 * (1-a), keeping the iteration stable in the turbulent-wake region.
 */
public final class WakeModel {

    /** Axial induction at which the simple formula gives way to the Buhl parabola. */
    public static final double A_CRITICAL = 0.4;

    private WakeModel() {
    }

    /** Thrust coefficient of the simple momentum branch. */
    public static double simpleThrustCoefficient(double f, double a) {
        return 4.0 * f * a * (1.0 - a);
    }

    /**
     * Solve the axial induction factor for the local loads.
     *
     * <p>Equating the local blade-element thrust coefficient
     * {@code C_T = K (1-a)^2}, with {@code K = sigma Cn / sin^2 phi}, to the
     * momentum relations gives:
     * <ul>
     *   <li>simple branch {@code 4 F a (1-a)} &rArr; {@code a = K/(K + 4F)};</li>
     *   <li>Buhl parabola when {@code K >= 8F/3}, solved as a quadratic for a.</li>
     * </ul>
     * No division by (1-a) is ever performed, so the turbulent-wake region
     * stays numerically stable.
     *
     * @param f tip-loss factor
     * @param k solidity-weighted normal load, sigma Cn / sin^2 phi
     * @return a in [0, 1)
     */
    public static double solveAxialInduction(double f, double k) {
        double kCrit = 8.0 * f / 3.0;
        if (k <= kCrit) {
            return k / (k + 4.0 * f);
        }
        // K(1-a)^2 = 8/9 + (4F - 40/9) a + (50/9 - 4F) a^2
        // -> A a^2 + B a + C = 0
        double c2 = 50.0 / 9.0 - 4.0 * f - k;
        double c1 = 4.0 * f - 40.0 / 9.0 + 2.0 * k;
        double c0 = 8.0 / 9.0 - k;
        double disc = c1 * c1 - 4.0 * c2 * c0;
        double root = Math.sqrt(Math.max(0.0, disc));
        double a1 = (-c1 + root) / (2.0 * c2);
        double a2 = (-c1 - root) / (2.0 * c2);
        // The Buhl branch is physical for a in [a_c, 1); the other quadratic
        // root lies outside that interval. Prefer a root in range, taking the
        // lower one if both look valid (the branch joins the simple curve at
        // a_c, so the physical solution is the first root above a_c).
        boolean r1Valid = a1 >= A_CRITICAL && a1 < 1.0;
        boolean r2Valid = a2 >= A_CRITICAL && a2 < 1.0;
        double a;
        if (r1Valid && r2Valid) {
            a = Math.min(a1, a2);
        } else if (r1Valid) {
            a = a1;
        } else if (r2Valid) {
            a = a2;
        } else {
            a = Math.max(a1, a2);
        }
        // Buhl's fit targets the [0.4, ~1.0) range; clamp strictly below 1.
        return Math.max(A_CRITICAL, Math.min(a, 0.999));
    }

    /**
     * Tangential induction from angular momentum balance with the
     * relative-velocity factor retained:
     * <pre>
     * a' = sigma Cy (1-a)^2 / (4 F sin phi cos phi - sigma Cy (1-a)^2)
     * </pre>
     */
    public static double solveTangentialInduction(double f, double solidity,
                                                   double cy, double inflowRad,
                                                   double oneMinusA) {
        double load = solidity * cy * oneMinusA * oneMinusA;
        double denom = 4.0 * f * Math.sin(inflowRad) * Math.cos(inflowRad) - load;
        if (Math.abs(denom) < 1e-12) {
            return 0.0;
        }
        double aPrime = load / denom;
        return aPrime < 0.0 ? 0.0 : aPrime;
    }
}
