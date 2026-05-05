package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.Modality;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping for the {@code days} table.
 */
@Entity
@Table(name = "days")
public class DayJpaEntity {

    @Id
    private UUID id;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "day_label", nullable = false)
    private String label;

    @Column(name = "focus_area", nullable = false)
    private String focusArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "modality", nullable = false)
    private Modality modality;

    @Column(name = "methodology_source")
    private String methodologySource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = false)
    private WeekJpaEntity week;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<SectionJpaEntity> sections = new ArrayList<>();

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<WarmCoolEntryJpaEntity> warmCoolEntries = new ArrayList<>();

    public DayJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getFocusArea() { return focusArea; }
    public void setFocusArea(String focusArea) { this.focusArea = focusArea; }
    public Modality getModality() { return modality; }
    public void setModality(Modality modality) { this.modality = modality; }
    public String getMethodologySource() { return methodologySource; }
    public void setMethodologySource(String methodologySource) { this.methodologySource = methodologySource; }
    public WeekJpaEntity getWeek() { return week; }
    public void setWeek(WeekJpaEntity week) { this.week = week; }
    public List<SectionJpaEntity> getSections() { return sections; }
    public void setSections(List<SectionJpaEntity> sections) { this.sections = sections; }
    public List<WarmCoolEntryJpaEntity> getWarmCoolEntries() { return warmCoolEntries; }
    public void setWarmCoolEntries(List<WarmCoolEntryJpaEntity> warmCoolEntries) { this.warmCoolEntries = warmCoolEntries; }
}
