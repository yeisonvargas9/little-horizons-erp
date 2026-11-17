package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** An item the school charges for (enrollment, monthly tuition, uniform...). Table "conceptos_pago". */
public record PaymentConcept(int id, String code, String name, String description, BigDecimal baseAmount,
                             boolean recurring, boolean appliesVat, boolean active) {

    public PaymentConcept withBaseAmount(BigDecimal newAmount) {
        return new PaymentConcept(id, code, name, description, newAmount, recurring, appliesVat, active);
    }
}
