package com.example.bem.web.dto;

/**
 * Per-station result in an analysis response. Elemental thrust and torque are
 * physical per-unit-span quantities (N/m and N respectively per unit span,
 * torque units N&middot;m per metre span).
 */
public record ElementResultDto(
        double rOverR,
        double axialInduction,
        double tangentialInduction,
        double inflowDeg,
        double angleOfAttackDeg,
        double cl,
        double cd,
        double tipLossFactor,
        double dThrustPerSpan,
        double dTorquePerSpan) {
}
