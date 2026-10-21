package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Lembrete vinculado a um Event ou Task. Quando {@code persistent} (nudge — RF-03.2), repete a cada
 * {@code repeatIntervalMinutes} até {@code maxRepetitions} ou até confirmação de leitura.
 */
public class Reminder {

    private final UUID id;
    private final ReminderTrigger trigger;
    private final boolean persistent;
    private final int repeatIntervalMinutes;
    private final int maxRepetitions;
    private int currentRepetitions;
    private ReminderStatus status;
    private Instant nextFireAt;
    private Instant confirmedAt;

    public Reminder(UUID id, ReminderTrigger trigger, boolean persistent, int repeatIntervalMinutes,
                    int maxRepetitions, int currentRepetitions, ReminderStatus status,
                    Instant nextFireAt, Instant confirmedAt) {
        this.id = id;
        this.trigger = trigger;
        this.persistent = persistent;
        this.repeatIntervalMinutes = repeatIntervalMinutes;
        this.maxRepetitions = maxRepetitions;
        this.currentRepetitions = currentRepetitions;
        this.status = status;
        this.nextFireAt = nextFireAt;
        this.confirmedAt = confirmedAt;
    }

    public static Reminder create(int minutesBefore, boolean persistent, int repeatIntervalMinutes,
                                  int maxRepetitions) {
        return new Reminder(UUID.randomUUID(), new ReminderTrigger(minutesBefore), persistent,
                repeatIntervalMinutes, maxRepetitions, 0, ReminderStatus.SCHEDULED, null, null);
    }

    /** Calcula o primeiro disparo a partir do horário base (início do evento/vencimento da tarefa). */
    public void schedule(Instant baseTime) {
        this.nextFireAt = trigger.resolveFor(baseTime);
        this.status = ReminderStatus.SCHEDULED;
    }

    public void markSent() {
        this.status = ReminderStatus.SENT;
        this.currentRepetitions++;
        if (persistent && shouldRepeat()) {
            this.nextFireAt = Instant.now().plusSeconds(repeatIntervalMinutes * 60L);
        } else if (persistent) {
            this.status = ReminderStatus.UNCONFIRMED_LIMIT_REACHED;
        }
    }

    public void confirm(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
        this.status = ReminderStatus.CONFIRMED;
    }

    public boolean shouldRepeat() {
        return persistent && currentRepetitions < maxRepetitions;
    }

    public UUID id() { return id; }
    public ReminderTrigger trigger() { return trigger; }
    public boolean persistent() { return persistent; }
    public int repeatIntervalMinutes() { return repeatIntervalMinutes; }
    public int maxRepetitions() { return maxRepetitions; }
    public int currentRepetitions() { return currentRepetitions; }
    public ReminderStatus status() { return status; }
    public Instant nextFireAt() { return nextFireAt; }
    public Instant confirmedAt() { return confirmedAt; }
}
