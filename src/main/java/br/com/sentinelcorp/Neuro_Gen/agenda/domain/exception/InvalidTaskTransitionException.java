package br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.TaskStatus;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.DomainException;
import org.springframework.http.HttpStatus;

/** HTTP 409 — a operação não é permitida a partir do status atual da tarefa. */
public class InvalidTaskTransitionException extends DomainException {

    public InvalidTaskTransitionException(TaskStatus current, String action) {
        super("INVALID_TASK_TRANSITION",
                "Não é possível " + action + " uma tarefa com status " + current,
                HttpStatus.CONFLICT);
    }
}
