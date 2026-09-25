package com.example.bem.aero;

import com.example.bem.domain.Airfoil;
import com.example.bem.domain.PolarPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolarInterpolatorTest {

    private static final Airfoil POLAR = new Airfoil("p", List.of(
            new PolarPoint(0.0, 0.4, 0.01),
            new PolarPoint(5.0, 1.0, 0.02),
            new PolarPoint(10.0, 1.3, 0.04)));

    @Test
    void interpolatesLinearlyAtInteriorPoint() {
        double[] clcd = PolarInterpolator.liftDrag(POLAR, 2.5);
        assertEquals(0.7, clcd[0], 1e-12);
        assertEquals(0.015, clcd[1], 1e-12);
    }

    @Test
    void returnsExactTableValueAtNode() {
        double[] clcd = PolarInterpolator.liftDrag(POLAR, 5.0);
        assertEquals(1.0, clcd[0], 1e-12);
        assertEquals(0.02, clcd[1], 1e-12);
    }

    @Test
    void clampsOutsideRangeToEndPoints() {
        double[] low = PolarInterpolator.liftDrag(POLAR, -8.0);
        assertEquals(0.4, low[0], 1e-12);
        double[] high = PolarInterpolator.liftDrag(POLAR, 42.0);
        assertEquals(1.3, high[0], 1e-12);
    }
}
