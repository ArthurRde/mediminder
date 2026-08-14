package com.mediminder.model;

import jakarta.persistence.*;

@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "care_circle_id", unique = true)
    private CareCircle careCircle;

    @Column(nullable = false)
    private String name;

    private Integer birthYear;

    private String note;

    public Long getId() {
        return id;
    }

    public CareCircle getCareCircle() {
        return careCircle;
    }

    public void setCareCircle(CareCircle careCircle) {
        this.careCircle = careCircle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
