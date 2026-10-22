package br.com.sentinelcorp.Neuro_Gen.link.api.dto;

import br.com.sentinelcorp.Neuro_Gen.link.application.result.LinkResult;

import java.util.Set;

public record LinkResponse(
        String id,
        String titularId,
        String apoioId,
        String status,
        Set<String> permissions,
        String linkedAt) {

    public static LinkResponse from(LinkResult r) {
        return new LinkResponse(r.id(), r.titularId(), r.apoioId(), r.status(),
                r.grantedPermissions(), r.createdAt());
    }
}
