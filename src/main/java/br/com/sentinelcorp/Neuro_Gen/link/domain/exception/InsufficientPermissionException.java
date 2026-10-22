package br.com.sentinelcorp.Neuro_Gen.link.domain.exception;

import br.com.sentinelcorp.Neuro_Gen.link.domain.model.ModulePermission;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.ForbiddenAccessException;

/** HTTP 403 — Apoio tenta acessar módulo não liberado pelo Titular. */
public class InsufficientPermissionException extends ForbiddenAccessException {
    public InsufficientPermissionException(ModulePermission requiredModule) {
        super(requiredModule.name(), "permissão não concedida pelo Titular");
    }

    public InsufficientPermissionException(String module) {
        super(module, "permissão não concedida pelo Titular");
    }
}
