package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception.InvalidTaskTransitionException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.AggregateRoot;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Aggregate Root para tarefas sem horário fixo. Calcula o progresso a partir das subtarefas. */
public class Task extends AggregateRoot {

    static final int TITLE_MAX_LENGTH = 200;

    private static final Set<TaskStatus> CAN_COMPLETE = EnumSet.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
    private static final Set<TaskStatus> CAN_ARCHIVE =
            EnumSet.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.DONE);
    private static final Set<TaskStatus> CAN_REOPEN = EnumSet.of(TaskStatus.DONE, TaskStatus.ARCHIVED);

    private final TaskId id;
    private final UserId ownerId;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;             // nullable
    private final List<Subtask> subtasks;
    private final List<Reminder> reminders;
    private CategoryId categoryId;          // nullable
    private boolean sharedWithSupport;
    private AuditMetadata audit;

    private Task(TaskId id, UserId ownerId, String title, String description, TaskStatus status,
                 Priority priority, LocalDate dueDate, List<Subtask> subtasks, List<Reminder> reminders,
                 CategoryId categoryId, boolean sharedWithSupport, AuditMetadata audit) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.subtasks = new ArrayList<>(subtasks);
        this.reminders = new ArrayList<>(reminders);
        this.categoryId = categoryId;
        this.sharedWithSupport = sharedWithSupport;
        this.audit = audit;
    }

    public static Task create(UserId ownerId, String title, String description, Priority priority,
                              LocalDate dueDate, CategoryId categoryId, boolean sharedWithSupport,
                              List<Reminder> reminders) {
        return new Task(TaskId.generate(), ownerId, validTitle(title), description, TaskStatus.PENDING,
                priority == null ? Priority.MEDIUM : priority, dueDate, List.of(), reminders,
                categoryId, sharedWithSupport, AuditMetadata.createNow(ownerId.asString()));
    }

    public static Task reconstitute(TaskId id, UserId ownerId, String title, String description,
                                    TaskStatus status, Priority priority, LocalDate dueDate,
                                    List<Subtask> subtasks, List<Reminder> reminders, CategoryId categoryId,
                                    boolean sharedWithSupport, AuditMetadata audit) {
        return new Task(id, ownerId, title, description, status, priority, dueDate, subtasks, reminders,
                categoryId, sharedWithSupport, audit);
    }

    public void complete() {
        transition(CAN_COMPLETE, TaskStatus.DONE, "concluir");
    }

    public void archive() {
        transition(CAN_ARCHIVE, TaskStatus.ARCHIVED, "arquivar");
    }

    public void reopen() {
        transition(CAN_REOPEN, TaskStatus.PENDING, "reabrir");
    }

    private void transition(Set<TaskStatus> allowedFrom, TaskStatus target, String action) {
        if (!allowedFrom.contains(status)) {
            throw new InvalidTaskTransitionException(status, action);
        }
        this.status = target;
        this.audit = audit.touch(ownerId.asString());
    }

    private static String validTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException(Map.of("title", "Título é obrigatório"));
        }
        String trimmed = title.strip();
        if (trimmed.length() > TITLE_MAX_LENGTH) {
            throw new ValidationException(Map.of("title", "Título deve ter no máximo " + TITLE_MAX_LENGTH + " caracteres"));
        }
        return trimmed;
    }

    public Subtask addSubtask(String title) {
        Subtask subtask = Subtask.create(title, subtasks.size());
        subtasks.add(subtask);
        this.audit = audit.touch(ownerId.asString());
        return subtask;
    }

    public void completeSubtask(UUID subtaskId) {
        Subtask subtask = subtasks.stream().filter(s -> s.id().equals(subtaskId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Subtarefa", subtaskId.toString()));
        subtask.complete();
        this.audit = audit.touch(ownerId.asString());
    }

    public int completionPercentage() {
        if (subtasks.isEmpty()) {
            return status == TaskStatus.DONE ? 100 : 0;
        }
        long done = subtasks.stream().filter(Subtask::done).count();
        return (int) Math.round((done * 100.0) / subtasks.size());
    }

    public TaskId id() { return id; }
    public UserId ownerId() { return ownerId; }
    public String title() { return title; }
    public String description() { return description; }
    public TaskStatus status() { return status; }
    public Priority priority() { return priority; }
    public LocalDate dueDate() { return dueDate; }
    public List<Subtask> subtasks() {
        return subtasks.stream().sorted(Comparator.comparingInt(Subtask::position)).toList();
    }
    public List<Reminder> reminders() { return List.copyOf(reminders); }
    public CategoryId categoryId() { return categoryId; }
    public boolean sharedWithSupport() { return sharedWithSupport; }
    public AuditMetadata audit() { return audit; }
}
