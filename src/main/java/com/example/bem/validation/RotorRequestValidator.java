package com.example.bem.validation;

import com.example.bem.domain.Airfoil;
import com.example.bem.domain.BladeElement;
import com.example.bem.domain.PolarPoint;
import com.example.bem.web.dto.AirfoilRequest;
import com.example.bem.web.dto.ElementRequest;
import com.example.bem.web.dto.PolarPointRequest;
import com.example.bem.web.dto.RotorAnalysisRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Pure input validation, independent of the aerodynamic and HTTP layers.
 * Every illegal condition maps to a stable {@link ErrorCode} and a readable
 * message; valid requests are converted into domain objects.
 */
@Component
public class RotorRequestValidator {

    /** Minimum number of radial stations accepted for an analysis. */
    public static final int MIN_STATIONS = 3;

    /** Validate the common rotor/analysis envelope and map to domain elements. */
    public ValidatedRotor validateRotor(RotorAnalysisRequest request,
                                       Function<String, Optional<Airfoil>> airfoilLookup) {
        if (request == null) {
            throw new ApiException(ErrorCode.INVALID_JSON, "request body is required");
        }
        int blades = validateBlades(request.blades());
        double lambda = validateTipSpeedRatio(request.tipSpeedRatio());
        Airfoil airfoil = validateAirfoilName(request.airfoilName(), airfoilLookup);
        List<BladeElement> elements = validateElements(request.elements());
        double radius = validatePositive(request.radiusM() == null ? 1.0 : request.radiusM(),
                "radiusM", ErrorCode.ROTOR_PARAMETER_INVALID);
        double freeStream = validatePositive(request.freeStreamMs() == null ? 8.0 : request.freeStreamMs(),
                "freeStreamMs", ErrorCode.ROTOR_PARAMETER_INVALID);
        double density = validatePositive(request.densityKgM3() == null ? 1.225 : request.densityKgM3(),
                "densityKgM3", ErrorCode.ROTOR_PARAMETER_INVALID);
        boolean tipLoss = request.tipLoss() == null || request.tipLoss();
        return new ValidatedRotor(blades, lambda, airfoil, elements,
                radius, freeStream, density, tipLoss);
    }

    /** Validate a polar submitted for registration; returns a domain airfoil. */
    public Airfoil validateAirfoilForRegistration(AirfoilRequest request) {
        if (request == null) {
            throw new ApiException(ErrorCode.INVALID_JSON, "request body is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ApiException(ErrorCode.AIRFOIL_NAME_MISSING,
                    "airfoil name must be a non-empty string");
        }
        if (request.points() == null || request.points().isEmpty()) {
            throw new ApiException(ErrorCode.AIRFOIL_TABLE_EMPTY,
                    "airfoil polar '" + request.name() + "' must contain at least one point");
        }
        List<PolarPoint> points = new ArrayList<>(request.points().size());
        double prevAlpha = -Double.MAX_VALUE;
        for (int i = 0; i < request.points().size(); i++) {
            PolarPointRequest row = request.points().get(i);
            if (row == null || row.alphaDeg() == null || row.cl() == null || row.cd() == null) {
                throw new ApiException(ErrorCode.POLAR_POINT_INVALID,
                        "polar point at index " + i + " must define alphaDeg, cl and cd");
            }
            if (Double.isNaN(row.alphaDeg()) || Double.isInfinite(row.alphaDeg())
                    || Double.isNaN(row.cl()) || Double.isInfinite(row.cl())
                    || Double.isNaN(row.cd()) || Double.isInfinite(row.cd())) {
                throw new ApiException(ErrorCode.POLAR_POINT_INVALID,
                        "polar point at index " + i + " contains NaN or infinite values");
            }
            if (i > 0 && row.alphaDeg() <= prevAlpha) {
                throw new ApiException(ErrorCode.POLAR_NOT_ORDERED,
                        "polar points must be strictly ordered by increasing alphaDeg; "
                                + "violation at index " + i);
            }
            prevAlpha = row.alphaDeg();
            points.add(new PolarPoint(row.alphaDeg(), row.cl(), row.cd()));
        }
        return new Airfoil(request.name().trim(), points);
    }

    private int validateBlades(Integer blades) {
        if (blades == null || blades <= 0) {
            throw new ApiException(ErrorCode.BLADES_NOT_POSITIVE_INTEGER,
                    "number of blades must be a positive integer, got: " + blades);
        }
        return blades;
    }

    private double validateTipSpeedRatio(Double lambda) {
        if (lambda == null || !(lambda > 0.0) || Double.isNaN(lambda) || Double.isInfinite(lambda)) {
            throw new ApiException(ErrorCode.TIP_SPEED_RATIO_NOT_POSITIVE,
                    "tip speed ratio must be a positive number, got: " + lambda);
        }
        return lambda;
    }

    private Airfoil validateAirfoilName(String name,
                                       Function<String, Optional<Airfoil>> airfoilLookup) {
        if (name == null || name.isBlank()) {
            throw new ApiException(ErrorCode.AIRFOIL_NAME_MISSING,
                    "airfoilName must reference a registered polar");
        }
        return airfoilLookup.apply(name.trim())
                .orElseThrow(() -> new ApiException(ErrorCode.AIRFOIL_NOT_FOUND,
                        "no airfoil polar registered under name '" + name + "'", 404));
    }

    private List<BladeElement> validateElements(List<ElementRequest> raw) {
        if (raw == null || raw.size() < MIN_STATIONS) {
            int count = raw == null ? 0 : raw.size();
            throw new ApiException(ErrorCode.NOT_ENOUGH_STATIONS,
                    "at least " + MIN_STATIONS + " radial stations are required, got: " + count);
        }
        List<BladeElement> elements = new ArrayList<>(raw.size());
        double prevR = -Double.MAX_VALUE;
        for (int i = 0; i < raw.size(); i++) {
            ElementRequest e = raw.get(i);
            if (e == null || e.rOverR() == null || e.chord() == null || e.twistDeg() == null) {
                throw new ApiException(ErrorCode.RADIUS_OUT_OF_RANGE,
                        "element at index " + i + " must define rOverR, chord and twistDeg");
            }
            double mu = e.rOverR();
            if (!(mu > 0.0 && mu <= 1.0)) {
                throw new ApiException(ErrorCode.RADIUS_OUT_OF_RANGE,
                        "r/R at index " + i + " must be within (0, 1], got: " + mu);
            }
            if (i > 0 && mu <= prevR) {
                throw new ApiException(ErrorCode.RADIUS_NOT_MONOTONIC,
                        "radial stations must be strictly increasing; violation at index " + i);
            }
            prevR = mu;
            if (!(e.chord() > 0.0)) {
                throw new ApiException(ErrorCode.CHORD_NOT_POSITIVE,
                        "chord at r/R=" + mu + " must be positive, got: " + e.chord());
            }
            elements.add(new BladeElement(mu, e.chord(), e.twistDeg()));
        }
        return elements;
    }

    private double validatePositive(double value, String field, ErrorCode code) {
        if (!(value > 0.0) || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new ApiException(code, field + " must be a positive finite number, got: " + value);
        }
        return value;
    }

    /** Validated and defaulted analysis envelope. */
    public record ValidatedRotor(
            int blades,
            double tipSpeedRatio,
            Airfoil airfoil,
            List<BladeElement> elements,
            double radius,
            double freeStream,
            double density,
            boolean tipLoss) {
    }
}
