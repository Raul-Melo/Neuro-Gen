package br.com.sentinelcorp.Neuro_Gen.link.domain.port;

import br.com.sentinelcorp.Neuro_Gen.link.domain.model.LinkId;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.SupportLink;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;

import java.util.List;
import java.util.Optional;

/** Porta de saída do agregado {@code SupportLink}. */
public interface SupportLinkRepository {

    SupportLink save(SupportLink link);

    Optional<SupportLink> findById(LinkId id);

    Optional<SupportLink> findByTokenHash(String tokenHash);

    List<SupportLink> findActiveByTitularId(UserId titularId);

    List<SupportLink> findActiveByApoioId(UserId apoioId);

    /** Busca um vínculo ativo específico entre Apoio e Titular (checagem de permissão). */
    Optional<SupportLink> findActiveBetween(UserId apoioId, UserId titularId);
}
