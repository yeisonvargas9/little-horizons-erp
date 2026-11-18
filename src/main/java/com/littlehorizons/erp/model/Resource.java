package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** A lendable item of the school (table "recursos"). */
public record Resource(int id, String code, String name, int categoryId, String categoryName, String description,
                       LocalDate acquisitionDate, String location, ResourceStatus status) {
}
