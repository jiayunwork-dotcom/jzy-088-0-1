package com.example.bem.domain;

/**
 * Converged aerodynamic state at a single blade element.
 *
 * @param rOverR        dimensionless radius
 * @param a             axial induction factor
 * @param aPrime        tangential (angular) induction factor
 * @param inflowDeg     inflow angle relative to rotor plane, degrees
 * @param alphaDeg      angle of attack, degrees
 * @param cl            interpolated lift coefficient
 * @param cd            interpolated drag coefficient
 * @param tipLossFactor Prandtl tip loss factor F (1.0 when correction disabled)
 * @param dThrust       nondimensional elemental thrust per unit span,
 *                      B(c/R)(W/U)^2 Cn; multiply by q R = 1/2 rho U^2 R for N/m
 * @param dTorque       nondimensional elemental torque per unit span,
 *                      B(c/R)mu(W/U)^2 Cy; multiply by q R^2 for N (per unit span)
 */
public record ElementResult(
        double rOverR,
        double a,
        double aPrime,
        double inflowDeg,
        double alphaDeg,
        double cl,
        double cd,
        double tipLossFactor,
        double dThrust,
        double dTorque) {
}
