package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.EnrollmentStatus;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.ProgramEnrollment;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.SkipRecord;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapping for the {@code program_enrollments} table.
 * Includes a one-to-many relationship with {@link SkipRecordJpaEntity}.
 */
@Entity
@Table(name = "program_enrollments")
class EnrollmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "program_name", nullable = false, length = 500)
    private String programName;

    @Column(name = "current_week", nullable = false)
    private int currentWeek;

    @Column(name = "current_day", nullable = false)
    private int currentDay;

    @Column(name = "total_weeks", nullable = false)
    private int totalWeeks;

    @Column(name = "total_days_per_week", nullable = false)
    private int totalDaysPerWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", referencedColumnName = "id")
    private List<SkipRecordJpaEntity> skipRecords = new ArrayList<>();

    protected EnrollmentJpaEntity() {
        // JPA requires a no-arg constructor
    }

    static EnrollmentJpaEntity fromDomain(ProgramEnrollment enrollment) {
        EnrollmentJpaEntity entity = new EnrollmentJpaEntity();
        entity.id = enrollment.getId();
        entity.userId = enrollment.getUserId();
        entity.programId = enrollment.getProgramId();
        entity.programName = enrollment.getProgramName();
        entity.currentWeek = enrollment.getCurrentWeek();
        entity.currentDay = enrollment.getCurrentDay();
        entity.totalWeeks = enrollment.getTotalWeeks();
        entity.totalDaysPerWeek = enrollment.getTotalDaysPerWeek();
        entity.status = enrollment.getStatus();
        entity.enrolledAt = enrollment.getEnrolledAt();
        entity.completedAt = enrollment.getCompletedAt();
        entity.skipRecords = new ArrayList<>(enrollment.getSkips().stream()
                .map(skip -> SkipRecordJpaEntity.fromDomain(skip, enrollment.getId()))
                .toList());
        return entity;
    }

    /**
     * Updates this managed entity's fields from the domain object.
     * Merges skip records by adding new ones without replacing existing ones,
     * avoiding Hibernate orphan removal issues.
     */
    void updateFromDomain(ProgramEnrollment enrollment) {
        this.userId = enrollment.getUserId();
        this.programId = enrollment.getProgramId();
        this.programName = enrollment.getProgramName();
        this.currentWeek = enrollment.getCurrentWeek();
        this.currentDay = enrollment.getCurrentDay();
        this.totalWeeks = enrollment.getTotalWeeks();
        this.totalDaysPerWeek = enrollment.getTotalDaysPerWeek();
        this.status = enrollment.getStatus();
        this.enrolledAt = enrollment.getEnrolledAt();
        this.completedAt = enrollment.getCompletedAt();

        // Merge skip records: add any new ones from the domain that don't exist yet
        int existingCount = this.skipRecords.size();
        List<SkipRecord> domainSkips = enrollment.getSkips();
        if (domainSkips.size() > existingCount) {
            // Only add the new skip records (domain has more than what's persisted)
            for (int i = existingCount; i < domainSkips.size(); i++) {
                this.skipRecords.add(SkipRecordJpaEntity.fromDomain(domainSkips.get(i), enrollment.getId()));
            }
        }
    }

    ProgramEnrollment toDomain() {
        List<SkipRecord> domainSkips = skipRecords.stream()
                .map(SkipRecordJpaEntity::toDomain)
                .toList();

        return new ProgramEnrollment.Builder()
                .id(id)
                .userId(userId)
                .programId(programId)
                .programName(programName)
                .currentWeek(currentWeek)
                .currentDay(currentDay)
                .totalWeeks(totalWeeks)
                .totalDaysPerWeek(totalDaysPerWeek)
                .status(status)
                .enrolledAt(enrolledAt)
                .completedAt(completedAt)
                .skips(domainSkips)
                .build();
    }
}
