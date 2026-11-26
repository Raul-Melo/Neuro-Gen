package br.com.sentinelcorp.Neuro_Gen.emotional.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateJournalRequest(
        @NotBlank @Size(max = 10000) String content) {
}
