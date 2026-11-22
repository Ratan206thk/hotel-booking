package com.rupeek.hotel.repository;

/** Identifies a pool of interchangeable rooms: one room type of one property. */
public record InventoryKey(String propertyId, String roomTypeId) {
}
