package br.com.sentinelcorp.Neuro_Gen.agenda.domain.model;

import br.com.sentinelcorp.Neuro_Gen.agenda.domain.exception.EventAlreadyCancelledException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Invariantes do agregado Evento: título válido e imutabilidade após cancelamento. */
class EventTest {

    private static final Instant START = Instant.now().plus(1, ChronoUnit.DAYS);

    private static TimeRange umaHora(Instant start) {
        return new TimeRange(start, start.plus(1, ChronoUnit.HOURS));
    }

    private Event newEvent(String title) {
        return Event.create(UserId.generate(), title, null, umaHora(START), null, null, null, false, List.of());
    }

    @Test
    void criaEventoComTipoPadraoAppointment() {
        Event event = newEvent("Consulta");
        assertThat(event.type()).isEqualTo(EventType.APPOINTMENT);
        assertThat(event.isCancelled()).isFalse();
    }

    @Test
    void rejeitaTituloEmBranco() {
        assertThatThrownBy(() -> newEvent("  "))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("title"));
    }

    @Test
    void rejeitaTituloMaiorQue200Caracteres() {
        assertThatThrownBy(() -> newEvent("x".repeat(201))).isInstanceOf(ValidationException.class);
    }

    @Test
    void cancelarMarcaOEventoComoCancelado() {
        Event event = newEvent("Consulta");
        event.cancel(RecurrenceEditScope.THIS_OCCURRENCE);
        assertThat(event.isCancelled()).isTrue();
        assertThat(event.cancelledAt()).isNotNull();
    }

    @Test
    void naoCancelaEventoJaCancelado() {
        Event event = newEvent("Consulta");
        event.cancel(RecurrenceEditScope.THIS_OCCURRENCE);
        Instant primeiroCancelamento = event.cancelledAt();

        assertThatThrownBy(() -> event.cancel(RecurrenceEditScope.THIS_OCCURRENCE))
                .isInstanceOf(EventAlreadyCancelledException.class);
        assertThat(event.cancelledAt()).isEqualTo(primeiroCancelamento);
    }

    @Test
    void naoReagendaEventoCancelado() {
        Event event = newEvent("Consulta");
        event.cancel(RecurrenceEditScope.THIS_OCCURRENCE);
        TimeRange original = event.timeRange();

        assertThatThrownBy(() -> event.reschedule(umaHora(START.plus(2, ChronoUnit.DAYS)),
                RecurrenceEditScope.THIS_OCCURRENCE))
                .isInstanceOf(EventAlreadyCancelledException.class);
        assertThat(event.timeRange()).isEqualTo(original);
    }

    @Test
    void naoAdicionaLembreteEmEventoCancelado() {
        Event event = newEvent("Consulta");
        event.cancel(RecurrenceEditScope.THIS_OCCURRENCE);
        assertThatThrownBy(() -> event.addReminder(Reminder.create(15, false, 0, 0)))
                .isInstanceOf(EventAlreadyCancelledException.class);
    }

    @Test
    void cancelamentoRepetidoMapeiaParaHttp409() {
        Event event = newEvent("Consulta");
        event.cancel(RecurrenceEditScope.THIS_OCCURRENCE);
        assertThatThrownBy(() -> event.cancel(RecurrenceEditScope.THIS_OCCURRENCE))
                .isInstanceOfSatisfying(EventAlreadyCancelledException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo("EVENT_ALREADY_CANCELLED"));
    }
}
