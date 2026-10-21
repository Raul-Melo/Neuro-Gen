package br.com.sentinelcorp.Neuro_Gen.vault.api;

import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import br.com.sentinelcorp.Neuro_Gen.vault.api.dto.VaultRequests.CreateEntryRequest;
import br.com.sentinelcorp.Neuro_Gen.vault.api.dto.VaultRequests.OpenVaultRequest;
import br.com.sentinelcorp.Neuro_Gen.vault.api.dto.VaultRequests.RecoverVaultRequest;
import br.com.sentinelcorp.Neuro_Gen.vault.api.dto.VaultRequests.SetupVaultRequest;
import br.com.sentinelcorp.Neuro_Gen.vault.api.dto.VaultRequests.UpdateEntryRequest;
import br.com.sentinelcorp.Neuro_Gen.vault.application.VaultUseCase;
import br.com.sentinelcorp.Neuro_Gen.vault.application.command.VaultCommands.CreateVaultEntryCommand;
import br.com.sentinelcorp.Neuro_Gen.vault.application.command.VaultCommands.OpenVaultCommand;
import br.com.sentinelcorp.Neuro_Gen.vault.application.command.VaultCommands.RecoverVaultCommand;
import br.com.sentinelcorp.Neuro_Gen.vault.application.command.VaultCommands.SetupVaultCommand;
import br.com.sentinelcorp.Neuro_Gen.vault.application.command.VaultCommands.UpdateVaultEntryCommand;
import br.com.sentinelcorp.Neuro_Gen.vault.application.result.VaultResults.VaultEntryResult;
import br.com.sentinelcorp.Neuro_Gen.vault.application.result.VaultResults.VaultSessionResult;
import br.com.sentinelcorp.Neuro_Gen.vault.application.result.VaultResults.VaultSetupResult;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cofre zero-knowledge (EPIC-08). Toda a cadeia {@code /api/v1/vault/**} NEGA estruturalmente o
 * papel APOIO (RF-08.5), antes de alcançar este controller. Operações sobre entradas exigem o
 * cabeçalho {@code X-Vault-Session} obtido em {@code POST /open}.
 */
@RestController
@RequestMapping("/api/v1/vault")
@Tag(name = "Vault — Cofre (isolado, zero-knowledge)", description = "Setup, abertura e entradas cifradas")
public class VaultController {

    private static final String SESSION_HEADER = "X-Vault-Session";

    private final VaultUseCase vault;

    public VaultController(VaultUseCase vault) {
        this.vault = vault;
    }

    @PostMapping("/setup")
    @Operation(summary = "Configura o Cofre e retorna a chave de recuperação (uma única vez)")
    public ResponseEntity<VaultSetupResult> setup(@Valid @RequestBody SetupVaultRequest req) {
        VaultSetupResult result = vault.setup(new SetupVaultCommand(
                CurrentUser.requireId().asString(), req.argon2Params(), req.masterPasswordHash()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/open")
    @Operation(summary = "Abre o Cofre com a senha mestra e retorna um token de sessão")
    public VaultSessionResult open(@Valid @RequestBody OpenVaultRequest req) {
        return vault.open(new OpenVaultCommand(CurrentUser.requireId().asString(), req.masterPasswordHash()));
    }

    @PostMapping("/recover")
    @Operation(summary = "Redefine a senha mestra usando a chave de recuperação (RF-08.11)")
    public ResponseEntity<Void> recover(@Valid @RequestBody RecoverVaultRequest req) {
        vault.recover(new RecoverVaultCommand(
                CurrentUser.requireId().asString(), req.recoveryKey(), req.newMasterPasswordHash()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lock")
    @Operation(summary = "Bloqueia o Cofre (invalida a sessão)")
    public ResponseEntity<Void> lock(@RequestHeader(SESSION_HEADER) String sessionToken) {
        vault.lock(sessionToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/entries")
    @Operation(summary = "Cria uma entrada cifrada")
    public ResponseEntity<VaultEntryResult> create(
            @RequestHeader(SESSION_HEADER) String sessionToken,
            @Valid @RequestBody CreateEntryRequest req) {
        VaultEntryResult result = vault.createEntry(sessionToken, new CreateVaultEntryCommand(
                CurrentUser.requireId().asString(), req.label(), req.encryptedPayload(), req.entryType()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/entries")
    @Operation(summary = "Lista as entradas cifradas do Cofre")
    public List<VaultEntryResult> list(@RequestHeader(SESSION_HEADER) String sessionToken) {
        return vault.listEntries(sessionToken);
    }

    @PutMapping("/entries/{id}")
    @Operation(summary = "Atualiza uma entrada cifrada")
    public VaultEntryResult update(
            @RequestHeader(SESSION_HEADER) String sessionToken,
            @PathVariable String id,
            @Valid @RequestBody UpdateEntryRequest req) {
        return vault.updateEntry(sessionToken, new UpdateVaultEntryCommand(
                id, CurrentUser.requireId().asString(), req.label(), req.encryptedPayload()));
    }

    @DeleteMapping("/entries/{id}")
    @Operation(summary = "Exclui uma entrada cifrada")
    public ResponseEntity<Void> delete(
            @RequestHeader(SESSION_HEADER) String sessionToken,
            @PathVariable String id) {
        vault.deleteEntry(sessionToken, id);
        return ResponseEntity.noContent().build();
    }
}
