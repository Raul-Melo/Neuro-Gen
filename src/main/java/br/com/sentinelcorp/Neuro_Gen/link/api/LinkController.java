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

/** Vínculo Titular/Apoio: convite, aceite, permissões granulares e revogação (EPIC-01). */
@RestController
@RequestMapping("/api/v1/links")
@Tag(name = "Link — Vínculo Titular/Apoio", description = "Convites, permissões granulares e revogação")
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
    @Operation(summary = "Cria um convite de vínculo (apenas Titular)")
    public ResponseEntity<InviteResponse> createInvite() {
        var result = invite.create(new CreateInviteCommand(CurrentUser.requireId().asString()));
        return ResponseEntity.status(HttpStatus.CREATED).body(InviteResponse.from(result));
    }

    @PostMapping("/accept")
    @Operation(summary = "Aceita um convite de vínculo (conta Apoio)")
    public LinkResponse accept(@Valid @RequestBody AcceptInviteRequest req) {
        var result = invite.accept(new AcceptInviteCommand(req.inviteToken(), CurrentUser.requireId().asString()));
        return LinkResponse.from(result);
    }

    @PatchMapping("/{linkId}/permissions")
    @PreAuthorize("hasRole('TITULAR')")
    @Operation(summary = "Atualiza permissões granulares do vínculo (apenas Titular dono)")
    public LinkResponse updatePermissions(@PathVariable String linkId,
                                          @Valid @RequestBody UpdatePermissionsRequest req) {
        var result = permission.updatePermissions(new UpdatePermissionsCommand(
                linkId, CurrentUser.requireId().asString(), req.grant(), req.revoke()));
        return LinkResponse.from(result);
    }

    @DeleteMapping("/{linkId}")
    @PreAuthorize("hasRole('TITULAR')")
    @Operation(summary = "Revoga o vínculo — efeito imediato, invalida sessões do Apoio (RF-01.4)")
    public ResponseEntity<Void> revoke(@PathVariable String linkId) {
        lifecycle.revoke(new RevokeLinkCommand(linkId, CurrentUser.requireId().asString()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lista os vínculos do usuário autenticado (visão por papel)")
    public List<LinkResponse> list() {
        AuthenticatedUser user = CurrentUser.require();
        List<br.com.sentinelcorp.Neuro_Gen.link.application.result.LinkResult> results = user.isApoio()
                ? lifecycle.listByApoio(user.userId())
                : lifecycle.listByTitular(user.userId());
        return results.stream().map(LinkResponse::from).toList();
    }
}
