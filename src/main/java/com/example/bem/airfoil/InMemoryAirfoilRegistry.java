package com.example.bem.airfoil;

import com.example.bem.domain.Airfoil;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concurrent-map backed {@link AirfoilRegistry}. Safe for the concurrent
 * requests of the embedded HTTP service.
 */
@Component
public class InMemoryAirfoilRegistry implements AirfoilRegistry {

    private final ConcurrentHashMap<String, Airfoil> polars = new ConcurrentHashMap<>();

    @Override
    public Airfoil register(Airfoil airfoil) {
        polars.put(airfoil.name(), airfoil);
        return airfoil;
    }

    @Override
    public Optional<Airfoil> find(String name) {
        return Optional.ofNullable(polars.get(name));
    }

    @Override
    public boolean remove(String name) {
        return polars.remove(name) != null;
    }

    @Override
    public Collection<Airfoil> all() {
        return polars.values();
    }
}
