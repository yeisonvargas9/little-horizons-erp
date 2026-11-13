package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Domain entity: a student of the school (table "estudiantes"). */
public class Student {

    private int id;
    private DocumentType documentType;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String firstSurname;
    private String secondSurname;
    private LocalDate birthDate;
    private Gender gender;
    private String address;
    private String phone;
    private String email;
    private String bloodType;
    private String allergies;
    private String medicalConditions;
    private LocalDate admissionDate;
    private StudentStatus status = StudentStatus.ACTIVE;

    public String getFullName() {
        return Stream.of(firstName, middleName, firstSurname, secondSurname)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    public int getAge() {
        return birthDate == null ? 0 : Period.between(birthDate, LocalDate.now()).getYears();
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
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public String getMedicalConditions() { return medicalConditions; }
    public void setMedicalConditions(String medicalConditions) { this.medicalConditions = medicalConditions; }
    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }
    public StudentStatus getStatus() { return status; }
    public void setStatus(StudentStatus status) { this.status = status; }
}
