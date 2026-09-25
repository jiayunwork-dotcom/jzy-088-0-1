package com.example.bem.aero;

import com.example.bem.domain.Airfoil;
import com.example.bem.domain.BladeElement;
import com.example.bem.domain.ElementResult;

/**
 * Per-element blade-element-momentum iteration.
 *
 * <p>Each iteration performs the standard BEM fixed point:
 * <ol>
 *   <li>compute inflow angle phi from the current a, a';</li>
 *   <li>alpha = phi - theta, interpolate Cl, Cd from the polar;</li>
 *   <li>resolve loads normal/tangential to the rotor plane and form the local
 *       thrust coefficient C_T = sigma (Cl cos phi + Cd sin phi) / sin^2 phi;</li>
 *   <li>recover a from the appropriate branch of {@link WakeModel}
 *       (simple momentum below a_c, Buhl parabola in the turbulent-wake
 *       region) and a' from the angular-momentum balance;</li>
 *   <li>under-relax and repeat until both inductions settle.</li>
 * </ol>
 * The Prandtl tip-loss factor F is carried in every momentum relation; with
 * tip losses disabled F is 1.
 *
 * <p>This class holds only the iterative core. Integration along the blade
 * lives in {@link BladeIntegration}; neither knows about HTTP.
 */
public final class InductionSolver {

    private final SolverSettings settings;

    public InductionSolver() {
        this(SolverSettings.defaults());
    }

    public InductionSolver(SolverSettings settings) {
        this.settings = settings;
    }

    /**
     * Solve one blade element.
     *
     * @param element  geometry at the station (mu = r/R, chord, twist)
     * @param airfoil  polar table already registered for this analysis
     * @param blades   number of blades B
     * @param tipSpeedRatio lambda
     * @param tipLoss  whether the Prandtl correction is applied
     */
    public ElementResult solve(BladeElement element, Airfoil airfoil, int blades,
                               double tipSpeedRatio, boolean tipLoss) {
        double mu = element.rOverR();
        double localLambda = tipSpeedRatio * mu;
        double solidity = blades * element.chord() / (2.0 * Math.PI * mu);
        double twistRad = Math.toRadians(element.twistDeg());

        double a = 0.0;
        double aPrime = 0.0;
        double phi = FlowAngles.inflowAngleRad(a, aPrime, localLambda);
        double f = 1.0;

        for (int iter = 0; iter < settings.maxIterations(); iter++) {
            phi = FlowAngles.inflowAngleRad(a, aPrime, localLambda);
            f = tipLoss ? PrandtlTipLoss.factor(blades, mu, phi) : 1.0;

            double alphaRad = FlowAngles.angleOfAttackRad(phi, twistRad);
            double[] clCd = PolarInterpolator.liftDrag(airfoil, Math.toDegrees(alphaRad));
            double cl = clCd[0];
            double cd = clCd[1];

            double sinPhi = Math.sin(phi);
            double cosPhi = Math.cos(phi);
            // Normal and tangential force coefficients relative to rotor plane.
            double cn = cl * cosPhi + cd * sinPhi;
            double cy = cl * sinPhi - cd * cosPhi;

            // Local thrust coefficient carries the relative-velocity factor:
            // C_T = sigma Cn (W/U)^2 = sigma Cn (1-a)^2 / sin^2 phi = K (1-a)^2.
            double k = solidity * cn / (sinPhi * sinPhi);

            double aNew = WakeModel.solveAxialInduction(f, k);
            double aPrimeNew = WakeModel.solveTangentialInduction(
                    f, solidity, cy, phi, 1.0 - a);

            double aRelaxed = relax(a, aNew);
            double aPrimeRelaxed = relax(aPrime, aPrimeNew);

            double da = Math.abs(aRelaxed - a);
            double dap = Math.abs(aPrimeRelaxed - aPrime);
            a = aRelaxed;
            aPrime = aPrimeRelaxed;

            // Definite divergence: a leaving the physical interval hard, or NaN.
            if (Double.isNaN(a) || Double.isNaN(aPrime) || a > 1.5 || aPrime > 10.0) {
                throw new ConvergenceException(
                        "induction iteration diverged", mu, iter + 1);
            }

            if (da < settings.tolerance() && dap < settings.tolerance()) {
                return assemble(element, blades, tipSpeedRatio, a, aPrime,
                        phi, f, cl, cd, cn, cy);
            }
        }
        throw new ConvergenceException(
                "induction iteration failed to converge", mu, settings.maxIterations());
    }

    private double relax(double oldValue, double newValue) {
        double w = settings.relaxation();
        return w * newValue + (1.0 - w) * oldValue;
    }

    private ElementResult assemble(BladeElement element, int blades, double tipSpeedRatio,
                                   double a, double aPrime,
                                   double phi, double f, double cl, double cd,
                                   double cn, double cy) {
        // Element results are reported nondimensionally against dynamic
        // pressure 1/2 rho U^2 and rotor radius R:
        //   dT* = B c/R (W/U)^2 cn ,  dQ* = B c/R mu (W/U)^2 cy .
        // The integration layer restores physical units, so this kernel stays
        // free of rho, U and R.
        double wOverU = element.rOverR() * tipSpeedRatio * (1.0 + aPrime);
        double wSquared = (1.0 - a) * (1.0 - a) + wOverU * wOverU;
        double dThrustStar = blades * element.chord() * wSquared * cn;
        double dTorqueStar = blades * element.chord() * element.rOverR() * wSquared * cy;
        return new ElementResult(
                element.rOverR(),
                a,
                aPrime,
                Math.toDegrees(phi),
                Math.toDegrees(phi) - element.twistDeg(),
                cl,
                cd,
                f,
                dThrustStar,
                dTorqueStar);
    }
}
