package br.com.sentinelcorp.Neuro_Gen.identity.application.command;

/**
 * Commands imutáveis (entrada dos use cases do módulo Identity). Agrupados num único arquivo por
 * serem records pequenos e coesos; cada um é mapeado a partir de um Request DTO pelo controller.
 */
public final class IdentityCommands {

    private IdentityCommands() {
    }

    public record RegisterUserCommand(String email, String rawPassword, String role) {
    }

    public record VerifyEmailCommand(String token) {
    }

    public record LoginCommand(String email, String rawPassword, String totpCode, String deviceInfo) {
    }

    public record RefreshTokenCommand(String refreshToken, String deviceInfo) {
    }

    public record LogoutCommand(String refreshToken) {
    }

    public record RequestPasswordResetCommand(String email) {
    }

    public record ResetPasswordCommand(String resetToken, String newRawPassword) {
    }

    public record EnableTotpCommand(String userId) {
    }

    public record ConfirmTotpCommand(String userId, String code) {
    }

    public record DisableTotpCommand(String userId, String code) {
    }
}
