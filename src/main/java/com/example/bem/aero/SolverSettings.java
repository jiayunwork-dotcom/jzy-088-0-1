package com.example.bem.aero;

/**
 * Fixed-point iteration settings for the blade-element induction solve.
 *
 * @param maxIterations hard iteration cap; exceeding it is a failure
 * @param tolerance     convergence threshold on max(|Δa|, |Δa'|)
 * @param relaxation    under-relaxation factor w in (0, 1]; new = w·new + (1-w)·old
 */
public record SolverSettings(int maxIterations, double tolerance, double relaxation) {

    public static SolverSettings defaults() {
        return new SolverSettings(200, 1.0e-6, 0.3);
    }

    public SolverSettings {
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("maxIterations must be positive");
        }
        if (tolerance <= 0 || tolerance >= 1) {
            throw new IllegalArgumentException("tolerance must be in (0, 1)");
        }
        if (relaxation <= 0 || relaxation > 1) {
            throw new IllegalArgumentException("relaxation must be in (0, 1]");
        }
    }
}
