package com.example.bem.domain;

/**
 * A single blade element: dimensionless radial position r/R, chord and twist.
 *
 * @param rOverR  radial position divided by rotor radius, in (0, 1]
 * @param chord   local chord length (m)
 * @param twistDeg geometric twist angle, degrees; relative to the rotor plane,
 *                 positive towards stall (the zero-lift line offset convention)
 */
public record BladeElement(double rOverR, double chord, double twistDeg) {
}
