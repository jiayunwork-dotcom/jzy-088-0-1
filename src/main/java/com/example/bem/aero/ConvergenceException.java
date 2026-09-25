package com.example.bem.aero;

/**
 * Thrown when the fixed-point induction iteration at a blade element fails to
 * reach the requested tolerance. The service must surface this as an error
 * rather than returning a diverged value that merely looks numeric.
 */
public class ConvergenceException extends RuntimeException {

    private final double rOverR;
    private final int iterations;

    public ConvergenceException(String message, double rOverR, int iterations) {
        super(message + " (r/R=" + rOverR + ", iterations=" + iterations + ")");
        this.rOverR = rOverR;
        this.iterations = iterations;
    }

    public double getROverR() {
        return rOverR;
    }

    public int getIterations() {
        return iterations;
    }
}
