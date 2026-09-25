package com.example.bem.web.dto;

/**
 * One row of an airfoil polar in a request.
 */
public record PolarPointRequest(Double alphaDeg, Double cl, Double cd) {
}
