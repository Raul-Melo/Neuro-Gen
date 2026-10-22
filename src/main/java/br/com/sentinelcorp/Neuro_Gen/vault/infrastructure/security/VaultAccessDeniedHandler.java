package br.com.sentinelcorp.Neuro_Gen.vault.infrastructure.security;

import br.com.sentinelcorp.Neuro_Gen.shared.api.ErrorResponse;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import br.com.sentinelcorp.Neuro_Gen.shared.security.Tokens;
import br.com.sentinelcorp.Neuro_Gen.vault.domain.port.VaultAuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Trata acesso negado na cadeia do Vault. Quando um usuário autenticado (tipicamente APOIO) é
 * bloqueado, registra a tentativa na auditoria de segurança (RF-08.5 / RNF-SEC.8) e responde 403
 * padronizado — a negação é estrutural (cadeia de segurança), não apenas ocultada na UI.
 */
@Component
public class VaultAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(VaultAccessDeniedHandler.class);

    private final VaultAuditLogger auditLogger;
    private final ObjectMapper objectMapper;

    public VaultAccessDeniedHandler(VaultAuditLogger auditLogger, ObjectMapper objectMapper) {
        this.auditLogger = auditLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       org.springframework.security.access.AccessDeniedException accessDeniedException) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            try {
                String ua = request.getHeader("User-Agent");
                auditLogger.log(UserId.of(jwt.getSubject()), "ACCESS_DENIED_APOIO",
                        request.getRemoteAddr(), ua == null ? null : Tokens.sha256Hex(ua),
                        false, "role=" + jwt.getClaimAsString("role"));
                log.warn("Tentativa de acesso ao Cofre NEGADA para userId={} role={} path={}",
                        jwt.getSubject(), jwt.getClaimAsString("role"), request.getRequestURI());
            } catch (Exception ex) {
                log.error("Falha ao auditar acesso negado ao Cofre", ex);
            }
        }
        writeForbidden(request, response);
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(), "VAULT_ACCESS_DENIED",
                "Contas de Apoio não têm acesso ao Cofre", request.getRequestURI(), Map.of());
        try {
            objectMapper.writeValue(response.getWriter(), body);
        } catch (Exception ex) {
            log.error("Falha ao escrever resposta 403 do Cofre", ex);
        }
    }
}
