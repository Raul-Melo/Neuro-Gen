package br.com.sentinelcorp.Neuro_Gen.link.application;

import br.com.sentinelcorp.Neuro_Gen.link.domain.exception.InsufficientPermissionException;
import br.com.sentinelcorp.Neuro_Gen.link.domain.model.ModulePermission;
import br.com.sentinelcorp.Neuro_Gen.link.domain.port.SupportLinkRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.application.SupportAccessPolicy;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação da porta compartilhada {@link SupportAccessPolicy}. É aqui que outros módulos
 * (ex.: Emotional) verificam, sem acoplamento ao Link, se um Apoio pode acessar dados de um Titular.
 */
@Service
public class SupportAccessPolicyImpl implements SupportAccessPolicy {

    private final SupportLinkRepository links;

    public SupportAccessPolicyImpl(SupportLinkRepository links) {
        this.links = links;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasModuleAccess(UserId apoio, UserId titular, String moduleName) {
        ModulePermission module;
        try {
            module = ModulePermission.valueOf(moduleName);
        } catch (IllegalArgumentException ex) {
            return false; // módulo não liberável (ex.: VAULT) nunca concede acesso
        }
        return links.findActiveBetween(apoio, titular)
                .map(link -> link.hasPermission(module))
                .orElse(false);
    }

    @Override
    public void requireModuleAccess(UserId requester, UserId owner, String moduleName) {
        if (requester.equals(owner)) {
            return; // o próprio dono sempre acessa seus dados
        }
        if (!hasModuleAccess(requester, owner, moduleName)) {
            throw new InsufficientPermissionException(moduleName);
        }
    }
}
