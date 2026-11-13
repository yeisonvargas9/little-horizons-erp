package com.littlehorizons.erp.model;

import java.util.ArrayList;
import java.util.List;

/** Domain entity: a professor teaching a subject in a section for an academic year, with its weekly schedule. */
public class Assignment {

    private int id;
    private int professorId;
    private int subjectId;
    private String subjectCode;
    private String subjectName;
    private String gradeName;
    private int academicYear;
    private String section;
    private List<ScheduleSlot> slots = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProfessorId() { return professorId; }
    public void setProfessorId(int professorId) { this.professorId = professorId; }
    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getGradeName() { return gradeName; }
    public void setGradeName(String gradeName) { this.gradeName = gradeName; }
    public int getAcademicYear() { return academicYear; }
    public void setAcademicYear(int academicYear) { this.academicYear = academicYear; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public List<ScheduleSlot> getSlots() { return slots; }
    public void setSlots(List<ScheduleSlot> slots) { this.slots = slots; }
}
