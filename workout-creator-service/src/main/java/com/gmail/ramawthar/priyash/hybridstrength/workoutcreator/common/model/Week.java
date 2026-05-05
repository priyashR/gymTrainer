package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.model;

import java.util.List;
import java.util.Objects;

/**
 * A single training week within a Program.
 * Pure domain object — no framework dependencies.
 */
public final class Week {

    private final int weekNumber;
    private final List<Day> days;

    public Week(int weekNumber, List<Day> days) {
        this.weekNumber = weekNumber;
        this.days = List.copyOf(days);
    }

    public int getWeekNumber() { return weekNumber; }
    public List<Day> getDays() { return days; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Week w)) return false;
        return weekNumber == w.weekNumber && Objects.equals(days, w.days);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekNumber, days);
    }
}
