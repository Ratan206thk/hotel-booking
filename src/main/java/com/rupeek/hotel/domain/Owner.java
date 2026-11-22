package com.rupeek.hotel.domain;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An account that owns one or more properties. A standalone property is simply an owner with exactly one
 * property -- there is no separate "single property" code path.
 */
public class Owner {
    private final String id;
    private final String name;
    private final List<String> propertyIds = new CopyOnWriteArrayList<>();

    public Owner(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() { return id; }

    public String name() { return name; }

    public List<String> propertyIds() { return List.copyOf(propertyIds); }

    public void addProperty(String propertyId) { propertyIds.add(propertyId); }
}
