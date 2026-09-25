package com.example.bem.web.dto;

import java.util.List;

/**
 * A named airfoil polar submitted for registration.
 */
public record AirfoilRequest(String name, List<PolarPointRequest> points) {
}
