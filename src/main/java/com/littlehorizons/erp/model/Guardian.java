package com.littlehorizons.erp.model;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Domain entity: a guardian (parent or legal tutor) of one or more students (table "acudientes"). */
public class Guardian {

    private int id;
    private DocumentType documentType = DocumentType.CITIZENSHIP_CARD;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String firstSurname;
    private String secondSurname;
    private String relationship;
    private String primaryPhone;
    private String secondaryPhone;
    private String email;
    private String address;
    private String occupation;
    private boolean financiallyResponsible = true;

    public String getFullName() {
        return Stream.of(firstName, middleName, firstSurname, secondSurname)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getFirstSurname() { return firstSurname; }
    public void setFirstSurname(String firstSurname) { this.firstSurname = firstSurname; }
    public String getSecondSurname() { return secondSurname; }
    public void setSecondSurname(String secondSurname) { this.secondSurname = secondSurname; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getPrimaryPhone() { return primaryPhone; }
    public void setPrimaryPhone(String primaryPhone) { this.primaryPhone = primaryPhone; }
    public String getSecondaryPhone() { return secondaryPhone; }
    public void setSecondaryPhone(String secondaryPhone) { this.secondaryPhone = secondaryPhone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public boolean isFinanciallyResponsible() { return financiallyResponsible; }
    public void setFinanciallyResponsible(boolean financiallyResponsible) { this.financiallyResponsible = financiallyResponsible; }
}
