package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade filha de {@link Task} (não é aggregate próprio — sua consistência é gerida pela Task-mãe).
 */
public class Subtask {

    private final UUID id;
    private String title;
    private boolean done;
    private int position;
    private Instant completedAt;

    public Subtask(UUID id, String title, boolean done, int position, Instant completedAt) {
        this.id = id;
        this.title = title;
        this.done = done;
        this.position = position;
        this.completedAt = completedAt;
    }

    public static Subtask create(String title, int position) {
        return new Subtask(UUID.randomUUID(), title, false, position, null);
    }

    public void complete() {
        this.done = true;
        this.completedAt = Instant.now();
    }

    public void reopen() {
        this.done = false;
        this.completedAt = null;
    }

    public void reorder(int newPosition) {
        this.position = newPosition;
    }

    public UUID id() { return id; }
    public String title() { return title; }
    public boolean done() { return done; }
    public int position() { return position; }
    public Instant completedAt() { return completedAt; }
}
