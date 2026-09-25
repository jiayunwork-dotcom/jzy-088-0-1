package com.example.bem.airfoil;

import com.example.bem.domain.Airfoil;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of named airfoil polars. A polar is registered once under
 * a name and can then be referenced by many analyses; this is only an
 * organisational data store and contains no aerodynamic logic.
 */
public interface AirfoilRegistry {

    /** Add or replace a named polar. */
    Airfoil register(Airfoil airfoil);

    Optional<Airfoil> find(String name);

    /** Remove a polar; returns false if no such name was registered. */
    boolean remove(String name);

    Collection<Airfoil> all();
}
