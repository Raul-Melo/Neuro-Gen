package br.com.sentinelcorp.Neuro_Gen.agenda.application;

import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.CreateTaskCommand;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.result.TaskResult;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception.InvalidTaskTransitionException;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception.TaskNotFoundException;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Priority;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Task;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.TaskStatus;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.port.TaskRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testes do caso de uso de Tarefas com o repositório (porta de saída) simulado. */
@ExtendWith(MockitoExtension.class)
class TaskUseCaseTest {

    @Mock TaskRepository tasks;

    TaskUseCase useCase;
    UserId owner;

    @BeforeEach
    void setUp() {
        useCase = new TaskUseCase(tasks);
        owner = UserId.generate();
    }

    private Task existingTask() {
        Task task = Task.create(owner, "Estudar", null, Priority.HIGH, null, null, false, List.of());
        when(tasks.findById(task.id())).thenReturn(Optional.of(task));
        return task;
    }

    // ---- Caracterização (comportamento que já existia) ----

    @Test
    void createPersisteATarefaComoPendente() {
        when(tasks.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TaskResult result = useCase.create(new CreateTaskCommand(
                owner.asString(), "Estudar", null, "HIGH", null, null, false, List.of()));
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.title()).isEqualTo("Estudar");
    }

    @Test
    void completePersisteETornaATarefaConcluida() {
        Task task = existingTask();
        when(tasks.save(any())).thenAnswer(inv -> inv.getArgument(0));
        TaskResult result = useCase.complete(task.id().asString(), owner.asString());
        assertThat(result.status()).isEqualTo("DONE");
        verify(tasks).save(task);
    }

    @Test
    void naoPermiteAlterarTarefaDeOutroUsuario() {
        Task task = existingTask();
        String outroUsuario = UserId.generate().asString();
        assertThatThrownBy(() -> useCase.complete(task.id().asString(), outroUsuario))
                .isInstanceOf(ForbiddenAccessException.class);
        verify(tasks, never()).save(any());
    }

    @Test
    void tarefaInexistenteLancaNotFound() {
        String id = java.util.UUID.randomUUID().toString();
        when(tasks.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.complete(id, owner.asString()))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void transicaoInvalidaNaoPersiste() {
        Task task = existingTask();
        task.archive();
        assertThatThrownBy(() -> useCase.archive(task.id().asString(), owner.asString()))
                .isInstanceOf(InvalidTaskTransitionException.class);
        verify(tasks, never()).save(any());
    }

    @Test
    void idMalformadoEhRejeitadoComoErroDeValidacao() {
        assertThatThrownBy(() -> useCase.complete("nao-e-um-uuid", owner.asString()))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("taskId"));
        verify(tasks, never()).save(any());
    }

    // ---- Novos comportamentos (antes resultavam em HTTP 500) ----

    @Test
    void listComStatusInvalidoLancaValidationException() {
        assertThatThrownBy(() -> useCase.list(owner.asString(), "QUALQUER_COISA"))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("status"));
    }

    @Test
    void listAceitaStatusEmMinusculas() {
        when(tasks.findByOwnerAndStatus(owner, TaskStatus.DONE)).thenReturn(List.of());
        assertThat(useCase.list(owner.asString(), "done")).isEmpty();
        verify(tasks).findByOwnerAndStatus(owner, TaskStatus.DONE);
    }

    @Test
    void listSemStatusRetornaTodasAsTarefasDoUsuario() {
        when(tasks.findByOwner(owner)).thenReturn(List.of());
        assertThat(useCase.list(owner.asString(), null)).isEmpty();
        verify(tasks).findByOwner(owner);
    }
}
