package com.example.bem.web.dto;

import java.util.List;

/**
 * Full-blade BEM analysis request.
 *
 * @param blades       number of blades (positive integer)
 * @param tipSpeedRatio lambda (positive)
 * @param airfoilName  name of a previously registered polar
 * @param elements     blade stations, ascending r/R, at least three
 * @param radiusM      rotor radius R in metres, used to restore physical units
 * @param freeStreamMs wind speed U in m/s (defaults to 8)
 * @param densityKgM3  air density in kg/m^3 (defaults to 1.225)
 * @param tipLoss      apply Prandtl tip-loss correction (defaults to true)
 */
public record RotorAnalysisRequest(
        Integer blades,
        Double tipSpeedRatio,
        String airfoilName,
        List<ElementRequest> elements,
        Double radiusM,
        Double freeStreamMs,
        Double densityKgM3,
        Boolean tipLoss) {
}
