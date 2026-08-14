package com.mediminder.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "care_circle_id")
    private CareCircle careCircle;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String dosage;

    @Column(nullable = false)
    private int stockCount;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "medication", cascade = CascadeType.ALL)
    @OrderBy("timeOfDay")
    private List<IntakeSchedule> schedules = new ArrayList<>();

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

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public int getStockCount() {
        return stockCount;
    }

    public void setStockCount(int stockCount) {
        this.stockCount = stockCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<IntakeSchedule> getSchedules() {
        return schedules;
    }
}
