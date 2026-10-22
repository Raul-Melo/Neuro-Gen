package br.com.sentinelcorp.Neuro_Gen.link.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Vínculo Titular/Apoio (tabela {@code ng_core.ng_support_links}). */
@Entity
@Table(name = "ng_support_links", schema = "ng_core")
@Getter
@Setter
@NoArgsConstructor
public class SupportLinkEntity {

    @Id
    private UUID id;

    @Column(name = "titular_id", nullable = false)
    private UUID titularId;

    @Column(name = "apoio_id")
    private UUID apoioId;

    @Column(name = "invite_token_hash", nullable = false, unique = true)
    private String inviteTokenHash;

    @Column(name = "invite_expires_at", nullable = false)
    private Instant inviteExpiresAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
