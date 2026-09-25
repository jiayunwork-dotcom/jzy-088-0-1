package com.example.bem.domain;

import java.util.List;

/**
 * A named airfoil polar table, ordered by ascending angle of attack.
 *
 * @param name   unique registration name
 * @param points polar rows, sorted by increasing alpha
 */
public record Airfoil(String name, List<PolarPoint> points) {
}
