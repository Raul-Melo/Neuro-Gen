package br.com.sentinelcorp.Neuro_Gen.identity.api;

import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.TotpCodeRequest;
import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.TotpSetupResponse;
import br.com.sentinelcorp.Neuro_Gen.identity.api.dto.UserProfileResponse;
import br.com.sentinelcorp.Neuro_Gen.identity.application.TotpUseCase;
import br.com.sentinelcorp.Neuro_Gen.identity.application.UserProfileUseCase;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.ConfirmTotpCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.DisableTotpCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.EnableTotpCommand;
import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Conta do usuário autenticado: perfil e gestão de 2FA (RF-01.9). */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Identity — Conta", description = "Perfil e 2FA do usuário autenticado")
public class UserController {

    private final UserProfileUseCase profile;
    private final TotpUseCase totp;

    public UserController(UserProfileUseCase profile, TotpUseCase totp) {
        this.profile = profile;
        this.totp = totp;
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o perfil da conta autenticada")
    public UserProfileResponse me() {
        return UserProfileResponse.from(profile.getProfile(CurrentUser.requireId()));
    }

    @PostMapping("/me/totp")
    @Operation(summary = "Inicia o setup de 2FA — gera o segredo pendente (não o expõe aqui)",
               description = "Após este endpoint, chame GET /me/totp/setup UMA ÚNICA VEZ para obter o segredo.")
    public ResponseEntity<Void> enableTotp() {
        totp.enable(new EnableTotpCommand(CurrentUser.requireId().asString()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/totp/setup")
    @Operation(summary = "Retorna o segredo TOTP UMA ÚNICA VEZ para configurar o autenticador",
               description = "Exibe o base32 e o otpAuthUri apenas na primeira chamada. "
                       + "Chamadas subsequentes retornam erro 422. "
                       + "Para gerar um novo segredo, chame POST /me/totp novamente.")
    public TotpSetupResponse getTotpSetupOnce() {
        return TotpSetupResponse.from(totp.getSetupOnce(CurrentUser.requireId().asString()));
    }

    @PostMapping("/me/totp/confirm")
    @Operation(summary = "Confirma o 2FA com um código válido e ativa a exigência no login")
    public ResponseEntity<Void> confirmTotp(@Valid @RequestBody TotpCodeRequest req) {
        totp.confirm(new ConfirmTotpCommand(CurrentUser.requireId().asString(), req.code()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/totp")
    @Operation(summary = "Desativa o 2FA (exige código válido)")
    public ResponseEntity<Void> disableTotp(@Valid @RequestBody TotpCodeRequest req) {
        totp.disable(new DisableTotpCommand(CurrentUser.requireId().asString(), req.code()));
        return ResponseEntity.noContent().build();
    }
}
