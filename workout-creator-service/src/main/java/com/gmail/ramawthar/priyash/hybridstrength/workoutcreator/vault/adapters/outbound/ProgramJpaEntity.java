package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model.ContentSource;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping for the {@code programs} table.
 * Shared by all content sources: AI_GENERATED, UPLOADED, and MANUAL.
 * Public so the upload adapter in a sibling package can reuse this entity infrastructure.
 */
@Entity
@Table(name = "programs")
public class ProgramJpaEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "duration_weeks", nullable = false)
    private int durationWeeks;

    @Column(name = "goal", nullable = false)
    private String goal;

    @Column(name = "equipment_profile", nullable = false)
    private String equipmentProfile; // stored as comma-separated; split on read

    @Column(name = "owner_user_id", nullable = false)
    private String ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_source", nullable = false)
    private ContentSource contentSource;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber ASC")
    private List<WeekJpaEntity> weeks = new ArrayList<>();

    public ProgramJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(int durationWeeks) { this.durationWeeks = durationWeeks; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getEquipmentProfile() { return equipmentProfile; }
    public void setEquipmentProfile(String equipmentProfile) { this.equipmentProfile = equipmentProfile; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
    public ContentSource getContentSource() { return contentSource; }
    public void setContentSource(ContentSource contentSource) { this.contentSource = contentSource; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<WeekJpaEntity> getWeeks() { return weeks; }
    public void setWeeks(List<WeekJpaEntity> weeks) { this.weeks = weeks; }
}
