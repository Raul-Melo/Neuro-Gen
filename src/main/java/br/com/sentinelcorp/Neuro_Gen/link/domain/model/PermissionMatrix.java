package br.com.sentinelcorp.Neuro_Gen.link.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Conjunto imutável de módulos liberados a um Apoio. Cada {@link #grant}/{@link #revoke} retorna uma
 * NOVA instância — fonte única de verdade do que o Apoio pode acessar.
 */
public final class PermissionMatrix {

    private final Set<ModulePermission> granted;

    private PermissionMatrix(Set<ModulePermission> granted) {
        this.granted = granted.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(granted));
    }

    public static PermissionMatrix empty() {
        return new PermissionMatrix(EnumSet.noneOf(ModulePermission.class));
    }

    public static PermissionMatrix of(Set<ModulePermission> modules) {
        return new PermissionMatrix(modules);
    }

    public PermissionMatrix grant(ModulePermission module) {
        EnumSet<ModulePermission> copy = emptyOrCopy();
        copy.add(module);
        return new PermissionMatrix(copy);
    }

    public PermissionMatrix revoke(ModulePermission module) {
        EnumSet<ModulePermission> copy = emptyOrCopy();
        copy.remove(module);
        return new PermissionMatrix(copy);
    }

    public boolean has(ModulePermission module) {
        return granted.contains(module);
    }

    public Set<ModulePermission> grantedModules() {
        return granted;
    }

    private EnumSet<ModulePermission> emptyOrCopy() {
        return granted.isEmpty() ? EnumSet.noneOf(ModulePermission.class) : EnumSet.copyOf(granted);
    }
}
