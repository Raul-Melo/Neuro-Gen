package br.com.sentinelcorp.Neuro_Gen.identity.api;

import br.com.sentinelcorp.Neuro_Gen.identity.application.RegistrationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints auxiliares de desenvolvimento — NÃO devem ser expostos em produção.
 * Permitem que o frontend obtenha tokens transacionais sem depender de e-mail.
 */
@RestController
@RequestMapping("/api/v1/dev")
@Tag(name = "Dev", description = "Utilitários de desenvolvimento (desabilitar em produção)")
public class DevController {

    private final RegistrationUseCase registration;

    public DevController(RegistrationUseCase registration) {
        this.registration = registration;
    }

    @GetMapping("/verification-token")
    @Operation(
        summary = "Obter token de verificação de e-mail (dev only)",
        description = "Reemite e retorna o token de verificação para o e-mail informado. "
                    + "Use esse token em POST /api/v1/auth/verify-email. "
                    + "**Remover antes de ir para produção.**"
    )
    public ResponseEntity<Map<String, String>> getVerificationToken(@RequestParam String email) {
        String token = registration.issueVerificationTokenForDev(email);
        return ResponseEntity.ok(Map.of(
            "email", email,
            "verificationToken", token,
            "usage", "POST /api/v1/auth/verify-email com body {\"token\": \"<verificationToken>\"}"
        ));
    }
}
