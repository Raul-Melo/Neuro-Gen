package br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;

/** HTTP 404 — tarefa inexistente para este usuário. */
public class TaskNotFoundException extends NotFoundException {
    public TaskNotFoundException(String id) {
        super("Tarefa", id);
    }
}
