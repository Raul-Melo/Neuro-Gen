package br.com.sentinelcorp.Neuro_Gen.agenda.application;

import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.CreateTaskCommand;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.ReminderInput;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.result.TaskResult;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception.TaskNotFoundException;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.CategoryId;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Priority;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Reminder;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Task;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.TaskId;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.TaskStatus;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.port.TaskRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Use case de tarefas, subtarefas e ciclo de vida (RF-02, RF-06). */
@Service
public class TaskUseCase {

    private final TaskRepository tasks;

    public TaskUseCase(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Transactional
    public TaskResult create(CreateTaskCommand cmd) {
        Task task = Task.create(
                UserId.of(cmd.ownerId()), cmd.title(), cmd.description(),
                cmd.priority() == null ? null : Priority.valueOf(cmd.priority()),
                cmd.dueDate(), CategoryId.of(cmd.categoryId()), cmd.sharedWithSupport(),
                toReminders(cmd.reminders()));
        return TaskResult.from(tasks.save(task));
    }

    @Transactional
    public TaskResult complete(String taskId, String ownerId) {
        Task task = load(taskId, ownerId);
        task.complete();
        return TaskResult.from(tasks.save(task));
    }

    @Transactional
    public TaskResult archive(String taskId, String ownerId) {
        Task task = load(taskId, ownerId);
        task.archive();
        return TaskResult.from(tasks.save(task));
    }

    @Transactional
    public TaskResult reopen(String taskId, String ownerId) {
        Task task = load(taskId, ownerId);
        task.reopen();
        return TaskResult.from(tasks.save(task));
    }

    @Transactional
    public TaskResult addSubtask(String taskId, String ownerId, String title) {
        Task task = load(taskId, ownerId);
        task.addSubtask(title);
        return TaskResult.from(tasks.save(task));
    }

    @Transactional
    public TaskResult completeSubtask(String taskId, String ownerId, String subtaskId) {
        Task task = load(taskId, ownerId);
        task.completeSubtask(UUID.fromString(subtaskId));
        return TaskResult.from(tasks.save(task));
    }

    @Transactional
    public void delete(String taskId, String ownerId) {
        load(taskId, ownerId);
        tasks.delete(TaskId.of(taskId));
    }

    @Transactional(readOnly = true)
    public List<TaskResult> list(String ownerId, String status) {
        UserId owner = UserId.of(ownerId);
        List<Task> result = status == null
                ? tasks.findByOwner(owner)
                : tasks.findByOwnerAndStatus(owner, parseStatus(status));
        return result.stream().map(TaskResult::from).toList();
    }

    private Task load(String taskId, String ownerId) {
        Task task = tasks.findById(TaskId.of(taskId))
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        if (!task.ownerId().equals(UserId.of(ownerId))) {
            throw new ForbiddenAccessException("Tarefa", "não pertence ao usuário");
        }
        return task;
    }

    private static TaskStatus parseStatus(String status) {
        try {
            return TaskStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(Map.of("status",
                    "Status inválido. Valores aceitos: " + Arrays.toString(TaskStatus.values())));
        }
    }

    private static List<Reminder> toReminders(List<ReminderInput> inputs) {
        if (inputs == null) {
            return List.of();
        }
        return inputs.stream()
                .map(r -> Reminder.create(r.minutesBefore(), r.persistent(),
                        r.repeatIntervalMinutes(), r.maxRepetitions()))
                .toList();
    }
}
