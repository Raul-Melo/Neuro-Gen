package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;

import java.util.Map;

/**
 * Regra única de título para os itens da Agenda (Evento e Tarefa): obrigatório, sem espaços nas
 * pontas e com no máximo {@value #MAX_LENGTH} caracteres (coluna {@code VARCHAR(200)}).
 */
final class AgendaTitle {

    static final int MAX_LENGTH = 200;

    private AgendaTitle() {
    }

    static String validate(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException(Map.of("title", "Título é obrigatório"));
        }
        String trimmed = title.strip();
        if (trimmed.length() > MAX_LENGTH) {
            throw new ValidationException(Map.of("title",
                    "Título deve ter no máximo " + MAX_LENGTH + " caracteres"));
        }
        return trimmed;
    }
}
