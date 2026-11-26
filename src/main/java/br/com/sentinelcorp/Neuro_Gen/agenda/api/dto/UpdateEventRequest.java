package br.com.sentinelcorp.Neuro_Gen.agenda.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEventRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @Pattern(regexp = "APPOINTMENT|PERSONAL|ROUTINE") String type,
        String categoryId,
        boolean sharedWithSupport) {
}
