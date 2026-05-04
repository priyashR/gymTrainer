package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA entity for warm-up and cool-down entries within a Day.
 * The {@code entry_type} column distinguishes WARM_UP from COOL_DOWN rows.
 */
@Entity
@Table(name = "warm_cool_entries")
public class WarmCoolEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "entry_type", nullable = false)
    private String entryType; // "WARM_UP" or "COOL_DOWN"

    @Column(name = "movement", nullable = false)
    private String movement;

    @Column(name = "instruction", nullable = false)
    private String instruction;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    private DayJpaEntity day;

    public WarmCoolEntryJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public String getMovement() { return movement; }
    public void setMovement(String movement) { this.movement = movement; }
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public DayJpaEntity getDay() { return day; }
    public void setDay(DayJpaEntity day) { this.day = day; }
}
