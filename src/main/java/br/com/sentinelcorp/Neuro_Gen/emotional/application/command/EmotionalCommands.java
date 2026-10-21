package br.com.sentinelcorp.Neuro_Gen.emotional.application.command;

/** Commands imutáveis do módulo Emotional. */
public final class EmotionalCommands {

    private EmotionalCommands() {
    }

    public record SubmitMoodCheckInCommand(String userId, int moodScore, int energyLevel, String notes) {
    }

    public record WriteJournalCommand(String userId, String content, String relatedCheckInId) {
    }
}
