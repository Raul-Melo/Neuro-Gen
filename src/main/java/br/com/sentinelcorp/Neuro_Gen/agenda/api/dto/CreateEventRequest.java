package br.com.sentinelcorp.Neuro_Gen.agenda.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateEventRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        String rrule,
        String type,
        String categoryId,
        boolean sharedWithSupport,
        List<ReminderRequest> reminders) {
}
