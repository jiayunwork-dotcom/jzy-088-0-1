package com.example.bem.validation;

import com.example.bem.airfoil.AirfoilRegistry;
import com.example.bem.airfoil.InMemoryAirfoilRegistry;
import com.example.bem.domain.Airfoil;
import com.example.bem.service.SampleData;
import com.example.bem.web.dto.AirfoilRequest;
import com.example.bem.web.dto.ElementRequest;
import com.example.bem.web.dto.PolarPointRequest;
import com.example.bem.web.dto.RotorAnalysisRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotorRequestValidatorTest {

    private final RotorRequestValidator validator = new RotorRequestValidator();
    private AirfoilRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryAirfoilRegistry();
        registry.register(SampleData.sampleAirfoil());
    }

    private List<ElementRequest> threeStations() {
        return List.of(
                new ElementRequest(0.2, 0.1, 15.0),
                new ElementRequest(0.5, 0.08, 8.0),
                new ElementRequest(0.9, 0.04, 2.0));
    }

    private RotorAnalysisRequest request(Integer blades, Double lambda,
                                         List<ElementRequest> stations, String airfoil) {
        return new RotorAnalysisRequest(blades, lambda, airfoil, stations,
                2.0, 8.0, 1.225, true);
    }

    private ApiException expect(ErrorCode code, RotorAnalysisRequest req) {
        ApiException ex = assertThrows(ApiException.class,
                () -> validator.validateRotor(req, registry::find));
        assertEquals(code, ex.getErrorCode());
        return ex;
    }

    @Test
    void rejectsNonPositiveTipSpeedRatio() {
        expect(ErrorCode.TIP_SPEED_RATIO_NOT_POSITIVE,
                request(3, 0.0, threeStations(), SampleData.AIRFOIL_NAME));
        expect(ErrorCode.TIP_SPEED_RATIO_NOT_POSITIVE,
                request(3, -2.0, threeStations(), SampleData.AIRFOIL_NAME));
    }

    @Test
    void rejectsFewerThanThreeStations() {
        ApiException ex = expect(ErrorCode.NOT_ENOUGH_STATIONS,
                request(3, 7.0,
                        List.of(new ElementRequest(0.2, 0.1, 15.0),
                                new ElementRequest(0.9, 0.04, 2.0)),
                        SampleData.AIRFOIL_NAME));
        assertTrue(ex.getMessage().contains("3"));
    }

    @Test
    void rejectsNonPositiveBladeCount() {
        expect(ErrorCode.BLADES_NOT_POSITIVE_INTEGER,
                request(0, 7.0, threeStations(), SampleData.AIRFOIL_NAME));
        expect(ErrorCode.BLADES_NOT_POSITIVE_INTEGER,
                request(-1, 7.0, threeStations(), SampleData.AIRFOIL_NAME));
    }

    @Test
    void rejectsNonMonotonicRadii() {
        List<ElementRequest> bad = List.of(
                new ElementRequest(0.2, 0.1, 15.0),
                new ElementRequest(0.5, 0.08, 8.0),
                new ElementRequest(0.4, 0.06, 5.0));
        expect(ErrorCode.RADIUS_NOT_MONOTONIC, request(3, 7.0, bad, SampleData.AIRFOIL_NAME));
    }

    @Test
    void rejectsRadiusOutsideRotor() {
        List<ElementRequest> bad = List.of(
                new ElementRequest(0.2, 0.1, 15.0),
                new ElementRequest(0.5, 0.08, 8.0),
                new ElementRequest(1.4, 0.04, 2.0));
        expect(ErrorCode.RADIUS_OUT_OF_RANGE, request(3, 7.0, bad, SampleData.AIRFOIL_NAME));
    }

    @Test
    void rejectsUnknownAirfoil() {
        ApiException ex = expect(ErrorCode.AIRFOIL_NOT_FOUND,
                request(3, 7.0, threeStations(), "no-such-polar"));
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void rejectsEmptyPolarOnRegistration() {
        ApiException ex = assertThrows(ApiException.class,
                () -> validator.validateAirfoilForRegistration(
                        new AirfoilRequest("empty", List.of())));
        assertEquals(ErrorCode.AIRFOIL_TABLE_EMPTY, ex.getErrorCode());
    }

    @Test
    void rejectsUnorderedPolar() {
        List<PolarPointRequest> rows = List.of(
                new PolarPointRequest(0.0, 0.4, 0.01),
                new PolarPointRequest(0.0, 0.5, 0.01));
        ApiException ex = assertThrows(ApiException.class,
                () -> validator.validateAirfoilForRegistration(new AirfoilRequest("dup", rows)));
        assertEquals(ErrorCode.POLAR_NOT_ORDERED, ex.getErrorCode());
    }

    @Test
    void rejectsMissingAirfoilName() {
        expect(ErrorCode.AIRFOIL_NAME_MISSING,
                request(3, 7.0, threeStations(), "  "));
    }

    @Test
    void acceptsAndDefaultsValidRequest() {
        var validated = validator.validateRotor(
                new RotorAnalysisRequest(3, 7.0, SampleData.AIRFOIL_NAME, threeStations(),
                        null, null, null, null),
                registry::find);
        assertEquals(3, validated.blades());
        assertEquals(1.0, validated.radius());
        assertEquals(8.0, validated.freeStream());
        assertEquals(1.225, validated.density());
        assertTrue(validated.tipLoss());
    }
}
