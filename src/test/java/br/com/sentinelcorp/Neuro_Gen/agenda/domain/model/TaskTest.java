package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
