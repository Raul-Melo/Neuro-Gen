package br.com.sentinelcorp.Neuro_Gen.agenda.domain.port;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.Event;
import br.com.sentinelcorp.Neuro_Gen.agenda.domain.model.EventId;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventRepository {

    Event save(Event event);

    Optional<Event> findById(EventId id);

    List<Event> findByOwnerAndPeriod(UserId owner, Instant from, Instant to);

    void delete(EventId id);
}
