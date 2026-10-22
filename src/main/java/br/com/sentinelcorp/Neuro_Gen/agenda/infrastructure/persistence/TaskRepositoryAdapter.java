package br.com.sentinelcorp.Neuro_Gen.agenda.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.CategoryId;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Priority;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Reminder;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Subtask;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Task;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.TaskId;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.TaskStatus;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.port.TaskRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class TaskRepositoryAdapter implements TaskRepository {

    private final TaskJpaRepository taskJpa;
    private final SubtaskJpaRepository subtaskJpa;
    private final ReminderJpaRepository reminderJpa;

    public TaskRepositoryAdapter(TaskJpaRepository taskJpa, SubtaskJpaRepository subtaskJpa,
                                 ReminderJpaRepository reminderJpa) {
        this.taskJpa = taskJpa;
        this.subtaskJpa = subtaskJpa;
        this.reminderJpa = reminderJpa;
    }

    @Override
    public Task save(Task task) {
        TaskEntity e = taskJpa.findById(task.id().value()).orElseGet(TaskEntity::new);
        e.setId(task.id().value());
        e.setOwnerId(task.ownerId().value());
        e.setTitle(task.title());
        e.setDescription(task.description());
        e.setStatus(task.status().name());
        e.setPriority(task.priority().name());
        e.setDueDate(task.dueDate());
        e.setCategoryId(task.categoryId() == null ? null : task.categoryId().value());
        e.setSharedWithSupport(task.sharedWithSupport());
        if (e.getCreatedAt() == null) {
            e.setCreatedAt(task.audit().createdAt());
        }
        e.setUpdatedAt(task.audit().updatedAt());
        e.setArchivedAt(task.status() == TaskStatus.ARCHIVED ? Instant.now() : null);
        taskJpa.save(e);

        subtaskJpa.deleteByTaskId(task.id().value());
        for (Subtask s : task.subtasks()) {
            SubtaskEntity se = new SubtaskEntity();
            se.setId(s.id());
            se.setTaskId(task.id().value());
            se.setTitle(s.title());
            se.setDone(s.done());
            se.setPosition(s.position());
            se.setCompletedAt(s.completedAt());
            se.setCreatedAt(Instant.now());
            subtaskJpa.save(se);
        }

        reminderJpa.deleteByTaskId(task.id().value());
        for (Reminder r : task.reminders()) {
            reminderJpa.save(ReminderMapper.toEntity(r, null, task.id().value()));
        }
        return task;
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        return taskJpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Task> findByOwnerAndStatus(UserId owner, TaskStatus status) {
        return taskJpa.findByOwnerIdAndStatus(owner.value(), status.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Task> findByOwner(UserId owner) {
        return taskJpa.findByOwnerId(owner.value()).stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(TaskId id) {
        subtaskJpa.deleteByTaskId(id.value());
        reminderJpa.deleteByTaskId(id.value());
        taskJpa.deleteById(id.value());
    }

    private Task toDomain(TaskEntity e) {
        List<Subtask> subtasks = subtaskJpa.findByTaskId(e.getId()).stream()
                .map(s -> new Subtask(s.getId(), s.getTitle(), s.isDone(), s.getPosition(), s.getCompletedAt()))
                .toList();
        List<Reminder> reminders = reminderJpa.findByTaskId(e.getId()).stream()
                .map(ReminderMapper::toDomain).toList();
        return Task.reconstitute(
                new TaskId(e.getId()),
                new UserId(e.getOwnerId()),
                e.getTitle(),
                e.getDescription(),
                TaskStatus.valueOf(e.getStatus()),
                Priority.valueOf(e.getPriority()),
                e.getDueDate(),
                subtasks,
                reminders,
                e.getCategoryId() == null ? null : new CategoryId(e.getCategoryId()),
                e.isSharedWithSupport(),
                new AuditMetadata(e.getCreatedAt(), e.getUpdatedAt(), e.getOwnerId().toString()));
    }
}
