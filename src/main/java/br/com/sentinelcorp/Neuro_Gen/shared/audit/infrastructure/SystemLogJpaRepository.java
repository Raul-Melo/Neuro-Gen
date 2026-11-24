package br.com.sentinelcorp.Neuro_Gen.shared.audit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SystemLogJpaRepository extends JpaRepository<SystemLogEntity, UUID> {}
