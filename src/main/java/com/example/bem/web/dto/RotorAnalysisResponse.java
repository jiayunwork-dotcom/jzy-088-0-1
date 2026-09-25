package com.example.bem.web.dto;

import java.util.List;

/**
 * Full-blade analysis response: per-station detail plus integrated rotor
 * performance. {@code dThrustPerSpan}/{@code dTorquePerSpan} at the stations
 * are N/m and N&middot;m/m; {@code thrustN}, {@code torqueNm}, {@code powerW}
 * are integrated over the blade span.
 */
public record RotorAnalysisResponse(
        int blades,
        double tipSpeedRatio,
        boolean tipLossApplied,
        String airfoilName,
        double powerCoefficient,
        double thrustCoefficient,
        double thrustN,
        double torqueNm,
        double powerW,
        List<ElementResultDto> stations) {
}
