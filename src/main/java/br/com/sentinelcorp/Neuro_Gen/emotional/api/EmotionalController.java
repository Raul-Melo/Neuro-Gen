package br.com.sentinelcorp.Neuro_Gen.emotional.api;

import br.com.sentinelcorp.Neuro_Gen.emotional.api.dto.MoodCheckInRequest;
import br.com.sentinelcorp.Neuro_Gen.emotional.api.dto.UpdateJournalRequest;
import br.com.sentinelcorp.Neuro_Gen.emotional.api.dto.WriteJournalRequest;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.EmotionalUseCase;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.DeleteCheckInCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.DeleteJournalCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.EditJournalCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.SubmitMoodCheckInCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.WriteJournalCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.result.JournalEntryResult;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.result.MoodCheckInResult;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.result.MoodTrendResult;
import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

/**
 * Regulação emocional (EPIC-05) — dados sensíveis de saúde (LGPD).
 *
 * <p>Notas e conteúdo do diário são cifrados em repouso com AES-256-GCM ({@code ng_sensitive}).</p>
 * <p>Acesso delegado (Apoio → Titular) requer permissão EMOTIONAL_CHECKIN via vínculo ativo.</p>
 */
@RestController
@RequestMapping("/api/v1/emotional")
@Tag(name = "Emotional — Regulação emocional",
     description = "Check-ins de humor/energia (1–5) e diário cifrado. "
                 + "Dados sensíveis de saúde: cifrados em repouso com AES-256-GCM (LGPD). "
                 + "A tendência emocional pode ser consultada por um Apoio com permissão EMOTIONAL_CHECKIN.")
public class EmotionalController {

    private final EmotionalUseCase emotional;

    public EmotionalController(EmotionalUseCase emotional) {
        this.emotional = emotional;
    }

    // ── Check-ins ─────────────────────────────────────────────────────────────

