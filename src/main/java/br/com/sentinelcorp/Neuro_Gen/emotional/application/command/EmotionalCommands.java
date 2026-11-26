package br.com.sentinelcorp.Neuro_Gen.emotional.application.command;

/** Commands imutáveis do módulo Emotional. */
public final class EmotionalCommands {

    private EmotionalCommands() {
    }

    public record SubmitMoodCheckInCommand(String userId, int moodScore, int energyLevel, String notes) {
    }

    public record WriteJournalCommand(String userId, String content, String relatedCheckInId) {
    }

    public record EditJournalCommand(String userId, String entryId, String content) {
    }

    public record DeleteCheckInCommand(String userId, String checkInId) {
    }

    public record DeleteJournalCommand(String userId, String entryId) {
    }
}
