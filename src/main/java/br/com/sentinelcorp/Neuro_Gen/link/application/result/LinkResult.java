package br.com.sentinelcorp.Neuro_Gen.link.application.result;

import br.com.sentinelcorp.Neuro_Gen.link.domain.model.ModulePermission;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.SupportLink;

import java.util.Set;
import java.util.stream.Collectors;

public record LinkResult(
        String id,
        String titularId,
        String apoioId,
        String status,
        Set<String> grantedPermissions,
        String createdAt) {

    public static LinkResult from(SupportLink link) {
        Set<String> granted = link.permissions().grantedModules().stream()
                .map(ModulePermission::name)
                .collect(Collectors.toUnmodifiableSet());
        return new LinkResult(
                link.id().asString(),
                link.titularId().asString(),
                link.apoioId() == null ? null : link.apoioId().asString(),
                link.status().name(),
                granted,
                link.audit().createdAt().toString());
    }
}
