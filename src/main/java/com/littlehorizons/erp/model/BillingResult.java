package com.littlehorizons.erp.model;

import java.util.List;

/** What confirming a payment produces: the saved payments (with receipt numbers) and their invoice. */
public record BillingResult(Invoice invoice, List<Payment> payments) {
}
