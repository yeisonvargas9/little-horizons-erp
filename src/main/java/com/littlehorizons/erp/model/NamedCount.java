package com.littlehorizons.erp.model;

/** A label with a count (members per club, resources per status...). */
public record NamedCount(String name, long count) {
}
