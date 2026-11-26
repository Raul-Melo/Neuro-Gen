package br.com.sentinelcorp.Neuro_Gen.identity.domain.port;

import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.util.Optional;

/** Porta de saída para gerenciar o estado do setup de 2FA (exibição única do segredo). */
public interface TotpSetupStatePort {

    /** Retorna o segredo cifrado se o setup ainda não foi exibido; vazio se já foi exibido. */
    Optional<String> findEncryptedSecretIfNotShown(UserId userId);

    /** Marca o setup como exibido (chamado após retornar o segredo ao usuário pela primeira vez). */
    void markSetupShown(UserId userId);

    /** Reinicia o flag (chamado quando um novo segredo é gerado via enable). */
    void resetSetupShown(UserId userId);
}
