package br.com.sentinelcorp.Neuro_Gen.agenda.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddSubtaskRequest(@NotBlank @Size(max = 200) String title) {
}
