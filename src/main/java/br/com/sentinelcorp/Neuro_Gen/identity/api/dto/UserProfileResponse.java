package br.com.sentinelcorp.Neuro_Gen.identity.api.dto;

import br.com.sentinelcorp.Neuro_Gen.identity.application.result.UserResult;

public record UserProfileResponse(
        String id,
        String email,
        String role,
        String status,
        boolean emailVerified,
        boolean totpEnabled,
        String createdAt) {

    public static UserProfileResponse from(UserResult r) {
        return new UserProfileResponse(r.id(), r.email(), r.role(), r.status(),
                r.emailVerified(), r.totpEnabled(), r.createdAt());
    }
}
