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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cofre zero-knowledge (EPIC-08).
 *
 * <p><b>Segurança estrutural:</b> toda a cadeia {@code /api/v1/vault/**} nega o papel APOIO antes
 * de alcançar este controller (RF-08.5). O servidor jamais vê plaintext — apenas ciphertext.</p>
 *
 * <p><b>Fluxo de acesso às entradas:</b>
 * <ol>
 *   <li>POST /setup — configura uma vez, recebe recovery key (exibida uma única vez)</li>
 *   <li>POST /open — autentica com senha mestra, obtém {@code X-Vault-Session}</li>
 *   <li>CRUD em /entries — requer header {@code X-Vault-Session} em toda operação</li>
 *   <li>POST /lock — invalida a sessão ao final do uso</li>
 * </ol>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/vault")
@Tag(name = "Vault — Cofre (zero-knowledge, isolado)",
     description = "Armazenamento de credenciais cifradas pelo cliente. "
                 + "O servidor opera apenas sobre ciphertext — zero acesso ao plaintext. "
                 + "Datasource e transaction manager separados (ADR-005). "
                 + "Papel APOIO bloqueado estruturalmente (RF-08.5).")
public class VaultController {

    private static final String SESSION_HEADER = "X-Vault-Session";
    private static final String SESSION_DESC = "Token de sessão do Cofre obtido em POST /open";

    private final VaultUseCase vault;

    public VaultController(VaultUseCase vault) {
        this.vault = vault;
    }

    // ── Gerenciamento do Cofre ────────────────────────────────────────────────

    @PostMapping("/setup")
    @Operation(summary = "Configura o Cofre (uma única vez por conta)",
               description = "Inicializa o Cofre com a senha mestra (hash Argon2 calculado pelo cliente). "
                           + "Retorna a chave de recuperação — exibida UMA ÚNICA VEZ, não recuperável pelo servidor.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cofre configurado; retorna recovery key"),
        @ApiResponse(responseCode = "400", description = "Parâmetros Argon2 inválidos"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "409", description = "Cofre já configurado para este usuário")
    })
    public ResponseEntity<VaultSetupResult> setup(@Valid @RequestBody SetupVaultRequest req) {
        VaultSetupResult result = vault.setup(new SetupVaultCommand(
                CurrentUser.requireId().asString(), req.argon2Params(), req.masterPasswordHash()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/open")
    @Operation(summary = "Abre o Cofre com a senha mestra",
               description = "Verifica o hash da senha mestra e emite um token de sessão com TTL configurável "
                           + "em `vault.session.ttl-seconds`. Registra evento de auditoria.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cofre aberto; retorna X-Vault-Session token"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Senha mestra incorreta"),
        @ApiResponse(responseCode = "422", description = "Cofre não configurado — execute POST /setup primeiro")
    })
    public VaultSessionResult open(@Valid @RequestBody OpenVaultRequest req) {
        return vault.open(new OpenVaultCommand(CurrentUser.requireId().asString(), req.masterPasswordHash()));
    }

    @PostMapping("/recover")
    @Operation(summary = "Redefine a senha mestra usando a chave de recuperação (RF-08.11)",
               description = "Permite ao usuário redefinir a senha mestra informando a recovery key gerada no setup. "
                           + "Registra evento de auditoria. As entradas cifradas permanecem intactas (zero-knowledge).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha mestra redefinida"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Recovery key incorreta"),
        @ApiResponse(responseCode = "422", description = "Cofre não configurado")
    })
    public ResponseEntity<Void> recover(@Valid @RequestBody RecoverVaultRequest req) {
        vault.recover(new RecoverVaultCommand(
                CurrentUser.requireId().asString(), req.recoveryKey(), req.newMasterPasswordHash()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lock")
    @Operation(summary = "Bloqueia o Cofre",
               description = "Invalida o token de sessão. Recomendado ao término de cada uso.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Cofre bloqueado / sessão invalidada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado")
    })
    public ResponseEntity<Void> lock(
            @Parameter(description = SESSION_DESC, required = true)
            @RequestHeader(SESSION_HEADER) String sessionToken) {
        vault.lock(sessionToken);
        return ResponseEntity.noContent().build();
    }

    // ── Entradas ─────────────────────────────────────────────────────────────

    @PostMapping("/entries")
    @Operation(summary = "Cria uma entrada cifrada",
               description = "O payload `encryptedPayload` deve ser cifrado pelo cliente antes do envio. "
                           + "O servidor armazena e retorna apenas ciphertext.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Entrada criada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão do Cofre inválida ou expirada")
    })
    public ResponseEntity<VaultEntryResult> create(
            @Parameter(description = SESSION_DESC, required = true)
            @RequestHeader(SESSION_HEADER) String sessionToken,
            @Valid @RequestBody CreateEntryRequest req) {
        VaultEntryResult result = vault.createEntry(sessionToken, new CreateVaultEntryCommand(
                CurrentUser.requireId().asString(), req.label(), req.encryptedPayload(), req.entryType()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/entries/{id}")
    @Operation(summary = "Busca uma entrada cifrada por ID",
               description = "Retorna os dados da entrada (ciphertext). Requer sessão válida do Cofre.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrada encontrada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão do Cofre inválida ou entrada não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Entrada não encontrada")
    })
    public VaultEntryResult getEntry(
            @Parameter(description = SESSION_DESC, required = true)
            @RequestHeader(SESSION_HEADER) String sessionToken,
            @Parameter(description = "ID UUID da entrada", required = true) @PathVariable String id) {
        return vault.getEntry(sessionToken, id);
    }

    @GetMapping("/entries")
    @Operation(summary = "Lista as entradas cifradas do Cofre",
               description = "Retorna todas as entradas do usuário (apenas ciphertext e metadados).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de entradas"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão do Cofre inválida ou expirada")
    })
    public List<VaultEntryResult> list(
            @Parameter(description = SESSION_DESC, required = true)
            @RequestHeader(SESSION_HEADER) String sessionToken) {
        return vault.listEntries(sessionToken);
    }

    @PutMapping("/entries/{id}")
    @Operation(summary = "Atualiza uma entrada cifrada",
               description = "Substitui o ciphertext e/ou o label da entrada. "
                           + "O novo payload deve ser cifrado pelo cliente antes do envio.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Entrada atualizada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão inválida ou entrada não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Entrada não encontrada")
    })
    public VaultEntryResult update(
            @Parameter(description = SESSION_DESC, required = true)
            @RequestHeader(SESSION_HEADER) String sessionToken,
            @Parameter(description = "ID UUID da entrada", required = true) @PathVariable String id,
            @Valid @RequestBody UpdateEntryRequest req) {
        return vault.updateEntry(sessionToken, new UpdateVaultEntryCommand(
                id, CurrentUser.requireId().asString(), req.label(), req.encryptedPayload()));
    }

    @DeleteMapping("/entries/{id}")
    @Operation(summary = "Exclui uma entrada cifrada",
               description = "Remoção permanente. O servidor descarta o ciphertext sem possibilidade de recuperação.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Entrada excluída"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Sessão inválida ou entrada não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Entrada não encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = SESSION_DESC, required = true)
            @RequestHeader(SESSION_HEADER) String sessionToken,
            @Parameter(description = "ID UUID da entrada", required = true) @PathVariable String id) {
        vault.deleteEntry(sessionToken, id);
        return ResponseEntity.noContent().build();
    }
}
