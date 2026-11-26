package br.com.sentinelcorp.Neuro_Gen.identity.application.result;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;

/** Projeção de saída de uma conta. NUNCA expõe hash de senha, segredo TOTP ou tokens internos. */
public record UserResult(
        String id,
        String email,
        String role,
        String status,
        boolean emailVerified,
        boolean totpEnabled,
        boolean hasPendingTotpSetup,
        String cpf,
        String birthDate,
        String createdAt) {

    public static UserResult from(User user) {
        return new UserResult(
                user.id().asString(),
                user.email().value(),
                user.role().name(),
                user.status().name(),
                user.emailVerified(),
                user.totpEnabled(),
                user.hasPendingTotpSetup(),
                user.cpf() == null ? null : user.cpf().masked(),
                user.birthDate() == null ? null : user.birthDate().toString(),
                user.audit().createdAt().toString());
    }
}
