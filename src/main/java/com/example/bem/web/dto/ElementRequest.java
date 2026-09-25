package com.example.bem.web.dto;

/**
 * One blade element in a request.
 *
 * @param rOverR  dimensionless radius r/R in (0, 1]
 * @param chord   local chord in metres (or in units of R, consistent with radius)
 * @param twistDeg geometric twist angle in degrees
 */
public record ElementRequest(Double rOverR, Double chord, Double twistDeg) {
}
