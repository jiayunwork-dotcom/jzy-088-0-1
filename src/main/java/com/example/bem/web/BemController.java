package com.example.bem.web;

import com.example.bem.airfoil.AirfoilRegistry;
import com.example.bem.domain.Airfoil;
import com.example.bem.service.BemService;
import com.example.bem.service.SampleData;
import com.example.bem.validation.ApiException;
import com.example.bem.validation.ErrorCode;
import com.example.bem.validation.RotorRequestValidator;
import com.example.bem.web.dto.AirfoilRequest;
import com.example.bem.web.dto.ElementRequest;
import com.example.bem.web.dto.RotorAnalysisRequest;
import com.example.bem.web.dto.RotorAnalysisResponse;
import com.example.bem.web.dto.StationAnalysisRequest;
import com.example.bem.web.dto.StationAnalysisResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * HTTP surface of the BEM service. Two aerodynamic capabilities are exposed:
 * <ul>
 *   <li>{@code POST /api/bem/station} — one station's axial/tangential
 *       induction factors plus integrated rotor power/thrust coefficients;</li>
 *   <li>{@code POST /api/bem/rotor} — the full blade sequence with angle of
 *       attack, elemental thrust and elemental torque at every radius.</li>
 * </ul>
 * Airfoil polars are managed as named resources under {@code /api/airfoils};
 * a built-in three-bladed example is available under {@code /api/example}.
 * No view layer and no authentication are provided.
 */
@RestController
@RequestMapping("/api")
public class BemController {

    private final RotorRequestValidator validator;
    private final BemService bemService;
    private final AirfoilRegistry registry;

    public BemController(RotorRequestValidator validator,
                         BemService bemService,
                         AirfoilRegistry registry) {
        this.validator = validator;
        this.bemService = bemService;
        this.registry = registry;
    }

    /** Full-blade analysis: per-station alpha, elemental thrust and torque. */
    @PostMapping("/bem/rotor")
    @ResponseStatus(HttpStatus.OK)
    public RotorAnalysisResponse analyseRotor(@RequestBody RotorAnalysisRequest request) {
        var input = validator.validateRotor(request, registry::find);
        return bemService.analyseRotor(input);
    }

    /** Single-station analysis: induction factors at one station, rotor Cp/Ct. */
    @PostMapping("/bem/station")
    @ResponseStatus(HttpStatus.OK)
    public StationAnalysisResponse analyseStation(@RequestBody StationAnalysisRequest request) {
        var base = new RotorAnalysisRequest(
                request.blades(),
                request.tipSpeedRatio(),
                request.airfoilName(),
                request.elements(),
                request.radiusM(),
                request.freeStreamMs(),
                request.densityKgM3(),
                request.tipLoss());
        var input = validator.validateRotor(base, registry::find);
        double station = request.stationROverR() == null
                ? input.elements().get(input.elements().size() - 1).rOverR()
                : request.stationROverR();
        return bemService.analyseStation(input, station);
    }

    // ---------------------------------------------------------------- airfoils

    @PostMapping("/airfoils")
    @ResponseStatus(HttpStatus.CREATED)
    public Airfoil registerAirfoil(@RequestBody AirfoilRequest request) {
        Airfoil airfoil = validator.validateAirfoilForRegistration(request);
        Airfoil previous = registry.find(airfoil.name()).orElse(null);
        if (previous != null) {
            throw new ApiException(ErrorCode.AIRFOIL_ALREADY_EXISTS,
                    "an airfoil polar named '" + airfoil.name()
                            + "' already exists; DELETE it first to replace", 409);
        }
        return registry.register(airfoil);
    }

    @GetMapping("/airfoils")
    public List<Airfoil> listAirfoils() {
        return List.copyOf(registry.all());
    }

    @GetMapping("/airfoils/{name}")
    public Airfoil getAirfoil(@PathVariable String name) {
        return registry.find(name)
                .orElseThrow(() -> new ApiException(ErrorCode.AIRFOIL_NOT_FOUND,
                        "no airfoil polar registered under name '" + name + "'", 404));
    }

    @DeleteMapping("/airfoils/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAirfoil(@PathVariable String name) {
        if (!registry.remove(name)) {
            throw new ApiException(ErrorCode.AIRFOIL_NOT_FOUND,
                    "no airfoil polar registered under name '" + name + "'", 404);
        }
    }

    // ----------------------------------------------------------------- example

    /** The built-in three-bladed rotor definition (blade + named polar). */
    @GetMapping("/example")
    public Map<String, Object> example(@RequestParam(defaultValue = "7.0") double tipSpeedRatio) {
        if (!(tipSpeedRatio > 0.0)) {
            throw new ApiException(ErrorCode.TIP_SPEED_RATIO_NOT_POSITIVE,
                    "tipSpeedRatio must be positive, got: " + tipSpeedRatio);
        }
        List<ElementRequest> elements = SampleData.sampleBlade().stream()
                .map(e -> new ElementRequest(e.rOverR(), e.chord(), e.twistDeg()))
                .toList();
        return Map.of(
                "airfoilName", SampleData.AIRFOIL_NAME,
                "blades", SampleData.DESIGN_BLADES,
                "radiusM", SampleData.RADIUS_M,
                "designTipSpeedRatio", SampleData.DESIGN_LAMBDA,
                "tipSpeedRatio", tipSpeedRatio,
                "elements", elements);
    }

    /** Convenience: analyse the built-in three-bladed example at a lambda. */
    @PostMapping("/example/analyze")
    public RotorAnalysisResponse analyzeExample(
            @RequestParam(defaultValue = "7.0") double tipSpeedRatio,
            @RequestParam(defaultValue = "true") boolean tipLoss) {
        if (!(tipSpeedRatio > 0.0)) {
            throw new ApiException(ErrorCode.TIP_SPEED_RATIO_NOT_POSITIVE,
                    "tipSpeedRatio must be positive, got: " + tipSpeedRatio);
        }
        RotorAnalysisRequest request = new RotorAnalysisRequest(
                SampleData.DESIGN_BLADES,
                tipSpeedRatio,
                SampleData.AIRFOIL_NAME,
                SampleData.sampleBlade().stream()
                        .map(e -> new ElementRequest(e.rOverR(), e.chord(), e.twistDeg()))
                        .toList(),
                SampleData.RADIUS_M,
                8.0,
                1.225,
                tipLoss);
        return bemService.analyseRotor(validator.validateRotor(request, registry::find));
    }
}
