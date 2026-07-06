package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto;

import com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.domain.DaySummary;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Response DTO for the GET /api/v1/vault/programs/{id}/days endpoint.
 * Groups days by week number for easy consumption by the frontend day picker.
 */
public record ProgramDaysResponse(List<WeekDays> weeks) {

    /**
     * Factory method that groups a flat list of day summaries by week number,
     * sorts weeks in ascending order, and maps each day to a DayEntry.
     */
    public static ProgramDaysResponse from(List<DaySummary> days) {
        Map<Integer, List<DaySummary>> grouped = days.stream()
                .collect(Collectors.groupingBy(DaySummary::weekNumber));

        List<WeekDays> weeks = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new WeekDays(e.getKey(), e.getValue().stream()
                        .map(d -> new DayEntry(d.dayNumber(), d.label(), d.focusArea()))
                        .toList()))
                .toList();

        return new ProgramDaysResponse(weeks);
    }

    public record WeekDays(int weekNumber, List<DayEntry> days) {}

    public record DayEntry(int dayNumber, String label, String focusArea) {}
}
