package br.com.sentinelcorp.Neuro_Gen.agenda.api;

import br.com.sentinelcorp.Neuro_Gen.agenda.api.dto.CreateEventRequest;
import br.com.sentinelcorp.Neuro_Gen.agenda.api.dto.RescheduleEventRequest;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.EventUseCase;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.CreateEventCommand;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.ReminderInput;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.command.AgendaCommands.RescheduleEventCommand;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.result.EventResult;
import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Eventos/compromissos com hora fixa (EPIC-02). */
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Agenda — Eventos", description = "Compromissos com hora fixa, recorrência e lembretes")
public class EventController {

    private final EventUseCase events;

    public EventController(EventUseCase events) {
        this.events = events;
    }

    @PostMapping
    @Operation(summary = "Cria um evento")
    public ResponseEntity<EventResult> create(@Valid @RequestBody CreateEventRequest req) {
        List<ReminderInput> reminders = req.reminders() == null ? List.of()
                : req.reminders().stream().map(r -> r.toInput()).toList();
        EventResult result = events.create(new CreateEventCommand(
                CurrentUser.requireId().asString(), req.title(), req.description(),
                req.startAt(), req.endAt(), req.rrule(), req.type(), req.categoryId(),
                req.sharedWithSupport(), reminders));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}/reschedule")
    @Operation(summary = "Reagenda um evento (exige escopo de recorrência)")
    public EventResult reschedule(@PathVariable String id, @Valid @RequestBody RescheduleEventRequest req) {
        return events.reschedule(new RescheduleEventCommand(
                id, CurrentUser.requireId().asString(), req.startAt(), req.endAt(), req.scopeOrDefault()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela um evento")
    public ResponseEntity<Void> cancel(@PathVariable String id,
                                       @RequestParam(defaultValue = "THIS_OCCURRENCE") String scope) {
        events.cancel(id, CurrentUser.requireId().asString(), scope);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um evento")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        events.delete(id, CurrentUser.requireId().asString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lista eventos por período (visão de calendário)")
    public List<EventResult> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return events.listByPeriod(CurrentUser.requireId().asString(), from, to);
    }
}
