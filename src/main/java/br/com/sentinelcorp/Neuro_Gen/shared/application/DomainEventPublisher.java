package br.com.sentinelcorp.Neuro_Gen.shared.application;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.AggregateRoot;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Wrapper fino sobre {@link ApplicationEventPublisher}. A camada de aplicação chama
 * {@link #publishFrom(AggregateRoot)} após persistir o agregado — os eventos acumulados são
 * drenados ({@code pullDomainEvents}) e despachados, integrando módulos sem acoplamento direto.
 */
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher delegate;

    public DomainEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    public void publishFrom(AggregateRoot aggregate) {
        List<DomainEvent> events = aggregate.pullDomainEvents();
        events.forEach(delegate::publishEvent);
    }

    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
