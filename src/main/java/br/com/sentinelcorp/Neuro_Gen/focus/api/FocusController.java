package br.com.sentinelcorp.Neuro_Gen.focus.api;

import br.com.sentinelcorp.Neuro_Gen.focus.api.dto.StartFocusRequest;
import br.com.sentinelcorp.Neuro_Gen.focus.application.FocusSessionResult;
import br.com.sentinelcorp.Neuro_Gen.focus.application.FocusUseCase;
import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Sessões de foco (EPIC-04).
 *
 * <p>Máquina de estados: {@code RUNNING → PAUSED → RUNNING → COMPLETED | CANCELLED}</p>
 */
@RestController
@RequestMapping("/api/v1/focus")
@Tag(name = "Focus — Sessões de foco",
     description = "Pomodoro configurável com pausas cronometradas. Uma sessão ativa por vez. "
                 + "Emite eventos de domínio FocusSessionStarted/Ended integrados ao módulo Agenda.")
public class FocusController {

    private final FocusUseCase focus;

    public FocusController(FocusUseCase focus) {
        this.focus = focus;
    }

    @PostMapping("/sessions")
    @Operation(summary = "Inicia uma sessão de foco",
               description = "Cria uma nova sessão no estado RUNNING. Retorna 409 se já houver uma sessão ativa.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sessão iniciada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (duração/pausa fora do permitido)"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "409", description = "Já existe uma sessão ativa para o usuário")
    })
    public ResponseEntity<FocusSessionResult> start(@Valid @RequestBody StartFocusRequest req) {
        FocusSessionResult result = focus.start(
                CurrentUser.requireId().asString(), req.taskId(), req.durationMinutes(), req.breakMinutes());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Busca uma sessão por ID",
               description = "Retorna os dados de uma sessão. Retorna 403 se a sessão não pertencer ao usuário autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessão encontrada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Sessão não encontrada")
    })
    public FocusSessionResult getById(
            @Parameter(description = "ID UUID da sessão", required = true) @PathVariable String id) {
        return focus.getById(id, CurrentUser.requireId().asString());
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Remove uma sessão do histórico",
               description = "Exclui permanentemente uma sessão. Retorna 403 se não pertencer ao usuário.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sessão removida"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Sessão não encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID UUID da sessão", required = true) @PathVariable String id) {
        focus.delete(id, CurrentUser.requireId().asString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/current")
    @Operation(summary = "Retorna a sessão em andamento",
               description = "Retorna 204 sem corpo se não houver sessão ativa (RUNNING ou PAUSED).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessão em andamento"),
        @ApiResponse(responseCode = "204", description = "Nenhuma sessão ativa"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado")
    })
    public ResponseEntity<FocusSessionResult> current() {
        return focus.current(CurrentUser.requireId().asString())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/sessions/{id}/pause")
    @Operation(summary = "Pausa a sessão",
               description = "Registra um PauseRecord com timestamp. A sessão passa para PAUSED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessão pausada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Sessão não encontrada"),
        @ApiResponse(responseCode = "422", description = "Transição de estado inválida")
    })
    public FocusSessionResult pause(
            @Parameter(description = "ID UUID da sessão", required = true) @PathVariable String id) {
        return focus.pause(id, CurrentUser.requireId().asString());
    }

    @PostMapping("/sessions/{id}/resume")
    @Operation(summary = "Retoma a sessão",
               description = "Fecha o PauseRecord aberto. A sessão volta para RUNNING.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessão retomada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Sessão não encontrada"),
        @ApiResponse(responseCode = "422", description = "Transição de estado inválida")
    })
    public FocusSessionResult resume(
            @Parameter(description = "ID UUID da sessão", required = true) @PathVariable String id) {
        return focus.resume(id, CurrentUser.requireId().asString());
    }

    @PostMapping("/sessions/{id}/complete")
    @Operation(summary = "Conclui a sessão",
               description = "Finaliza com status COMPLETED e emite evento FocusSessionEnded.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessão concluída"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Sessão não encontrada"),
        @ApiResponse(responseCode = "422", description = "Transição de estado inválida")
    })
    public FocusSessionResult complete(
            @Parameter(description = "ID UUID da sessão", required = true) @PathVariable String id) {
        return focus.complete(id, CurrentUser.requireId().asString());
    }

    @PostMapping("/sessions/{id}/cancel")
    @Operation(summary = "Cancela a sessão",
               description = "Finaliza com status CANCELLED. A sessão permanece no histórico.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessão cancelada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Sessão não encontrada"),
        @ApiResponse(responseCode = "422", description = "Transição de estado inválida")
    })
    public FocusSessionResult cancel(
            @Parameter(description = "ID UUID da sessão", required = true) @PathVariable String id) {
        return focus.cancel(id, CurrentUser.requireId().asString());
    }

    @GetMapping("/sessions")
    @Operation(summary = "Histórico de sessões por período",
               description = "Parâmetros `from` e `to` em formato ISO-8601 (ex: 2026-01-01T00:00:00Z).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de sessões do período"),
        @ApiResponse(responseCode = "400", description = "Formato de data/hora inválido"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado")
    })
    public List<FocusSessionResult> history(
            @Parameter(description = "Início do período (ISO-8601)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Fim do período (ISO-8601)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return focus.history(CurrentUser.requireId().asString(), from, to);
    }
}
