package com.example.bem.web.dto;

/**
 * Single-station analysis response. Carries the selected station's induction
 * factors and angles as well as the rotor-level coefficients they belong to.
 */
public record StationAnalysisResponse(
        int blades,
        double tipSpeedRatio,
        boolean tipLossApplied,
        String airfoilName,
        double stationROverR,
        double axialInduction,
        double tangentialInduction,
        double inflowDeg,
        double angleOfAttackDeg,
        double cl,
        double cd,
        double tipLossFactor,
        double powerCoefficient,
        double thrustCoefficient,
        double thrustN,
        double torqueNm,
        double powerW) {
}
