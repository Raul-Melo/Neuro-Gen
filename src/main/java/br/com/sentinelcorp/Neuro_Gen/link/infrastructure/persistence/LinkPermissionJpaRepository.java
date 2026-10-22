package br.com.sentinelcorp.Neuro_Gen.link.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LinkPermissionJpaRepository extends JpaRepository<LinkPermissionEntity, UUID> {

    List<LinkPermissionEntity> findByLinkId(UUID linkId);

    void deleteByLinkId(UUID linkId);
}
