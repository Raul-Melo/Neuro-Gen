package br.com.sentinelcorp.Neuro_Gen.agenda.application.result;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Task;

import java.util.List;

public record TaskResult(
        String id,
        String title,
        String description,
        String status,
        String priority,
        String dueDate,
        int completionPercentage,
        boolean sharedWithSupport,
        List<SubtaskResult> subtasks,
        List<ReminderResult> reminders) {

    public static TaskResult from(Task t) {
        return new TaskResult(
                t.id().asString(),
                t.title(),
                t.description(),
                t.status().name(),
                t.priority().name(),
                t.dueDate() == null ? null : t.dueDate().toString(),
                t.completionPercentage(),
                t.sharedWithSupport(),
                t.subtasks().stream().map(SubtaskResult::from).toList(),
                t.reminders().stream().map(ReminderResult::from).toList());
    }
}
