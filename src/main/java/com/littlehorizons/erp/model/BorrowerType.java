package com.littlehorizons.erp.model;

/** Who can borrow a resource. Students and professors are stored in separate loan tables. */
public enum BorrowerType {
    STUDENT("Estudiante"),
    PROFESSOR("Profesor");

    private final String label;

    BorrowerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
