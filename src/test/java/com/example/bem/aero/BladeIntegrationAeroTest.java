package com.example.bem.aero;

import com.example.bem.domain.Airfoil;
import com.example.bem.domain.BladeElement;
import com.example.bem.domain.ElementResult;
import com.example.bem.domain.RotorResult;
import com.example.bem.service.SampleData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aerodynamic law tests that lock the correctness of the full BEM analysis.
 * Every numeric claim uses an explicit tolerance.
 */
class BladeIntegrationAeroTest {

    /** Betz limit 16/27. */
    private static final double BETZ = 16.0 / 27.0;

    private final Airfoil airfoil = SampleData.sampleAirfoil();
    private final List<BladeElement> blade = SampleData.sampleBlade();
    private final BladeIntegration integration = new BladeIntegration(new InductionSolver());

    private RotorResult run(double lambda, int blades, boolean tipLoss) {
        return integration.analyse(blade, airfoil, blades, lambda, tipLoss,
                SampleData.RADIUS_M, 8.0, 1.225);
    }

    @Test
    void builtInThreeBladedExampleIsPositiveAndBelowBetz() {
        RotorResult design = run(SampleData.DESIGN_LAMBDA, SampleData.DESIGN_BLADES, true);
        assertTrue(design.powerCoefficient() > 0.0, "Cp must be positive");
        assertTrue(design.powerCoefficient() < BETZ,
                "Cp " + design.powerCoefficient() + " must stay below Betz 0.5926");
        // A well-designed rotor at its design point should capture a useful share.
        assertTrue(design.powerCoefficient() > 0.4,
                "design Cp should be substantial, got " + design.powerCoefficient());
    }

    @Test
    void midSpanAxialInductionLiesBetweenZeroAndHalf() {
        RotorResult design = run(7.0, 3, true);
        List<ElementResult> stations = design.elements();
        int n = stations.size();
        // "Mid-blade region": central third of the stations, excluding hub/tip.
        for (int i = n / 3; i < 2 * n / 3; i++) {
            double a = stations.get(i).a();
            assertTrue(a > 0.0 && a < 0.5,
                    "a=" + a + " out of (0, 0.5) at r/R=" + stations.get(i).rOverR());
        }
    }

    @Test
    void powerCoefficientNeverExceedsBetzAcrossSweep() {
        for (double lambda = 2.0; lambda <= 12.0; lambda += 0.5) {
            RotorResult result = run(lambda, 3, true);
            assertTrue(result.powerCoefficient() > 0.0,
                    "Cp must stay positive at lambda=" + lambda);
            assertTrue(result.powerCoefficient() <= BETZ + 1e-9,
                    "Cp " + result.powerCoefficient() + " exceeds Betz at lambda=" + lambda);
        }
    }

    @Test
    void powerCoefficientRisesThenPlateausAsLambdaApproachesDesign() {
        double low = run(3.0, 3, true).powerCoefficient();
        double mid = run(5.0, 3, true).powerCoefficient();
        double design = run(7.0, 3, true).powerCoefficient();
        double after = run(8.0, 3, true).powerCoefficient();

        // Clear rise from the under-speed side toward the design point.
        assertTrue(mid > low + 0.15, "Cp should climb: " + low + " -> " + mid);
        assertTrue(design > mid, "Cp should keep climbing to design: " + mid + " -> " + design);
        // Plateau around design: neighbouring points stay within a small band,
        // rather than jumping around monotonically.
        assertTrue(Math.abs(design - after) < 0.05,
                "Cp should plateau near design: design=" + design + " after=" + after);
        // The design-point value is the global maximum of the sweep.
        assertTrue(design >= run(4.0, 3, true).powerCoefficient());
        assertTrue(design >= run(9.0, 3, true).powerCoefficient());
    }

    @Test
    void disablingTipLossRaisesTipElementThrust() {
        RotorResult withLoss = run(7.0, 3, true);
        RotorResult withoutLoss = run(7.0, 3, false);
        ElementResult tipWith = withLoss.elements().get(withLoss.elements().size() - 1);
        ElementResult tipWithout = withoutLoss.elements().get(withoutLoss.elements().size() - 1);
        assertTrue(tipWith.tipLossFactor() < 1.0,
                "Prandtl factor must be below 1 near the tip, got " + tipWith.tipLossFactor());
        assertTrue(tipWithout.tipLossFactor() == 1.0,
                "tip loss disabled implies F = 1");
        assertTrue(tipWithout.dThrust() > tipWith.dThrust(),
                "off-design tip dT " + tipWithout.dThrust()
                        + " should exceed tip-loss value " + tipWith.dThrust());
    }

    @Test
    void reducingBladeCountMarkedlyReducesPowerCoefficient() {
        double three = run(7.0, 3, true).powerCoefficient();
        double two = run(7.0, 2, true).powerCoefficient();
        double one = run(7.0, 1, true).powerCoefficient();
        assertTrue(three - two > 0.03,
                "dropping 3->2 blades should cut Cp noticeably: 3=" + three + " 2=" + two);
        assertTrue(two > one, "fewer blades should monotonically lower Cp: 2=" + two + " 1=" + one);
        assertTrue(one > 0.0 && one < BETZ);
    }

    @Test
    void iterationReportsFailureRatherThanReturningDivergedValues() {
        // A tiny iteration budget cannot converge the nonlinear station;
        // the solver must fail loudly instead of emitting a spurious number.
        InductionSolver impatient = new InductionSolver(new SolverSettings(3, 1e-12, 0.3));
        BladeIntegration broken = new BladeIntegration(impatient);
        assertThrows(ConvergenceException.class,
                () -> broken.analyse(blade, airfoil, 3, 7.0, true, 2.0, 8.0, 1.225));
    }
}
