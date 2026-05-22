package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound;

import java.util.UUID;

/**
 * Outbound port for fetching workout/program definitions from the Workout Creator Service.
 * The implementation calls the Vault API and is wrapped with a circuit breaker.
 */
public interface WorkoutFetcher {

    /**
     * Fetches the program definition (including all workout days) from the Workout Creator Service.
     *
     * @param programId the program to fetch
     * @param jwt       the user's JWT for authorization propagation
     * @return the program definition as a JSON string (to be stored as the workout snapshot)
     */
    String fetchProgram(UUID programId, String jwt);
}
