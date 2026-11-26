package br.com.sentinelcorp.Neuro_Gen.link.api;

import br.com.sentinelcorp.Neuro_Gen.link.api.dto.AcceptInviteRequest;
import br.com.sentinelcorp.Neuro_Gen.link.api.dto.InviteResponse;
import br.com.sentinelcorp.Neuro_Gen.link.api.dto.LinkResponse;
import br.com.sentinelcorp.Neuro_Gen.link.api.dto.UpdatePermissionsRequest;
import br.com.sentinelcorp.Neuro_Gen.link.application.InviteUseCase;
import br.com.sentinelcorp.Neuro_Gen.link.application.LinkLifecycleUseCase;
import br.com.sentinelcorp.Neuro_Gen.link.application.PermissionUseCase;
import br.com.sentinelcorp.Neuro_Gen.link.application.command.LinkCommands.AcceptInviteCommand;
import br.com.sentinelcorp.Neuro_Gen.link.application.command.LinkCommands.CreateInviteCommand;
import br.com.sentinelcorp.Neuro_Gen.link.application.command.LinkCommands.RevokeLinkCommand;
import br.com.sentinelcorp.Neuro_Gen.link.application.command.LinkCommands.UpdatePermissionsCommand;
import br.com.sentinelcorp.Neuro_Gen.shared.security.AuthenticatedUser;
import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vínculo Titular/Apoio (EPIC-01).
 *
 * <p>Fluxo: Titular cria convite → Apoio aceita → vínculo ACTIVE → Titular concede permissões
 * granulares por módulo → Titular revoga quando necessário (efeito imediato).</p>
 *
 * <p>O VAULT nunca aparece na matriz de permissões — acesso estruturalmente bloqueado (RF-08.5).</p>
 */
@RestController
@RequestMapping("/api/v1/links")
@Tag(name = "Link — Vínculo Titular/Apoio",
     description = "Fluxo de convite, aceite e permissões granulares por módulo. "
                 + "Titular gerencia; Apoio aceita e herda permissões concedidas. "
                 + "Revogação invalida imediatamente o acesso delegado.")
public class LinkController {

    private final InviteUseCase invite;
    private final PermissionUseCase permission;
    private final LinkLifecycleUseCase lifecycle;

    public LinkController(InviteUseCase invite, PermissionUseCase permission, LinkLifecycleUseCase lifecycle) {
        this.invite = invite;
        this.permission = permission;
        this.lifecycle = lifecycle;
    }

    @PostMapping
    @PreAuthorize("hasRole('TITULAR')")
    @Operation(summary = "Cria um convite de vínculo (apenas Titular)",
               description = "Gera um token de convite de uso único válido por 72 h. "
                           + "Apenas usuários com papel TITULAR podem criar convites.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Convite criado; retorna token e URL"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Papel insuficiente — requer TITULAR")
    })
    public ResponseEntity<InviteResponse> createInvite() {
        var result = invite.create(new CreateInviteCommand(CurrentUser.requireId().asString()));
        return ResponseEntity.status(HttpStatus.CREATED).body(InviteResponse.from(result));
    }

    @PostMapping("/accept")
    @Operation(summary = "Aceita um convite de vínculo",
               description = "Ativa o vínculo. O token é de uso único e expira em 72 h. "
                           + "Apenas contas Apoio devem aceitar convites.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vínculo ativado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "404", description = "Token de convite inválido ou expirado"),
        @ApiResponse(responseCode = "409", description = "Convite já utilizado ou vínculo já existe")
    })
    public LinkResponse accept(@Valid @RequestBody AcceptInviteRequest req) {
        var result = invite.accept(new AcceptInviteCommand(req.inviteToken(), CurrentUser.requireId().asString()));
        return LinkResponse.from(result);
    }

    @GetMapping("/{linkId}")
    @Operation(summary = "Busca um vínculo por ID",
               description = "Retorna os detalhes do vínculo. Acessível pelo Titular dono e pelo Apoio participante.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vínculo encontrado"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Vínculo não pertence ao usuário"),
        @ApiResponse(responseCode = "404", description = "Vínculo não encontrado")
    })
    public LinkResponse getById(
            @Parameter(description = "ID UUID do vínculo", required = true) @PathVariable String linkId) {
        var result = lifecycle.getById(linkId, CurrentUser.requireId().asString());
        return LinkResponse.from(result);
    }

    @PatchMapping("/{linkId}/permissions")
    @PreAuthorize("hasRole('TITULAR')")
    @Operation(summary = "Atualiza permissões granulares do vínculo",
               description = "Concede e/ou revoga permissões por módulo (ex: AGENDA, FOCUS, EMOTIONAL_CHECKIN). "
                           + "VAULT é estruturalmente excluído da matriz (RF-08.5). Requer papel TITULAR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Permissões atualizadas"),
        @ApiResponse(responseCode = "400", description = "Módulo inválido na lista"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Requer TITULAR dono do vínculo"),
        @ApiResponse(responseCode = "404", description = "Vínculo não encontrado")
    })
    public LinkResponse updatePermissions(
            @Parameter(description = "ID UUID do vínculo", required = true) @PathVariable String linkId,
            @Valid @RequestBody UpdatePermissionsRequest req) {
        var result = permission.updatePermissions(new UpdatePermissionsCommand(
                linkId, CurrentUser.requireId().asString(), req.grant(), req.revoke()));
        return LinkResponse.from(result);
    }

    @DeleteMapping("/{linkId}")
    @PreAuthorize("hasRole('TITULAR')")
    @Operation(summary = "Revoga o vínculo (RF-01.4)",
               description = "Revogação lógica imediata — invalida sessões do Apoio e remove acesso delegado. "
                           + "Apenas o Titular dono pode revogar.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Vínculo revogado"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado"),
        @ApiResponse(responseCode = "403", description = "Requer TITULAR dono do vínculo"),
        @ApiResponse(responseCode = "404", description = "Vínculo não encontrado")
    })
    public ResponseEntity<Void> revoke(
            @Parameter(description = "ID UUID do vínculo", required = true) @PathVariable String linkId) {
        lifecycle.revoke(new RevokeLinkCommand(linkId, CurrentUser.requireId().asString()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lista os vínculos do usuário autenticado",
               description = "Titular vê os vínculos onde é dono; Apoio vê os vínculos onde participa. "
                           + "A visão é determinada automaticamente pelo papel do token JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de vínculos"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente ou expirado")
    })
    public List<LinkResponse> list() {
        AuthenticatedUser user = CurrentUser.require();
        List<br.com.sentinelcorp.Neuro_Gen.link.application.result.LinkResult> results = user.isApoio()
                ? lifecycle.listByApoio(user.userId())
                : lifecycle.listByTitular(user.userId());
        return results.stream().map(LinkResponse::from).toList();
    }
}
