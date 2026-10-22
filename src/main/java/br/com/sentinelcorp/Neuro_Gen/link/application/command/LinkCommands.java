package br.com.sentinelcorp.Neuro_Gen.link.application.command;

import java.util.Set;

/** Commands imutáveis do módulo Link. */
public final class LinkCommands {

    private LinkCommands() {
    }

    public record CreateInviteCommand(String titularId) {
    }

    public record AcceptInviteCommand(String rawInviteToken, String apoioId) {
    }

    public record UpdatePermissionsCommand(String linkId, String titularId,
                                           Set<String> modulesToGrant, Set<String> modulesToRevoke) {
    }

    public record RevokeLinkCommand(String linkId, String titularId) {
    }
}
