package com.example.bem.service;

import com.example.bem.aero.BladeIntegration;
import com.example.bem.aero.ConvergenceException;
import com.example.bem.domain.Airfoil;
import com.example.bem.domain.ElementResult;
import com.example.bem.domain.RotorResult;
import com.example.bem.validation.ApiException;
import com.example.bem.validation.ErrorCode;
import com.example.bem.validation.RotorRequestValidator.ValidatedRotor;
import com.example.bem.web.dto.ElementResultDto;
import com.example.bem.web.dto.RotorAnalysisResponse;
import com.example.bem.web.dto.StationAnalysisResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestration layer: feeds validated input into the aerodynamic modules and
 * shapes the results for the HTTP layer. No request parsing or web types leak
 * into the {@code aero} package and no iteration logic lives here.
 */
@Service
public class BemService {

    private final BladeIntegration integration;

    public BemService(BladeIntegration integration) {
        this.integration = integration;
    }

    /** Analyse every station of a validated rotor. */
    public RotorAnalysisResponse analyseRotor(ValidatedRotor input) {
        RotorResult result = run(input);
        return new RotorAnalysisResponse(
                input.blades(),
                input.tipSpeedRatio(),
                input.tipLoss(),
                input.airfoil().name(),
                result.powerCoefficient(),
                result.thrustCoefficient(),
                result.thrustN(),
                result.torqueNm(),
                result.powerW(),
                toStationDtos(result.elements(), input));
    }

    /** Analyse the full rotor but return one selected station in the spotlight. */
    public StationAnalysisResponse analyseStation(ValidatedRotor input, double stationROverR) {
        if (!(stationROverR > 0.0 && stationROverR <= 1.0)) {
            throw new ApiException(ErrorCode.RADIUS_OUT_OF_RANGE,
                    "stationROverR must be within (0, 1], got: " + stationROverR);
        }
        RotorResult result = run(input);
        ElementResult station = pickStation(result.elements(), stationROverR);
        return new StationAnalysisResponse(
                input.blades(),
                input.tipSpeedRatio(),
                input.tipLoss(),
                input.airfoil().name(),
                station.rOverR(),
                station.a(),
                station.aPrime(),
                station.inflowDeg(),
                station.alphaDeg(),
                station.cl(),
                station.cd(),
                station.tipLossFactor(),
                result.powerCoefficient(),
                result.thrustCoefficient(),
                result.thrustN(),
                result.torqueNm(),
                result.powerW());
    }

    private RotorResult run(ValidatedRotor input) {
        try {
            return integration.analyse(
                    input.elements(),
                    input.airfoil(),
                    input.blades(),
                    input.tipSpeedRatio(),
                    input.tipLoss(),
                    input.radius(),
                    input.freeStream(),
                    input.density());
        } catch (ConvergenceException e) {
            throw new ApiException(ErrorCode.ITERATION_NOT_CONVERGED, e.getMessage(), 422);
        }
    }

    private ElementResult pickStation(List<ElementResult> stations, double requested) {
        ElementResult best = null;
        double bestDelta = Double.MAX_VALUE;
        for (ElementResult station : stations) {
            double delta = Math.abs(station.rOverR() - requested);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = station;
            }
        }
        if (best == null || bestDelta > 1e-6) {
            throw new ApiException(ErrorCode.STATION_NOT_FOUND,
                    "no blade element at r/R=" + requested
                            + "; nearest is " + (best == null ? "n/a" : best.rOverR()),
                    404);
        }
        return best;
    }

    private List<ElementResultDto> toStationDtos(List<ElementResult> elements, ValidatedRotor input) {
        double q = 0.5 * input.density() * input.freeStream() * input.freeStream();
        double radius = input.radius();
        return elements.stream()
                .map(e -> new ElementResultDto(
                        e.rOverR(),
                        e.a(),
                        e.aPrime(),
                        e.inflowDeg(),
                        e.alphaDeg(),
                        e.cl(),
                        e.cd(),
                        e.tipLossFactor(),
                        q * radius * e.dThrust(),
                        q * radius * radius * e.dTorque()))
                .toList();
    }
}
