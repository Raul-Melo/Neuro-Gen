package br.com.sentinelcorp.Neuro_Gen.emotional.api;

import br.com.sentinelcorp.Neuro_Gen.emotional.api.dto.MoodCheckInRequest;
import br.com.sentinelcorp.Neuro_Gen.emotional.api.dto.WriteJournalRequest;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.EmotionalUseCase;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.SubmitMoodCheckInCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.command.EmotionalCommands.WriteJournalCommand;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.result.MoodCheckInResult;
import br.com.sentinelcorp.Neuro_Gen.emotional.application.result.MoodTrendResult;
import br.com.sentinelcorp.Neuro_Gen.shared.api.MessageResponse;
import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Regulação emocional (EPIC-05) — dados sensíveis de saúde (LGPD). A tendência de outro usuário
 * (Titular) só é acessível a um Apoio com permissão EMOTIONAL_CHECKIN concedida.
 */
@RestController
@RequestMapping("/api/v1/emotional")
@Tag(name = "Emotional — Regulação emocional", description = "Check-in de humor, diário e tendências (dado sensível)")
public class EmotionalController {

    private final EmotionalUseCase emotional;

    public EmotionalController(EmotionalUseCase emotional) {
        this.emotional = emotional;
    }

    @PostMapping("/checkins")
    @Operation(summary = "Registra um check-in de humor/energia")
    public ResponseEntity<MoodCheckInResult> checkIn(@Valid @RequestBody MoodCheckInRequest req) {
        MoodCheckInResult result = emotional.submitCheckIn(new SubmitMoodCheckInCommand(
                CurrentUser.requireId().asString(), req.moodScore(), req.energyLevel(), req.notes()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/journal")
    @Operation(summary = "Escreve uma entrada de diário emocional (cifrada)")
    public ResponseEntity<MessageResponse> journal(@Valid @RequestBody WriteJournalRequest req) {
        String id = emotional.writeJournal(new WriteJournalCommand(
                CurrentUser.requireId().asString(), req.content(), req.relatedCheckInId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Entrada registrada: " + id));
    }

    @GetMapping("/trend")
    @Operation(summary = "Tendência emocional do período (padrão observado — não é diagnóstico)")
    public MoodTrendResult trend(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "30") int periodDays) {
        String requester = CurrentUser.requireId().asString();
        String owner = (userId == null || userId.isBlank()) ? requester : userId;
        return emotional.getTrend(requester, owner, periodDays);
    }
}
