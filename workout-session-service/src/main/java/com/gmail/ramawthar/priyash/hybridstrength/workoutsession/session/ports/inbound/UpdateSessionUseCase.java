package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.Session;

import java.util.UUID;

/**
 * Inbound port for modifying session state during an active workout.
 * Covers exercise completion and section navigation.
 */
public interface UpdateSessionUseCase {

    /**
     * Marks an exercise as completed within a session.
     * Idempotent — completing an already-completed exercise is a no-op.
     *
     * @param sessionId     the session to update
     * @param userId        the authenticated user's ID (for ownership check)
     * @param sectionIndex  the section containing the exercise
     * @param exerciseIndex the exercise within the section
     * @return the updated session state
     */
    Session completeExercise(UUID sessionId, String userId, int sectionIndex, int exerciseIndex);

    /**
     * Navigates to a different section within the session.
     *
     * @param sessionId          the session to update
     * @param userId             the authenticated user's ID (for ownership check)
     * @param targetSectionIndex the section to navigate to
     * @return the updated session state
     */
    Session advanceSection(UUID sessionId, String userId, int targetSectionIndex);
}
