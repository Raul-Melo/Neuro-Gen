package br.com.sentinelcorp.Neuro_Gen.focus.api;

import br.com.sentinelcorp.Neuro_Gen.focus.api.dto.StartFocusRequest;
import br.com.sentinelcorp.Neuro_Gen.focus.application.FocusSessionResult;
import br.com.sentinelcorp.Neuro_Gen.focus.application.FocusUseCase;
import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Sessões de foco (EPIC-04). */
@RestController
@RequestMapping("/api/v1/focus")
@Tag(name = "Focus — Sessões de foco", description = "Pomodoro configurável: start/pause/resume/complete")
public class FocusController {

    private final FocusUseCase focus;

    public FocusController(FocusUseCase focus) {
        this.focus = focus;
    }

    @PostMapping("/sessions")
    @Operation(summary = "Inicia uma sessão de foco (uma ativa por vez)")
    public ResponseEntity<FocusSessionResult> start(@Valid @RequestBody StartFocusRequest req) {
        FocusSessionResult result = focus.start(
                CurrentUser.requireId().asString(), req.taskId(), req.durationMinutes(), req.breakMinutes());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/sessions/current")
    @Operation(summary = "Retorna a sessão em andamento, se houver")
    public ResponseEntity<FocusSessionResult> current() {
        return focus.current(CurrentUser.requireId().asString())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/sessions/{id}/pause")
    @Operation(summary = "Pausa a sessão")
    public FocusSessionResult pause(@PathVariable String id) {
        return focus.pause(id, CurrentUser.requireId().asString());
    }

    @PostMapping("/sessions/{id}/resume")
    @Operation(summary = "Retoma a sessão")
    public FocusSessionResult resume(@PathVariable String id) {
        return focus.resume(id, CurrentUser.requireId().asString());
    }

    @PostMapping("/sessions/{id}/complete")
    @Operation(summary = "Conclui a sessão")
    public FocusSessionResult complete(@PathVariable String id) {
        return focus.complete(id, CurrentUser.requireId().asString());
    }

    @PostMapping("/sessions/{id}/cancel")
    @Operation(summary = "Cancela a sessão")
    public FocusSessionResult cancel(@PathVariable String id) {
        return focus.cancel(id, CurrentUser.requireId().asString());
    }

    @GetMapping("/sessions")
    @Operation(summary = "Histórico de sessões por período")
    public List<FocusSessionResult> history(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return focus.history(CurrentUser.requireId().asString(), from, to);
    }
}
