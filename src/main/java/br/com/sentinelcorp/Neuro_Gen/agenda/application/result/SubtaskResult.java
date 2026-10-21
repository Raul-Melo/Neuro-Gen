package br.com.sentinelcorp.Neuro_Gen.agenda.application.result;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Subtask;

public record SubtaskResult(String id, String title, boolean done, int position) {

    public static SubtaskResult from(Subtask s) {
        return new SubtaskResult(s.id().toString(), s.title(), s.done(), s.position());
    }
}
