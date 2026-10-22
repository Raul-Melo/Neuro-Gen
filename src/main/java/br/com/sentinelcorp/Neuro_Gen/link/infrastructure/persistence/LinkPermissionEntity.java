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

/** Permissão granular de um vínculo (tabela {@code ng_core.ng_link_permissions}). */
@Entity
@Table(name = "ng_link_permissions", schema = "ng_core")
@Getter
@Setter
@NoArgsConstructor
public class LinkPermissionEntity {

    @Id
    private UUID id;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(name = "module_name", nullable = false)
    private String moduleName;

    @Column(name = "granted_by", nullable = false)
    private UUID grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;
}
