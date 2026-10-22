package br.com.sentinelcorp.Neuro_Gen.link.infrastructure.persistence;

import br.com.sentinelcorp.Neuro_Gen.link.domain.model.InviteToken;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.LinkId;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.LinkStatus;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.ModulePermission;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.PermissionMatrix;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.SupportLink;
import br.com.sentinelcorp.Neuro_Gen.link.domain.port.SupportLinkRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.AuditMetadata;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adaptador de persistência do vínculo + suas permissões granulares. */
@Repository
public class SupportLinkRepositoryAdapter implements SupportLinkRepository {

    private static final List<String> OPEN_STATUSES = List.of("PENDING", "ACTIVE");

    private final SupportLinkJpaRepository linkJpa;
    private final LinkPermissionJpaRepository permissionJpa;

    public SupportLinkRepositoryAdapter(SupportLinkJpaRepository linkJpa,
                                        LinkPermissionJpaRepository permissionJpa) {
        this.linkJpa = linkJpa;
        this.permissionJpa = permissionJpa;
    }

    @Override
    public SupportLink save(SupportLink link) {
        SupportLinkEntity entity = linkJpa.findById(link.id().value()).orElse(null);
        boolean isNew = entity == null;
        if (isNew) {
            entity = new SupportLinkEntity();
            entity.setId(link.id().value());
            entity.setInviteTokenHash(link.inviteToken().toHash());
            entity.setCreatedAt(link.audit().createdAt());
        }
        entity.setTitularId(link.titularId().value());
        entity.setApoioId(link.apoioId() == null ? null : link.apoioId().value());
        entity.setInviteExpiresAt(link.inviteToken().expiresAt());
        entity.setStatus(link.status().name());
        entity.setAcceptedAt(link.status() == LinkStatus.ACTIVE
                ? (entity.getAcceptedAt() == null ? Instant.now() : entity.getAcceptedAt()) : entity.getAcceptedAt());
        entity.setRevokedAt(link.status() == LinkStatus.REVOKED
                ? (entity.getRevokedAt() == null ? Instant.now() : entity.getRevokedAt()) : entity.getRevokedAt());
        entity.setUpdatedAt(link.audit().updatedAt());
        linkJpa.save(entity);
        syncPermissions(link);
        return link;
    }

    private void syncPermissions(SupportLink link) {
        permissionJpa.deleteByLinkId(link.id().value());
        for (ModulePermission module : link.permissions().grantedModules()) {
            LinkPermissionEntity p = new LinkPermissionEntity();
            p.setId(UUID.randomUUID());
            p.setLinkId(link.id().value());
            p.setModuleName(module.name());
            p.setGrantedBy(link.titularId().value());
            p.setGrantedAt(Instant.now());
            permissionJpa.save(p);
        }
    }

    @Override
    public Optional<SupportLink> findById(LinkId id) {
        return linkJpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<SupportLink> findByTokenHash(String tokenHash) {
        return linkJpa.findByInviteTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public List<SupportLink> findActiveByTitularId(UserId titularId) {
        return linkJpa.findByTitularIdAndStatusIn(titularId.value(), OPEN_STATUSES)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<SupportLink> findActiveByApoioId(UserId apoioId) {
        return linkJpa.findByApoioIdAndStatus(apoioId.value(), "ACTIVE")
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<SupportLink> findActiveBetween(UserId apoioId, UserId titularId) {
        return linkJpa.findByApoioIdAndTitularIdAndStatus(apoioId.value(), titularId.value(), "ACTIVE")
                .map(this::toDomain);
    }

    private SupportLink toDomain(SupportLinkEntity e) {
        Set<ModulePermission> modules = permissionJpa.findByLinkId(e.getId()).stream()
                .map(p -> ModulePermission.valueOf(p.getModuleName()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ModulePermission.class)));
        return SupportLink.reconstitute(
                new LinkId(e.getId()),
                new UserId(e.getTitularId()),
                e.getApoioId() == null ? null : new UserId(e.getApoioId()),
                InviteToken.fromPersistence(e.getInviteExpiresAt()),
                PermissionMatrix.of(modules),
                LinkStatus.valueOf(e.getStatus()),
                new AuditMetadata(e.getCreatedAt(), e.getUpdatedAt(), e.getTitularId().toString()));
    }
}
