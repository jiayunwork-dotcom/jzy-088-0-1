package com.example.bem.domain;

import java.util.List;

/**
 * Rotor-level result of a blade element momentum analysis.
 *
 * <p>The element list is ordered by increasing r/R. Elemental thrust and
 * torque are reported both as per-unit-span physical values ({@code dThrust},
 * {@code dTorque}, N/m and N) and the service integrates them by the
 * trapezoidal rule over r/R.
 *
 * @param elements        converged state at every station
 * @param powerCoefficient Cp, rotor power over wind power through the disk
 * @param thrustCoefficient Ct, rotor thrust over dynamic pressure times disk area
 * @param thrustN         integrated rotor thrust (N)
 * @param torqueNm        integrated rotor torque (N m)
 * @param powerW          rotor shaft power (W), torque times rotational speed
 * @param tipSpeedRatio   lambda used for the analysis
 */
public record RotorResult(
        List<ElementResult> elements,
        double powerCoefficient,
        double thrustCoefficient,
        double thrustN,
        double torqueNm,
        double powerW,
        double tipSpeedRatio) {
}
