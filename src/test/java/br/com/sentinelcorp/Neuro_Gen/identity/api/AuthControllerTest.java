package br.com.sentinelcorp.Neuro_Gen.identity.api;

import br.com.sentinelcorp.Neuro_Gen.identity.application.AuthenticationUseCase;
import br.com.sentinelcorp.Neuro_Gen.identity.application.PasswordResetUseCase;
import br.com.sentinelcorp.Neuro_Gen.identity.application.RegistrationUseCase;
import br.com.sentinelcorp.Neuro_Gen.identity.application.result.AuthResult;
import br.com.sentinelcorp.Neuro_Gen.identity.application.result.UserResult;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.InvalidCredentialsException;
import br.com.sentinelcorp.Neuro_Gen.shared.api.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de contrato HTTP do {@link AuthController} em modo standalone (sem contexto Spring/segurança):
 * foca no mapeamento request→command→response e na tradução de exceções pelo {@link GlobalExceptionHandler}.
 */
class AuthControllerTest {

    private final RegistrationUseCase registration = mock(RegistrationUseCase.class);
    private final AuthenticationUseCase authentication = mock(AuthenticationUseCase.class);
    private final PasswordResetUseCase passwordReset = mock(PasswordResetUseCase.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(registration, authentication, passwordReset))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerReturns201() throws Exception {
        when(registration.register(any())).thenReturn(new UserResult(
                "id-1", "user@example.com", "TITULAR", "PENDING_VERIFICATION", false, false, "2026-01-01T00:00:00Z"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"senha123","role":"TITULAR"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-1"))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void registerRejectsInvalidEmailWith400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-email","password":"senha123","role":"TITULAR"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void loginInvalidCredentialsReturns401() throws Exception {
        when(authentication.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"wrong"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
