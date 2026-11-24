package br.com.sentinelcorp.Neuro_Gen.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita @Async para o AuditService (gravação assíncrona de logs). */
@Configuration
@EnableAsync
public class AsyncConfig {}
