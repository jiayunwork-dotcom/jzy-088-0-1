package com.example.bem.config;

import com.example.bem.aero.BladeIntegration;
import com.example.bem.aero.InductionSolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the aerodynamic modules. The iterative core ({@link InductionSolver})
 * and the spanwise integration ({@link BladeIntegration}) stay independent
 * classes and are composed here rather than merged into one object.
 */
@Configuration
public class BemConfig {

    @Bean
    public InductionSolver inductionSolver() {
        return new InductionSolver();
    }

    @Bean
    public BladeIntegration bladeIntegration(InductionSolver inductionSolver) {
        return new BladeIntegration(inductionSolver);
    }
}
