package br.com.sentinelcorp.Neuro_Gen.shared.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Superclasse de todas as raízes de agregado.
 *
 * <p>Acumula {@link DomainEvent}s produzidos durante operações de domínio. A camada de aplicação
 * invoca {@link #pullDomainEvents()} após persistir o agregado e despacha os eventos — garantindo
 * que nenhum efeito colateral inter-módulo ocorra antes da transação ser consistente.</p>
 */
public abstract class AggregateRoot {

    private final transient List<DomainEvent> pendingEvents = new ArrayList<>();

    /** Registra um evento a ser despachado após a persistência. */
    protected void registerEvent(DomainEvent event) {
        if (event != null) {
            pendingEvents.add(event);
        }
    }

    /**
     * Retorna e <b>limpa</b> os eventos pendentes. Idempotente: uma segunda chamada devolve vazio.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> copy = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return copy;
    }

    /** Eventos pendentes (somente leitura) sem consumi-los — útil em testes. */
    public List<DomainEvent> peekDomainEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }
}
