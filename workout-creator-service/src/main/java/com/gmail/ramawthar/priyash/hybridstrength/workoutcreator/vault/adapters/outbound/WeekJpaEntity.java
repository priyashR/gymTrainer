package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping for the {@code weeks} table.
 */
@Entity
@Table(name = "weeks")
public class WeekJpaEntity {

    @Id
    private UUID id;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private ProgramJpaEntity program;

    @OneToMany(mappedBy = "week", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    private List<DayJpaEntity> days = new ArrayList<>();

    public WeekJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getWeekNumber() { return weekNumber; }
    public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }
    public ProgramJpaEntity getProgram() { return program; }
    public void setProgram(ProgramJpaEntity program) { this.program = program; }
    public List<DayJpaEntity> getDays() { return days; }
    public void setDays(List<DayJpaEntity> days) { this.days = days; }
}
