package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.outbound;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ProgramJpaEntity}.
 * Public so the upload adapter in a sibling package can reuse it without duplicating
 * the entity infrastructure.
 */
public interface ProgramSpringDataRepository extends JpaRepository<ProgramJpaEntity, UUID> {

    Page<ProgramJpaEntity> findAllByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId, Pageable pageable);

    Optional<ProgramJpaEntity> findByIdAndOwnerUserId(UUID id, String ownerUserId);

    void deleteByIdAndOwnerUserId(UUID id, String ownerUserId);

    boolean existsByIdAndOwnerUserId(UUID id, String ownerUserId);

    /**
     * Search programs by keyword (name/goal), focus area, and modality with relevance ordering.
     * Name matches rank higher than goal-only matches; ties broken by createdAt descending.
     *
     * <p>All filter parameters are nullable — when null they are excluded from the WHERE clause.
     */
    @Query(value = """
            SELECT p.* FROM (
                SELECT DISTINCT p.id, p.content_source, p.created_at, p.duration_weeks,
                       p.equipment_profile, p.goal, p.name, p.owner_user_id, p.updated_at,
                       CASE WHEN :query IS NOT NULL AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                            THEN 0 ELSE 1 END AS relevance
                FROM programs p
                LEFT JOIN weeks w ON p.id = w.program_id
                LEFT JOIN days d ON w.id = d.week_id
                WHERE p.owner_user_id = :ownerUserId
                AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                     OR LOWER(p.goal) LIKE LOWER(CONCAT('%', :query, '%')))
                AND (:focusArea IS NULL OR LOWER(d.focus_area) = LOWER(CAST(:focusArea AS TEXT)))
                AND (:modality IS NULL OR LOWER(d.modality) = LOWER(CAST(:modality AS TEXT)))
            ) p
            ORDER BY p.relevance, p.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM programs p
            LEFT JOIN weeks w ON p.id = w.program_id
            LEFT JOIN days d ON w.id = d.week_id
            WHERE p.owner_user_id = :ownerUserId
            AND (:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(p.goal) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:focusArea IS NULL OR LOWER(d.focus_area) = LOWER(CAST(:focusArea AS TEXT)))
            AND (:modality IS NULL OR LOWER(d.modality) = LOWER(CAST(:modality AS TEXT)))
            """,
            nativeQuery = true)
    Page<ProgramJpaEntity> searchPrograms(
            @Param("ownerUserId") String ownerUserId,
            @Param("query") String query,
            @Param("focusArea") String focusArea,
            @Param("modality") String modality,
            Pageable pageable);
}
