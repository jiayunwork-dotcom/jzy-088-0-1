package com.example.bem.aero;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowAnglesTest {

    private static final double TOL = 1e-12;

    @Test
    void zeroInductionGivesArctanOfInverseLocalSpeedRatio() {
        // tan phi = 1 / lambda_r with a = a' = 0
        double phi = FlowAngles.inflowAngleRad(0.0, 0.0, 2.0);
        assertEquals(Math.atan(0.5), phi, TOL);
    }

    @Test
    void axialInductionLowersInflowAngle() {
        double noInduction = FlowAngles.inflowAngleRad(0.0, 0.0, 5.0);
        double withAxial = FlowAngles.inflowAngleRad(0.25, 0.0, 5.0);
        assertTrue(withAxial < noInduction);
        assertEquals(Math.atan(0.75 / 5.0), withAxial, TOL);
    }

    @Test
    void tangentialInductionAlsoLowersInflowAngle() {
        double noInduction = FlowAngles.inflowAngleRad(0.0, 0.0, 5.0);
        double withTangential = FlowAngles.inflowAngleRad(0.0, 0.1, 5.0);
        assertTrue(withTangential < noInduction);
    }

    @Test
    void angleOfAttackIsInflowMinusTwist() {
        double phi = Math.toRadians(12.0);
        double theta = Math.toRadians(5.0);
        assertEquals(7.0, Math.toDegrees(FlowAngles.angleOfAttackRad(phi, theta)), TOL);
    }
}
