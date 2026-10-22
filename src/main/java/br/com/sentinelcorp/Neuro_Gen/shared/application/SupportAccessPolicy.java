package br.com.sentinelcorp.Neuro_Gen.shared.application;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

/**
 * Porta (contrato) de verificação de acesso de uma conta <b>Apoio</b> a um módulo de dados de um
 * <b>Titular</b>, consultada por módulos de feature (ex.: Emotional) sem importar o módulo Link —
 * a implementação vive no Link, respeitando o isolamento de bounded contexts (ADR-001).
 *
 * <p>Regra de ouro: ausência de permissão explícita = acesso negado (opt-in, RF-01.3).
 * O módulo Vault NÃO passa por aqui — seu acesso por Apoio é negado de forma estrutural (RF-08.5).</p>
 */
public interface SupportAccessPolicy {

    /** @return true se o Apoio possui a permissão de módulo concedida pelo Titular num vínculo ativo. */
    boolean hasModuleAccess(UserId apoio, UserId titular, String moduleName);

    /**
     * Lança {@code ForbiddenAccessException} se o Apoio não possui a permissão. No-op se o próprio
     * Titular acessa seus dados (requester == owner).
     */
    void requireModuleAccess(UserId requester, UserId owner, String moduleName);
}
