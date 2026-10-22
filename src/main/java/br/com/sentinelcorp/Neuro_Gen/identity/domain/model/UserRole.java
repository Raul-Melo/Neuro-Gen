package br.com.sentinelcorp.Neuro_Gen.identity.domain.model;

/** Papel da conta. Determina o acesso estrutural (ex.: APOIO nunca acessa o Vault — RF-08.5). */
public enum UserRole {
    /** Dono dos dados — cria vínculos e concede permissões. */
    TITULAR,
    /** Acessa dados do Titular conforme permissões concedidas. */
    APOIO
}
