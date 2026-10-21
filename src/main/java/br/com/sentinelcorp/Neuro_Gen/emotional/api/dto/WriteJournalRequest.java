package br.com.sentinelcorp.Neuro_Gen.emotional.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WriteJournalRequest(
        @NotBlank @Size(max = 10_000) String content,
        String relatedCheckInId) {
}
