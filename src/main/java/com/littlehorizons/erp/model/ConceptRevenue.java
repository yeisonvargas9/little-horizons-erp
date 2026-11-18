package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** Money collected for one payment concept in a date range. */
public record ConceptRevenue(String conceptName, long payments, BigDecimal total) {
}
