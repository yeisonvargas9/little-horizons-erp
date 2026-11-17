package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PaymentConcept;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ConceptDao {

    PaymentConcept save(PaymentConcept concept);

    Optional<PaymentConcept> findById(int id);

    Optional<PaymentConcept> findByCode(String code);

    List<PaymentConcept> findAll();

    boolean updateBaseAmount(int conceptId, BigDecimal newAmount);
}
