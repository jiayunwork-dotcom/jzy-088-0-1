package com.example.bem.service;

import com.example.bem.airfoil.AirfoilRegistry;
import com.example.bem.domain.Airfoil;
import com.example.bem.domain.BladeElement;
import com.example.bem.domain.PolarPoint;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in worked example: a small three-bladed horizontal-axis rotor sized by
 * the ideal (Betz) design relations and a NACA 4412-style polar. Registered at
 * startup under the name {@link #AIRFOIL_NAME} so it can be referenced by
 * analyses and hand-checked.
 *
 * <p>Design relations used to build the blade (design lambda = 7,
 * design Cl = 1.0):
 * <pre>
 * phi_d = (2/3) arctan(1/(lambda mu))
 * c/R   = (8 pi mu)/(B Cl) (1 - cos phi_d)
 * theta = phi_d - alpha_d ,  alpha_d = 6 deg
 * </pre>
 * Stations run from mu = 0.15 to 0.98 (the exact tip mu = 1 is a Prandtl
 * singularity: F -&gt; 0 there).
 */
@Component
public class SampleData {

    public static final String AIRFOIL_NAME = "naca4412-sample";
    public static final int DESIGN_BLADES = 3;
    public static final double DESIGN_LAMBDA = 7.0;
    public static final double RADIUS_M = 2.0;

    private final AirfoilRegistry registry;

    public SampleData(AirfoilRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    void registerSamplePolar() {
        registry.register(sampleAirfoil());
    }

    public static Airfoil sampleAirfoil() {
        // Representative NACA 4412 polar (degrees, Cl, Cd) around Re ~ 1e6.
        double[][] rows = {
                {-20, -0.75, 0.040},
                {-18, -0.90, 0.030},
                {-16, -1.00, 0.022},
                {-14, -1.02, 0.016},
                {-12, -0.86, 0.0115},
                {-10, -0.66, 0.0085},
                {-8, -0.46, 0.0068},
                {-6, -0.24, 0.0062},
                {-4, -0.02, 0.0060},
                {-2, 0.20, 0.0060},
                {0, 0.42, 0.0061},
                {2, 0.64, 0.0063},
                {4, 0.84, 0.0067},
                {6, 1.02, 0.0074},
                {8, 1.17, 0.0088},
                {10, 1.28, 0.0120},
                {12, 1.34, 0.0175},
                {14, 1.36, 0.025},
                {16, 1.30, 0.035},
                {18, 1.20, 0.048},
                {20, 1.05, 0.063}
        };
        List<PolarPoint> points = new ArrayList<>(rows.length);
        for (double[] row : rows) {
            points.add(new PolarPoint(row[0], row[1], row[2]));
        }
        return new Airfoil(AIRFOIL_NAME, points);
    }

    /** 18-station three-bladed sample blade in ascending r/R. */
    public static List<BladeElement> sampleBlade() {
        int stations = 18;
        double mu0 = 0.15;
        double muTip = 0.98;
        double designCl = 1.0;
        double designAlphaDeg = 6.0;

        List<BladeElement> blade = new ArrayList<>(stations);
        for (int i = 0; i < stations; i++) {
            double mu = mu0 + (muTip - mu0) * i / (stations - 1);
            double phi = (2.0 / 3.0) * Math.atan(1.0 / (DESIGN_LAMBDA * mu));
            double chordOverR = (8.0 * Math.PI * mu)
                    / (DESIGN_BLADES * designCl) * (1.0 - Math.cos(phi));
            double twistDeg = Math.toDegrees(phi) - designAlphaDeg;
            blade.add(new BladeElement(round(mu, 5), round(chordOverR, 5), round(twistDeg, 4)));
        }
        return blade;
    }

    private static double round(double v, int digits) {
        double scale = Math.pow(10.0, digits);
        return Math.round(v * scale) / scale;
    }
}
