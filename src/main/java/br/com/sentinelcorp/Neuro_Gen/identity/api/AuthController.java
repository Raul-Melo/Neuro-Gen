package br.com.sentinelcorp.Neuro_Gen.identity.api;

import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.LoginRequest;
import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.LogoutRequest;
import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.PasswordResetConfirmRequest;
import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.PasswordResetRequestRequest;
import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.RefreshRequest;
import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.RegisterRequest;
import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.TokenResponse;
import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.UserProfileResponse;
import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.VerifyEmailRequest;
import br.com.sentinelcorp.Neuro_Gen.identity.application.AuthenticationUseCase;
import br.com.sentinelcorp.Neuro_Gen.identity.application.PasswordResetUseCase;
import br.com.sentinelcorp.Neuro_Gen.identity.application.RegistrationUseCase;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.LoginCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.LogoutCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.RefreshTokenCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.RegisterUserCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.RequestPasswordResetCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.ResetPasswordCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.VerifyEmailCommand;
import br.com.sentinelcorp.Neuro_Gen.shared.api.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos de autenticação (RF-01.1, 01.6, 01.7). */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Identity — Autenticação", description = "Cadastro, login, refresh, verificação e reset de senha")
public class AuthController {

    private final RegistrationUseCase registration;
    private final AuthenticationUseCase authentication;
    private final PasswordResetUseCase passwordReset;

    public AuthController(RegistrationUseCase registration, AuthenticationUseCase authentication,
                          PasswordResetUseCase passwordReset) {
        this.registration = registration;
        this.authentication = authentication;
        this.passwordReset = passwordReset;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastra uma conta (Titular ou Apoio)", security = {})
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest req) {
        var result = registration.register(new RegisterUserCommand(req.email(), req.password(), req.role()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserProfileResponse.from(result));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e retorna tokens (com 2FA se habilitado)", security = {})
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        var result = authentication.login(
                new LoginCommand(req.email(), req.password(), req.totpCode(), deviceInfo(http)));
        return ResponseEntity.ok(TokenResponse.from(result));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Troca o refresh token por um novo par de tokens", security = {})
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        var result = authentication.refresh(new RefreshTokenCommand(req.refreshToken(), deviceInfo(http)));
        return ResponseEntity.ok(TokenResponse.from(result));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga o refresh token informado", security = {@SecurityRequirement(name = "bearer-jwt")})
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest req) {
        authentication.logout(new LogoutCommand(req.refreshToken()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verifica o e-mail via token", security = {})
    public ResponseEntity<UserProfileResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        var result = registration.verifyEmail(new VerifyEmailCommand(req.token()));
        return ResponseEntity.ok(UserProfileResponse.from(result));
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Solicita redefinição de senha (envia token por e-mail)", security = {})
    public ResponseEntity<MessageResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestRequest req) {
        passwordReset.requestReset(new RequestPasswordResetCommand(req.email()));
        return ResponseEntity.accepted().body(new MessageResponse(
                "Se o e-mail existir, enviaremos instruções de redefinição."));
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Confirma a redefinição de senha com o token", security = {})
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        passwordReset.reset(new ResetPasswordCommand(req.resetToken(), req.newPassword()));
        return ResponseEntity.noContent().build();
    }

    private static String deviceInfo(HttpServletRequest http) {
        String ua = http.getHeader("User-Agent");
        return (ua == null ? "unknown" : ua) + " | ip=" + http.getRemoteAddr();
    }
}
