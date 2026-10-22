package br.com.sentinelcorp.Neuro_Gen.vault.domain.model;

/** Dica visual para o cliente (ícone/campos). O conteúdo real é opaco para o servidor. */
public enum VaultEntryType {
    LOGIN_CREDENTIAL,
    SECURE_NOTE,
    CREDIT_CARD,
    API_KEY,
    CUSTOM
}
