package com.example.bem.domain;

/**
 * One row of an airfoil polar: angle of attack with lift/drag coefficients.
 *
 * @param alphaDeg angle of attack in degrees
 * @param cl       lift coefficient
 * @param cd       drag coefficient (must be positive)
 */
public record PolarPoint(double alphaDeg, double cl, double cd) {
}
