package br.com.sentinelcorp.Neuro_Gen.link.application;

import br.com.sentinelcorp.Neuro_Gen.link.application.command.LinkCommands.UpdatePermissionsCommand;
import br.com.sentinelcorp.Neuro_Gen.link.application.result.LinkResult;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.LinkId;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.ModulePermission;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.SupportLink;
import br.com.sentinelcorp.Neuro_Gen.link.domain.port.SupportLinkRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ValidationException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Use case de atualização granular de permissões por vínculo (RF-01.3). Apenas o Titular dono do
 * vínculo pode alterar. Tentar conceder VAULT resulta em erro de validação (enum não o contém).
 */
@Service
public class PermissionUseCase {

    private final SupportLinkRepository links;

    public PermissionUseCase(SupportLinkRepository links) {
        this.links = links;
    }

    @Transactional
    public LinkResult updatePermissions(UpdatePermissionsCommand cmd) {
        SupportLink link = links.findById(LinkId.of(cmd.linkId()))
                .orElseThrow(() -> new NotFoundException("Vínculo", cmd.linkId()));

        if (!link.titularId().equals(UserId.of(cmd.titularId()))) {
            throw new ForbiddenAccessException("Vínculo", "apenas o Titular dono pode alterar permissões");
        }
        safe(cmd.modulesToGrant()).forEach(m -> link.grantPermission(parse(m)));
        safe(cmd.modulesToRevoke()).forEach(m -> link.revokePermission(parse(m)));
        links.save(link);
        return LinkResult.from(link);
    }

    private static Set<String> safe(Set<String> set) {
        return set == null ? Set.of() : set;
    }

    private static ModulePermission parse(String module) {
        try {
            return ModulePermission.valueOf(module);
        } catch (IllegalArgumentException | NullPointerException ex) {
            // Inclui a tentativa de conceder "VAULT" — propositalmente ausente do enum (RF-08.5).
            throw new ValidationException(Map.of("module", "módulo inválido ou não liberável: " + module));
        }
    }
}
