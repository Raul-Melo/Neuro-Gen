package br.com.sentinelcorp.Neuro_Gen.link.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportLinkJpaRepository extends JpaRepository<SupportLinkEntity, UUID> {

    Optional<SupportLinkEntity> findByInviteTokenHash(String inviteTokenHash);

    List<SupportLinkEntity> findByTitularIdAndStatusIn(UUID titularId, List<String> statuses);

    List<SupportLinkEntity> findByApoioIdAndStatus(UUID apoioId, String status);

    Optional<SupportLinkEntity> findByApoioIdAndTitularIdAndStatus(UUID apoioId, UUID titularId, String status);
}
