package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.SectionType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping for the {@code sections} table.
 * Represents a Block within a Day (e.g., Tier 1: Compound, Metcon, Finisher).
 */
@Entity
@Table(name = "sections")
public class SectionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false)
    private SectionType sectionType;

    @Column(name = "format")
    private String format;

    @Column(name = "time_cap")
    private Integer timeCap;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    private DayJpaEntity day;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ExerciseJpaEntity> exercises = new ArrayList<>();

    public SectionJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SectionType getSectionType() { return sectionType; }
    public void setSectionType(SectionType sectionType) { this.sectionType = sectionType; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Integer getTimeCap() { return timeCap; }
    public void setTimeCap(Integer timeCap) { this.timeCap = timeCap; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public DayJpaEntity getDay() { return day; }
    public void setDay(DayJpaEntity day) { this.day = day; }
    public List<ExerciseJpaEntity> getExercises() { return exercises; }
    public void setExercises(List<ExerciseJpaEntity> exercises) { this.exercises = exercises; }
}
