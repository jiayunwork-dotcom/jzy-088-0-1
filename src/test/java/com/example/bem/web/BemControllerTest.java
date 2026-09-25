package com.example.bem.web;

import com.example.bem.service.SampleData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end HTTP tests for the two analysis capabilities, the error contract
 * and the built-in three-bladed example.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper json = new ObjectMapper();

    private static final double BETZ = 16.0 / 27.0;
    private static final double EPS = 1e-9;

    private String sampleRotorJson(double lambda, int blades, boolean tipLoss) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"blades\":").append(blades)
                .append(",\"tipSpeedRatio\":").append(lambda)
                .append(",\"airfoilName\":\"").append(SampleData.AIRFOIL_NAME).append('"')
                .append(",\"radiusM\":2.0,\"tipLoss\":").append(tipLoss)
                .append(",\"elements\":[");
        var blade = SampleData.sampleBlade();
        for (int i = 0; i < blade.size(); i++) {
            var e = blade.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"rOverR\":").append(e.rOverR())
                    .append(",\"chord\":").append(e.chord())
                    .append(",\"twistDeg\":").append(e.twistDeg()).append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void rotorEndpointReturnsEveryStationWithAlphaAndElementalLoads() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/bem/rotor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRotorJson(7.0, 3, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations", hasSize(18)))
                .andReturn();
        JsonNode node = body(result);
        assertEquals(18, node.get("stations").size());
        assertTrue(node.at("/stations/0/angleOfAttackDeg").asDouble() != 0.0);
        assertTrue(node.at("/stations/0/dThrustPerSpan").asDouble() > 0.0);
        assertTrue(node.at("/stations/0/dTorquePerSpan").asDouble() > 0.0);
        double cp = node.get("powerCoefficient").asDouble();
        assertTrue(cp > 0.4, "design Cp should be substantial: " + cp);
        assertTrue(cp < BETZ, "Cp must be below Betz: " + cp);
        assertTrue(node.get("thrustCoefficient").asDouble() > 0.0);
        assertTrue(node.get("powerW").asDouble() > 0.0);
    }

    @Test
    void stationEndpointReturnsInductionsAndRotorCoefficients() throws Exception {
        String content = sampleRotorJson(7.0, 3, true)
                .replaceFirst("\\}$", ",\"stationROverR\":0.54059}");
        MvcResult result = mockMvc.perform(post("/api/bem/station")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = body(result);
        assertEquals(0.54059, node.get("stationROverR").asDouble(), 1e-6);
        double a = node.get("axialInduction").asDouble();
        assertTrue(a > 0.0 && a < 0.5, "mid-blade a=" + a);
        assertTrue(node.get("tangentialInduction").asDouble() >= 0.0);
        double cp = node.get("powerCoefficient").asDouble();
        assertTrue(cp > 0.4 && cp < BETZ);
        assertTrue(node.get("thrustCoefficient").asDouble() > 0.0);
        assertTrue(node.get("inflowDeg").asDouble() > 0.0);
        assertTrue(node.get("angleOfAttackDeg").asDouble() != 0.0);
    }

    @Test
    void nonPositiveTipSpeedRatioReturnsMachineCodedError() throws Exception {
        mockMvc.perform(post("/api/bem/rotor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sampleRotorJson(0.0, 3, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TIP_SPEED_RATIO_NOT_POSITIVE"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void tooFewStationsReturnMachineCodedError() throws Exception {
        String twoStations =
                "{\"blades\":3,\"tipSpeedRatio\":7,\"airfoilName\":\""
                        + SampleData.AIRFOIL_NAME
                        + "\",\"elements\":[{\"rOverR\":0.2,\"chord\":0.1,\"twistDeg\":10},"
                        + "{\"rOverR\":0.9,\"chord\":0.04,\"twistDeg\":2}]}";
        mockMvc.perform(post("/api/bem/rotor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(twoStations))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOT_ENOUGH_STATIONS"))
                .andExpect(jsonPath("$.message", containsString("3")));
    }

    @Test
    void nonMonotonicRadiusReturnsMachineCodedError() throws Exception {
        // Reorder the third station above the fourth one (sample mu values are
        // rounded, so use the exact serialized value).
        String bad = sampleRotorJson(7.0, 3, true)
                .replace("\"rOverR\":0.2476", "\"rOverR\":0.99");
        mockMvc.perform(post("/api/bem/rotor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RADIUS_NOT_MONOTONIC"));
    }

    @Test
    void unknownAirfoilReturnsNotFoundWithCode() throws Exception {
        String body = sampleRotorJson(7.0, 3, true)
                .replace(SampleData.AIRFOIL_NAME, "ghost");
        mockMvc.perform(post("/api/bem/rotor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AIRFOIL_NOT_FOUND"));
    }

    @Test
    void malformedJsonReturnsInvalidJsonCode() throws Exception {
        mockMvc.perform(post("/api/bem/rotor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));
    }

    @Test
    void exampleEndpointDescribesBuiltInRotor() throws Exception {
        mockMvc.perform(get("/api/example").param("tipSpeedRatio", "7.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blades").value(3))
                .andExpect(jsonPath("$.airfoilName").value(SampleData.AIRFOIL_NAME))
                .andExpect(jsonPath("$.elements", hasSize(18)));
    }

    @Test
    void exampleAnalysisProducesPositiveSubBetzPower() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/example/analyze").param("tipSpeedRatio", "7.0"))
                .andExpect(status().isOk())
                .andReturn();
        double cp = body(result).get("powerCoefficient").asDouble();
        assertTrue(cp > 0.0 && cp < BETZ + EPS, "example Cp=" + cp);
    }

    @Test
    void airfoilRegistrationLifecycleWorksOverHttp() throws Exception {
        String polar = "{\"name\":\"test-polar\",\"points\":["
                + "{\"alphaDeg\":0,\"cl\":0.4,\"cd\":0.01},"
                + "{\"alphaDeg\":5,\"cl\":1.0,\"cd\":0.02}]}";
        mockMvc.perform(get("/api/airfoils/test-polar"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/airfoils")
                        .contentType(MediaType.APPLICATION_JSON).content(polar))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test-polar"));
        mockMvc.perform(get("/api/airfoils/test-polar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(2)));
        mockMvc.perform(post("/api/airfoils")
                        .contentType(MediaType.APPLICATION_JSON).content(polar))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AIRFOIL_ALREADY_EXISTS"));
        mockMvc.perform(delete("/api/airfoils/test-polar"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/airfoils/test-polar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void emptyPolarOverHttpIsRejectedWithCode() throws Exception {
        mockMvc.perform(post("/api/airfoils")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"points\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AIRFOIL_TABLE_EMPTY"));
    }
}
