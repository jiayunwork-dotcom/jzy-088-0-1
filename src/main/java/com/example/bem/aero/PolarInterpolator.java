package com.example.bem.aero;

import com.example.bem.domain.Airfoil;
import com.example.bem.domain.PolarPoint;

/**
 * Piecewise linear interpolation of lift/drag coefficients against angle of
 * attack. Outside the tabulated range the end values are held (clamped), which
 * is the standard behaviour for a finite polar table; input validation
 * guarantees the table itself is non-empty and ordered by increasing alpha.
 */
public final class PolarInterpolator {

    private PolarInterpolator() {
    }

    /** Interpolated coefficient pair at {@code alphaDeg}. */
    public static double[] liftDrag(Airfoil airfoil, double alphaDeg) {
        var points = airfoil.points();
        if (points.isEmpty()) {
            throw new IllegalArgumentException("airfoil table is empty");
        }
        int n = points.size();
        if (n == 1 || alphaDeg <= points.get(0).alphaDeg()) {
            PolarPoint p = points.get(0);
            return new double[] {p.cl(), p.cd()};
        }
        if (alphaDeg >= points.get(n - 1).alphaDeg()) {
            PolarPoint p = points.get(n - 1);
            return new double[] {p.cl(), p.cd()};
        }
        // Binary search for the bracketing interval.
        int lo = 0;
        int hi = n - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (points.get(mid).alphaDeg() <= alphaDeg) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        PolarPoint p0 = points.get(lo);
        PolarPoint p1 = points.get(hi);
        double span = p1.alphaDeg() - p0.alphaDeg();
        double t = (alphaDeg - p0.alphaDeg()) / span;
        return new double[] {
                p0.cl() + t * (p1.cl() - p0.cl()),
                p0.cd() + t * (p1.cd() - p0.cd())
        };
    }
}
