package com.example.bem.aero;

import com.example.bem.domain.Airfoil;
import com.example.bem.domain.BladeElement;
import com.example.bem.domain.ElementResult;
import com.example.bem.domain.RotorResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Trapezoidal integration of converged element loads along the blade.
 *
 * <p>With element kernels reporting nondimensional per-unit-span loads
 * <pre>
 * dT* = B (c/R) (W/U)^2 Cn ,  dQ* = B (c/R) mu (W/U)^2 Cy
 * </pre>
 * the integrated coefficients follow from the disk-area normalisations
 * T = q pi R^2 Ct and Q = q pi R^3 Cq (q = 1/2 rho U^2):
 * <pre>
 * C_T = (1/pi)   integral of dT* dmu
 * C_P = (lambda/pi) integral of dQ* dmu
 * </pre>
 * Physical units are restored by T = q R integral of dT* dmu and
 * Q = q R^2 integral of dQ* dmu; P = Q omega with omega = lambda U/R.
 *
 * <p>This class performs integration only; iteration is {@link InductionSolver}.
 */
public final class BladeIntegration {

    private final InductionSolver solver;

    public BladeIntegration(InductionSolver solver) {
        this.solver = solver;
    }

    /**
     * Analyse a complete rotor.
     *
     * @param elements    blade stations in ascending r/R
     * @param airfoil     polar used at every station
     * @param blades      number of blades
     * @param tipSpeedRatio lambda
     * @param tipLoss     apply Prandtl tip-loss correction
     * @param radius      rotor radius R (m), for physical unit restoration
     * @param freeStream  wind speed U (m/s)
     * @param density     air density rho (kg/m^3)
     */
    public RotorResult analyse(List<BladeElement> elements, Airfoil airfoil, int blades,
                               double tipSpeedRatio, boolean tipLoss,
                               double radius, double freeStream, double density) {
        List<ElementResult> results = new ArrayList<>(elements.size());
        for (BladeElement element : elements) {
            results.add(solver.solve(element, airfoil, blades, tipSpeedRatio, tipLoss));
        }

        double integralT = trapezoid(results, ElementResult::dThrust);
        double integralQ = trapezoid(results, ElementResult::dTorque);
        double ct = integralT / Math.PI;
        double cp = tipSpeedRatio * integralQ / Math.PI;

        double dynamicPressure = 0.5 * density * freeStream * freeStream;
        double thrust = dynamicPressure * radius * integralT;
        double torque = dynamicPressure * radius * radius * integralQ;
        double omega = tipSpeedRatio * freeStream / radius;
        double power = torque * omega;

        return new RotorResult(results, cp, ct, thrust, torque, power, tipSpeedRatio);
    }

    private double trapezoid(List<ElementResult> results,
                             java.util.function.ToDoubleFunction<ElementResult> value) {
        double sum = 0.0;
        for (int i = 1; i < results.size(); i++) {
            ElementResult prev = results.get(i - 1);
            ElementResult cur = results.get(i);
            double dx = cur.rOverR() - prev.rOverR();
            sum += 0.5 * dx * (value.applyAsDouble(prev) + value.applyAsDouble(cur));
        }
        return sum;
    }
}
