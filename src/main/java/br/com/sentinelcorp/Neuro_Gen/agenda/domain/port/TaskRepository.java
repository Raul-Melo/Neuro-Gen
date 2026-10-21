package br.com.sentinelcorp.Neuro_Gen.agenda.domain.port;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Task;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.TaskId;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.TaskStatus;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findById(TaskId id);

    List<Task> findByOwnerAndStatus(UserId owner, TaskStatus status);

    List<Task> findByOwner(UserId owner);

    void delete(TaskId id);
}
