package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Domain entity: a teacher of the school (table "profesores"). */
public class Professor {

    private int id;
    private DocumentType documentType = DocumentType.CITIZENSHIP_CARD;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String firstSurname;
    private String secondSurname;
    private String email;
    private String phone;
    private String specialization;
    private LocalDate hireDate;
    private ProfessorStatus status = ProfessorStatus.ACTIVE;

    public String getFullName() {
        return Stream.of(firstName, middleName, firstSurname, secondSurname)
                .filter(Objects::nonNull).collect(Collectors.joining(" "));
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
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public ProfessorStatus getStatus() { return status; }
    public void setStatus(ProfessorStatus status) { this.status = status; }
}
