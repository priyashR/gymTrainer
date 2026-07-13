package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * JPA entity mapping for the {@code day_assignments} table.
 * Each row represents a single day assignment within a manual program,
 * linking a day number to either a vault workout, an external activity,
 * or a copied day snapshot with provenance metadata.
 */
@Entity
@Table(name = "day_assignments")
public class DayAssignmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private ProgramJpaEntity program;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "assignment_type", nullable = false, length = 30)
    private String assignmentType;

    @Column(name = "workout_id")
    private UUID workoutId;

    @Column(name = "activity_type", length = 100)
    private String activityType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_data", columnDefinition = "jsonb")
    private String snapshotData;

    @Column(name = "source_program_id")
    private UUID sourceProgramId;

    @Column(name = "source_week_number")
    private Integer sourceWeekNumber;

    @Column(name = "source_day_number")
    private Integer sourceDayNumber;

    public DayAssignmentJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ProgramJpaEntity getProgram() { return program; }
    public void setProgram(ProgramJpaEntity program) { this.program = program; }
    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }
    public String getAssignmentType() { return assignmentType; }
    public void setAssignmentType(String assignmentType) { this.assignmentType = assignmentType; }
    public UUID getWorkoutId() { return workoutId; }
    public void setWorkoutId(UUID workoutId) { this.workoutId = workoutId; }
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public String getSnapshotData() { return snapshotData; }
    public void setSnapshotData(String snapshotData) { this.snapshotData = snapshotData; }
    public UUID getSourceProgramId() { return sourceProgramId; }
    public void setSourceProgramId(UUID sourceProgramId) { this.sourceProgramId = sourceProgramId; }
    public Integer getSourceWeekNumber() { return sourceWeekNumber; }
    public void setSourceWeekNumber(Integer sourceWeekNumber) { this.sourceWeekNumber = sourceWeekNumber; }
    public Integer getSourceDayNumber() { return sourceDayNumber; }
    public void setSourceDayNumber(Integer sourceDayNumber) { this.sourceDayNumber = sourceDayNumber; }
}
