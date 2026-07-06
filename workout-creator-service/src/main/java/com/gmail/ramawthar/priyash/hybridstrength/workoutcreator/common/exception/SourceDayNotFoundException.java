package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.exception;

/**
 * Thrown when the specified week/day combination does not exist within
 * the source program during a copied-day assignment resolution.
 * Maps to 400 Bad Request via the global exception handler.
 */
public class SourceDayNotFoundException extends RuntimeException {

    private final int weekNumber;
    private final int dayNumber;

    public SourceDayNotFoundException(int weekNumber, int dayNumber) {
        super("Day not found in source program: week " + weekNumber + ", day " + dayNumber);
        this.weekNumber = weekNumber;
        this.dayNumber = dayNumber;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public int getDayNumber() {
        return dayNumber;
    }
}
