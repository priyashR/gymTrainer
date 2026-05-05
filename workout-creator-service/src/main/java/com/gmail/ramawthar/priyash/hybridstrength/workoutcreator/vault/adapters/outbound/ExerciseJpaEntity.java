package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ModalityType;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA entity mapping for the {@code exercises} table.
 * A single movement within a Section/Block.
 */
@Entity
@Table(name = "exercises")
public class ExerciseJpaEntity {

    @Id
    private UUID id;

    @Column(name = "exercise_name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "modality_type")
    private ModalityType modalityType;

    @Column(name = "prescribed_sets", nullable = false)
    private int sets;

    @Column(name = "prescribed_reps", nullable = false)
    private String reps;

    @Column(name = "prescribed_weight")
    private String weight;

    @Column(name = "rest_interval_seconds")
    private Integer restSeconds;

    @Column(name = "notes")
    private String notes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private SectionJpaEntity section;

    public ExerciseJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ModalityType getModalityType() { return modalityType; }
    public void setModalityType(ModalityType modalityType) { this.modalityType = modalityType; }
    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }
    public String getReps() { return reps; }
    public void setReps(String reps) { this.reps = reps; }
    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
    public Integer getRestSeconds() { return restSeconds; }
    public void setRestSeconds(Integer restSeconds) { this.restSeconds = restSeconds; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public SectionJpaEntity getSection() { return section; }
    public void setSection(SectionJpaEntity section) { this.section = section; }
}
