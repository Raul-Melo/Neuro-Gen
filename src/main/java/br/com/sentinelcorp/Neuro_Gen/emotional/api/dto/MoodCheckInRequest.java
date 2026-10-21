package br.com.sentinelcorp.Neuro_Gen.emotional.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record MoodCheckInRequest(
        @Min(1) @Max(5) int moodScore,
        @Min(1) @Max(5) int energyLevel,
        @Size(max = 500) String notes) {
}
