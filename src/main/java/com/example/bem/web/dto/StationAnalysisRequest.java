package com.example.bem.web.dto;

import java.util.List;

/**
 * Single-station BEM analysis request. The whole blade is still required
 * (and validated) because rotor coefficients are integrated along it;
 * {@code stationROverR} selects which station's induction factors are
 * highlighted in the response.
 */
public record StationAnalysisRequest(
        Integer blades,
        Double tipSpeedRatio,
        String airfoilName,
        List<ElementRequest> elements,
        Double stationROverR,
        Double radiusM,
        Double freeStreamMs,
        Double densityKgM3,
        Boolean tipLoss) {
}
