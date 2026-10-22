package br.com.sentinelcorp.Neuro_Gen.agenda.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @Pattern(regexp = "LOW|MEDIUM|HIGH|CRITICAL") String priority,
        LocalDate dueDate,
        String categoryId,
        boolean sharedWithSupport,
        List<ReminderRequest> reminders) {
}
