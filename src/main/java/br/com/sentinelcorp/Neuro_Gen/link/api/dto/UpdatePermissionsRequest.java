package br.com.sentinelcorp.Neuro_Gen.link.api.dto;

import java.util.Set;

/** Conjuntos de módulos a conceder e/ou revogar para um vínculo (RF-01.3). */
public record UpdatePermissionsRequest(Set<String> grant, Set<String> revoke) {
}
