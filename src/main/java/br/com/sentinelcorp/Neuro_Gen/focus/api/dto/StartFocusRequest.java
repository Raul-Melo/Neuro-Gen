package br.com.sentinelcorp.Neuro_Gen.focus.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StartFocusRequest(
        String taskId,
        @Min(1) @Max(480) int durationMinutes,
        @Min(1) @Max(60) int breakMinutes) {
}
