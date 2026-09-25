package com.example.bem.aero;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WakeModelTest {

    private static final double TOL = 1e-9;

    @Test
    void simpleBranchMatchesQuarterAtKEqualsFourThirds() {
        // K = 4F/3 -> a = 1/4
        double a = WakeModel.solveAxialInduction(1.0, 4.0 / 3.0);
        assertEquals(0.25, a, TOL);
        assertTrue(a < WakeModel.A_CRITICAL);
    }

    @Test
    void branchesJoinContinuouslyAtCriticalLoad() {
        double f = 0.8;
        double kCrit = 8.0 * f / 3.0;
        double a = WakeModel.solveAxialInduction(f, kCrit);
        assertEquals(WakeModel.A_CRITICAL, a, 1e-6);
        // CT from both branches must agree at the switch.
        double ctSimple = WakeModel.simpleThrustCoefficient(f, WakeModel.A_CRITICAL);
        double ctBuhl = buhlCt(f, WakeModel.A_CRITICAL);
        assertEquals(ctSimple, ctBuhl, 1e-9);
    }

    @Test
    void highThrustBranchStaysAboveCriticalAndBelowOne() {
        // Heavily loaded element at F = 1.
        double a = WakeModel.solveAxialInduction(1.0, 8.0);
        assertTrue(a >= WakeModel.A_CRITICAL);
        assertTrue(a < 1.0);
        // Consistency: Buhl relation reproduces K(1-a)^2.
        double ct = buhlCt(1.0, a);
        assertEquals(8.0 * (1 - a) * (1 - a), ct, 1e-7);
    }

    @Test
    void highThrustBranchIsStableForSmallTipLoss() {
        // F near the tip is small; no NaN or a = 1 allowed.
        double a = WakeModel.solveAxialInduction(0.05, 2.0);
        assertTrue(a >= 0.0 && a < 1.0);
        assertEquals(a, a, TOL);
    }

    @Test
    void tangentialInductionZeroForZeroTangentialLoad() {
        double ap = WakeModel.solveTangentialInduction(1.0, 0.1, 0.0, 0.2, 0.7);
        assertEquals(0.0, ap, TOL);
    }

    private double buhlCt(double f, double a) {
        return 8.0 / 9.0 + (4.0 * f - 40.0 / 9.0) * a
                + (50.0 / 9.0 - 4.0 * f) * a * a;
    }
}
