package com.example.bem.aero;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrandtlTipLossTest {

    private static final double TOL = 1e-12;

    @Test
    void factorIsUnityAtTheHub() {
        // mu -> 0 means f -> infinity, F -> 1.
        double f = PrandtlTipLoss.factor(3, 0.01, Math.toRadians(10));
        assertEquals(1.0, f, 1e-6);
    }

    @Test
    void factorDropsTowardZeroAtTheTip() {
        double fHub = PrandtlTipLoss.factor(3, 0.3, Math.toRadians(10));
        double fTip = PrandtlTipLoss.factor(3, 0.98, Math.toRadians(10));
        assertTrue(fTip < fHub);
        assertTrue(fTip > 0.0);
        assertTrue(fTip < 0.8);
    }

    @Test
    void moreBladesRecoverMoreLoss() {
        double three = PrandtlTipLoss.factor(3, 0.95, Math.toRadians(8));
        double six = PrandtlTipLoss.factor(6, 0.95, Math.toRadians(8));
        assertTrue(six > three);
    }

    @Test
    void factorAlwaysInUnitInterval() {
        for (int b : new int[] {1, 2, 3, 6}) {
            for (double mu = 0.1; mu <= 0.999; mu += 0.05) {
                double f = PrandtlTipLoss.factor(b, mu, Math.toRadians(5));
                assertTrue(f > 0.0 && f <= 1.0 + TOL,
                        "F out of range: B=" + b + " mu=" + mu + " F=" + f);
            }
        }
    }
}
