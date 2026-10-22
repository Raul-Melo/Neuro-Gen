package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception.InvalidTaskTransitionException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskTest {

    private Task newTask() {
        return Task.create(UserId.generate(), "Estudar", null, Priority.HIGH, null, null, false, List.of());
    }

    @Test
    void completionPercentageIsZeroWhenPending() {
        assertThat(newTask().completionPercentage()).isZero();
    }

    @Test
    void completionPercentageReflectsSubtasks() {
        Task task = newTask();
        var s1 = task.addSubtask("Parte 1");
        task.addSubtask("Parte 2");
        assertThat(task.completionPercentage()).isZero();
        task.completeSubtask(s1.id());
        assertThat(task.completionPercentage()).isEqualTo(50);
    }

    @Test
    void completionPercentageIs100WhenDoneWithoutSubtasks() {
        Task task = newTask();
        task.complete();
        assertThat(task.completionPercentage()).isEqualTo(100);
        assertThat(task.status()).isEqualTo(TaskStatus.DONE);
    }

    /** Invariantes de criação: título é obrigatório e limitado ao tamanho da coluna (VARCHAR 200). */
    @Nested
    class Criacao {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejeitaTituloEmBranco(String titulo) {
            assertThatThrownBy(() -> Task.create(UserId.generate(), titulo, null, null, null, null, false, List.of()))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("title"));
        }

        @Test
        void rejeitaTituloNulo() {
            assertThatThrownBy(() -> Task.create(UserId.generate(), null, null, null, null, null, false, List.of()))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void rejeitaTituloMaiorQue200Caracteres() {
            assertThatThrownBy(() -> Task.create(UserId.generate(), "x".repeat(201), null, null, null, null, false, List.of()))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void removeEspacosDasPontasDoTitulo() {
            Task task = Task.create(UserId.generate(), "  Estudar  ", null, null, null, null, false, List.of());
            assertThat(task.title()).isEqualTo("Estudar");
        }
    }

    /**
     * Máquina de estados da tarefa:
     * PENDING/IN_PROGRESS --complete--> DONE; PENDING/IN_PROGRESS/DONE --archive--> ARCHIVED;
     * DONE/ARCHIVED --reopen--> PENDING. Qualquer outra transição é rejeitada (HTTP 409).
     */
    @Nested
    class Transicoes {

        @Test
        void naoConcluiTarefaArquivada() {
            Task task = newTask();
            task.archive();
            assertThatThrownBy(task::complete)
                    .isInstanceOf(InvalidTaskTransitionException.class)
                    .hasMessageContaining("ARCHIVED");
            assertThat(task.status()).isEqualTo(TaskStatus.ARCHIVED);
        }

        @Test
        void naoConcluiTarefaJaConcluida() {
            Task task = newTask();
            task.complete();
            assertThatThrownBy(task::complete).isInstanceOf(InvalidTaskTransitionException.class);
        }

        @Test
        void naoArquivaTarefaJaArquivada() {
            Task task = newTask();
            task.archive();
            assertThatThrownBy(task::archive).isInstanceOf(InvalidTaskTransitionException.class);
        }

        @Test
        void naoReabreTarefaPendente() {
            assertThatThrownBy(newTask()::reopen).isInstanceOf(InvalidTaskTransitionException.class);
        }

        @Test
        void reabreTarefaConcluidaVoltandoParaPendente() {
            Task task = newTask();
            task.complete();
            task.reopen();
            assertThat(task.status()).isEqualTo(TaskStatus.PENDING);
        }

        @Test
        void reabreTarefaArquivada() {
            Task task = newTask();
            task.archive();
            task.reopen();
            assertThat(task.status()).isEqualTo(TaskStatus.PENDING);
        }

        @Test
        void arquivaTarefaConcluida() {
            Task task = newTask();
            task.complete();
            task.archive();
            assertThat(task.status()).isEqualTo(TaskStatus.ARCHIVED);
        }

        @Test
        void transicaoInvalidaMapeiaParaHttp409() {
            Task task = newTask();
            task.archive();
            assertThatThrownBy(task::complete)
                    .isInstanceOfSatisfying(InvalidTaskTransitionException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo("INVALID_TASK_TRANSITION"));
        }
    }
}
