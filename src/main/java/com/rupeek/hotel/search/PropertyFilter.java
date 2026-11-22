package com.rupeek.hotel.search;

@FunctionalInterface
public interface PropertyFilter {
    boolean matches(PropertySearchResult candidate);
}