    @PostMapping("/checkins")
    @Operation(summary = "Registra um check-in de humor/energia",
               description = "Máximo de 1 check-in por dia. Notas cifradas em repouso. "
                           + "Retorna 409 se já existir um check-in para hoje.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Check-in registrado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (scores fora de 1–5)"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "409", description = "Já existe um check-in para hoje")
    })
    public ResponseEntity<MoodCheckInResult> checkIn(@Valid @RequestBody MoodCheckInRequest req) {
        MoodCheckInResult result = emotional.submitCheckIn(new SubmitMoodCheckInCommand(
                CurrentUser.requireId().asString(), req.moodScore(), req.energyLevel(), req.notes()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/checkins")
    @Operation(summary = "Lista check-ins do período",
               description = "Retorna os check-ins dos últimos N dias (padrão: 30). "
                           + "Notas são descriptografadas antes de retornar.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de check-ins"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado")
    })
    public List<MoodCheckInResult> listCheckIns(
            @Parameter(description = "Número de dias retroativos (padrão: 30)")
            @RequestParam(defaultValue = "30") int periodDays) {
        return emotional.listCheckIns(CurrentUser.requireId().asString(), periodDays);
    }

    @GetMapping("/checkins/{id}")
    @Operation(summary = "Busca um check-in por ID",
               description = "Retorna 403 se o check-in não pertencer ao usuário autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Check-in encontrado"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Check-in não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Check-in não encontrado")
    })
    public MoodCheckInResult getCheckIn(
            @Parameter(description = "ID UUID do check-in", required = true) @PathVariable String id) {
        return emotional.getCheckIn(CurrentUser.requireId().asString(), id);
    }

    @DeleteMapping("/checkins/{id}")
    @Operation(summary = "Remove um check-in",
               description = "Remoção permanente. Retorna 403 se o check-in não pertencer ao usuário.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Check-in removido"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Check-in não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Check-in não encontrado")
    })
    public ResponseEntity<Void> deleteCheckIn(
            @Parameter(description = "ID UUID do check-in", required = true) @PathVariable String id) {
        emotional.deleteCheckIn(new DeleteCheckInCommand(CurrentUser.requireId().asString(), id));
        return ResponseEntity.noContent().build();
    }

    // ── Diário ────────────────────────────────────────────────────────────────

    @PostMapping("/journal")
    @Operation(summary = "Escreve uma entrada de diário emocional",
               description = "O conteúdo é cifrado integralmente com AES-256-GCM antes de ser persistido. "
                           + "Pode ser vinculado a um check-in do mesmo dia via `relatedCheckInId`.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Entrada registrada"),
        @ApiResponse(responseCode = "400", description = "Conteúdo vazio ou excede 10 000 caracteres"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado")
    })
    public ResponseEntity<JournalEntryResult> journal(@Valid @RequestBody WriteJournalRequest req) {
        JournalEntryResult result = emotional.writeJournal(new WriteJournalCommand(
                CurrentUser.requireId().asString(), req.content(), req.relatedCheckInId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/journal")
    @Operation(summary = "Lista todas as entradas de diário do usuário autenticado",
               description = "Entradas ordenadas por data de criação decrescente. Conteúdo descriptografado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de entradas"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado")
    })
    public List<JournalEntryResult> listJournal() {
        return emotional.listJournalEntries(CurrentUser.requireId().asString());
    }

    @GetMapping("/journal/{id}")
    @Operation(summary = "Busca uma entrada de diário por ID",
               description = "Retorna a entrada descriptografada. "
                           + "Retorna 403 se não pertencer ao usuário autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrada encontrada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Entrada não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Entrada não encontrada")
    })
    public JournalEntryResult getJournalEntry(
            @Parameter(description = "ID UUID da entrada", required = true) @PathVariable String id) {
        return emotional.getJournalEntry(CurrentUser.requireId().asString(), id);
    }

    @PutMapping("/journal/{id}")
    @Operation(summary = "Edita o conteúdo de uma entrada de diário",
               description = "Substitui o conteúdo integralmente (PUT semântico). O novo conteúdo é cifrado novamente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrada atualizada"),
        @ApiResponse(responseCode = "400", description = "Conteúdo inválido"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Entrada não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Entrada não encontrada")
    })
    public JournalEntryResult editJournalEntry(
            @Parameter(description = "ID UUID da entrada", required = true) @PathVariable String id,
            @Valid @RequestBody UpdateJournalRequest req) {
        return emotional.editJournalEntry(new EditJournalCommand(
                CurrentUser.requireId().asString(), id, req.content()));
    }

    @DeleteMapping("/journal/{id}")
    @Operation(summary = "Remove uma entrada de diário",
               description = "Remoção permanente do ciphertext. Não pode ser desfeita.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Entrada removida"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Entrada não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Entrada não encontrada")
    })
    public ResponseEntity<Void> deleteJournalEntry(
            @Parameter(description = "ID UUID da entrada", required = true) @PathVariable String id) {
        emotional.deleteJournalEntry(new DeleteJournalCommand(CurrentUser.requireId().asString(), id));
        return ResponseEntity.noContent().build();
    }

    // ── Tendência ─────────────────────────────────────────────────────────────

    @GetMapping("/trend")
    @Operation(summary = "Tendência emocional do período",
               description = "**Aviso:** padrão observado — não é diagnóstico médico. "
                           + "Apoio pode consultar a tendência de um Titular com permissão EMOTIONAL_CHECKIN. "
                           + "Se `userId` for omitido, usa o usuário autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tendência calculada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Apoio sem permissão EMOTIONAL_CHECKIN para o Titular")
    })
    public MoodTrendResult trend(
            @Parameter(description = "ID do Titular (opcional — omitir para o próprio usuário)")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Período em dias (padrão: 30)")
            @RequestParam(defaultValue = "30") int periodDays) {
        String requester = CurrentUser.requireId().asString();
        String owner = (userId == null || userId.isBlank()) ? requester : userId;
        return emotional.getTrend(requester, owner, periodDays);
    }
}
