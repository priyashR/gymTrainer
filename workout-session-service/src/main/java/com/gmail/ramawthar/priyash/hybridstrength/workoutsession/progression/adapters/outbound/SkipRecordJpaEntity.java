package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.domain.SkipRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping for the {@code skip_records} table.
 */
@Entity
@Table(name = "skip_records")
class SkipRecordJpaEntity {

    @Id
    private UUID id;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "skipped_at", nullable = false)
    private Instant skippedAt;

    protected SkipRecordJpaEntity() {
        // JPA requires a no-arg constructor
    }

    static SkipRecordJpaEntity fromDomain(SkipRecord record, UUID enrollmentId) {
        SkipRecordJpaEntity entity = new SkipRecordJpaEntity();
        entity.id = UUID.randomUUID();
        entity.enrollmentId = enrollmentId;
        entity.weekNumber = record.getWeekNumber();
        entity.dayNumber = record.getDayNumber();
        entity.skippedAt = record.getSkippedAt();
        return entity;
    }

    SkipRecord toDomain() {
        return new SkipRecord(weekNumber, dayNumber, skippedAt);
    }

    UUID getId() {
        return id;
    }

    UUID getEnrollmentId() {
        return enrollmentId;
    }
}
