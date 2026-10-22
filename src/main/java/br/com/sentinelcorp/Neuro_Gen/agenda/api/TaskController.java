package br.com.sentinelcorp.Neuro_Gen.agenda.api;

import br.com.sentinelcorp.Neuro_Gen.agenda.api.dto.AddSubtaskRequest;
import br.com.sentinelcorp.Neuro_Gen.agenda.api.dto.CreateTaskRequest;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.TaskUseCase;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.CreateTaskCommand;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.ReminderInput;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.result.TaskResult;
import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tarefas, subtarefas e ciclo de vida (EPIC-02 / EPIC-06). */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Agenda — Tarefas", description = "Tarefas flexíveis, subtarefas e progresso")
public class TaskController {

    private final TaskUseCase tasks;

    public TaskController(TaskUseCase tasks) {
        this.tasks = tasks;
    }

    @PostMapping
    @Operation(summary = "Cria uma tarefa")
    public ResponseEntity<TaskResult> create(@Valid @RequestBody CreateTaskRequest req) {
        List<ReminderInput> reminders = req.reminders() == null ? List.of()
                : req.reminders().stream().map(r -> r.toInput()).toList();
        TaskResult result = tasks.create(new CreateTaskCommand(
                CurrentUser.requireId().asString(), req.title(), req.description(), req.priority(),
                req.dueDate(), req.categoryId(), req.sharedWithSupport(), reminders));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @Operation(summary = "Lista tarefas do usuário (opcionalmente filtradas por status)")
    public List<TaskResult> list(@RequestParam(required = false) String status) {
        return tasks.list(CurrentUser.requireId().asString(), status);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Conclui uma tarefa")
    public TaskResult complete(@PathVariable String id) {
        return tasks.complete(id, CurrentUser.requireId().asString());
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Arquiva uma tarefa")
    public TaskResult archive(@PathVariable String id) {
        return tasks.archive(id, CurrentUser.requireId().asString());
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reabre uma tarefa")
    public TaskResult reopen(@PathVariable String id) {
        return tasks.reopen(id, CurrentUser.requireId().asString());
    }

    @PostMapping("/{id}/subtasks")
    @Operation(summary = "Adiciona uma subtarefa")
    public TaskResult addSubtask(@PathVariable String id, @Valid @RequestBody AddSubtaskRequest req) {
        return tasks.addSubtask(id, CurrentUser.requireId().asString(), req.title());
    }

    @PostMapping("/{id}/subtasks/{subtaskId}/complete")
    @Operation(summary = "Conclui uma subtarefa")
    public TaskResult completeSubtask(@PathVariable String id, @PathVariable String subtaskId) {
        return tasks.completeSubtask(id, CurrentUser.requireId().asString(), subtaskId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma tarefa")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        tasks.delete(id, CurrentUser.requireId().asString());
        return ResponseEntity.noContent().build();
    }
}
