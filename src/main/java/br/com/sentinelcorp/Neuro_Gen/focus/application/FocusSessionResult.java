package br.com.sentinelcorp.Neuro_Gen.focus.application;

import br.com.sentinelcorp.Neuro_Gen.focus.domain.model.FocusSession;

public record FocusSessionResult(
        String id,
        String linkedTaskId,
        int plannedMinutes,
        int breakMinutes,
        String status,
        long effectiveSeconds,
        String startedAt,
        String endedAt) {

    public static FocusSessionResult from(FocusSession s) {
        return new FocusSessionResult(
                s.id().asString(),
                s.linkedTaskId(),
                s.plannedMinutes(),
                s.breakMinutes(),
                s.status().name(),
                s.effectiveTime().toSeconds(),
                s.startedAt().toString(),
                s.endedAt() == null ? null : s.endedAt().toString());
    }
}
