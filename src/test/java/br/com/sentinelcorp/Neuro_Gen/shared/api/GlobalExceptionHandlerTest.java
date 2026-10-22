package br.com.sentinelcorp.Neuro_Gen.shared.api;

import br.com.sentinelcorp.Neuro_Gen.agenda.api.EventController;
import br.com.sentinelcorp.Neuro_Gen.agenda.api.TaskController;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.EventUseCase;
import br.com.sentinelcorp.Neuro_Gen.agenda.application.TaskUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Erros de requisição do cliente devem virar 4xx com corpo {@link ErrorResponse} — e não 500.
 *
 * <p>O handler genérico {@code @ExceptionHandler(Exception.class)} capturava exceções que o Spring
 * MVC normalmente traduz para 400/404/405/415 (JSON malformado, parâmetro ausente, rota inexistente
 * etc.), devolvendo "500 Internal Server Error" para erros que são do cliente.</p>
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    /** Controller auxiliar apenas para simular situações difíceis de reproduzir em modo standalone. */
    @RestController
    static class FakeController {

        @GetMapping("/teste/recurso-inexistente")
        void recursoInexistente() throws NoResourceFoundException {
            // É o que o Spring lança em produção para rotas sem controller (ex.: /api/v1/nao-existe).
            throw new NoResourceFoundException(HttpMethod.GET, "api/v1/nao-existe");
        }

        @GetMapping("/teste/falha-inesperada")
        void falhaInesperada() {
            throw new IllegalStateException("detalhe interno sensivel");
        }
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TaskController(mock(TaskUseCase.class)),
                        new EventController(mock(EventUseCase.class)),
                        new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/v1/tasks"));
    }

    @Test
    void parametroObrigatorioAusenteRetorna400ComNomeDoCampo() throws Exception {
        mockMvc.perform(get("/api/v1/events").param("to", "2026-10-31T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors.from").exists());
    }

    @Test
    void parametroComTipoInvalidoRetorna400ComNomeDoCampo() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .param("from", "ontem")
                        .param("to", "2026-10-31T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors.from").exists());
    }

    @Test
    void metodoHttpNaoSuportadoRetorna405ComCabecalhoAllow() throws Exception {
        mockMvc.perform(patch("/api/v1/tasks"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void contentTypeNaoSuportadoRetorna415() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("titulo"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void rotaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/teste/recurso-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    /** Caracterização: erro realmente inesperado continua 500 e não vaza detalhes internos. */
    @Test
    void erroInesperadoContinua500SemVazarDetalhes() throws Exception {
        mockMvc.perform(get("/teste/falha-inesperada"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message", not(containsString("sensivel"))));
    }
}
