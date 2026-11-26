package br.com.sentinelcorp.Neuro_Gen.identity.application;

import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.ConfirmTotpCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.DisableTotpCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.EnableTotpCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.result.TotpSetupResult;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.InvalidTotpCodeException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.TotpService;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.TotpSetupStatePort;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.UserRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.UnprocessableEntityException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case de 2FA (RF-01.9).
 *
 * Fluxo:
 * 1. {@code enable} — gera e persiste o segredo pendente (setup_shown = false, confirmed = false).
 * 2. {@code getSetupOnce} — retorna base32/otpAuthUri UMA ÚNICA VEZ (via GET), marca setup_shown = true.
 * 3. {@code confirm} — valida o código e ativa o 2FA (totpConfirmed = true).
 * 4. {@code disable} — exige código válido e remove o segredo.
 */
@Service
public class TotpUseCase {

    private final UserRepository users;
    private final TotpService totpService;
    private final TotpSetupStatePort setupState;

    public TotpUseCase(UserRepository users, TotpService totpService, TotpSetupStatePort setupState) {
        this.users = users;
        this.totpService = totpService;
        this.setupState = setupState;
    }

    /**
     * Gera novo segredo pendente. Não retorna o segredo — use {@link #getSetupOnce} para obtê-lo.
     * Se o 2FA já estiver ativo ({@code totpConfirmed = true}), recusa a operação: o usuário deve
     * primeiro desativar com código válido via DELETE /me/totp. Isso impede que um token roubado
     * seja usado para fazer downgrade silencioso do 2FA.
     */
    @Transactional
    public void enable(EnableTotpCommand cmd) {
        User user = load(cmd.userId());
        if (user.requiresTotp()) {
            throw new UnprocessableEntityException("TOTP_ALREADY_ENABLED",
                    "O 2FA já está ativo. Para gerar um novo segredo, desative primeiro via DELETE /api/v1/users/me/totp.");
        }
        TotpService.GeneratedTotp generated = totpService.generate(user.email().value());
        user.enableTotp(generated.secret());
        users.save(user);
        setupState.resetSetupShown(UserId.of(cmd.userId()));
    }

    /**
     * Retorna o base32 e otpAuthUri UMA ÚNICA VEZ.
     * Após a primeira chamada, lança {@link UnprocessableEntityException} com código TOTP_SETUP_ALREADY_SHOWN.
     */
    @Transactional
    public TotpSetupResult getSetupOnce(String userId) {
        User user = load(userId);
        if (user.totpSecret() == null) {
            throw new NotFoundException("Setup TOTP", "nenhum segredo pendente — chame POST /me/totp primeiro");
        }
        String encryptedSecret = setupState.findEncryptedSecretIfNotShown(UserId.of(userId))
                .orElseThrow(() -> new UnprocessableEntityException(
                        "TOTP_SETUP_ALREADY_SHOWN",
                        "O código de setup já foi exibido e não pode ser recuperado novamente. "
                        + "Para gerar um novo segredo, chame POST /api/v1/users/me/totp."));
        setupState.markSetupShown(UserId.of(userId));
        String base32 = totpService.decryptBase32(user.totpSecret());
        String otpAuthUri = totpService.buildOtpAuthUri(user.email().value(), base32);
        return new TotpSetupResult(otpAuthUri, base32);
    }

    /** Confirma o código e ativa o 2FA. A partir daqui o login vai exigir código TOTP. */
    @Transactional
    public void confirm(ConfirmTotpCommand cmd) {
        User user = load(cmd.userId());
        if (user.totpSecret() == null || !totpService.verify(user.totpSecret(), cmd.code())) {
            throw new InvalidTotpCodeException();
        }
        user.confirmTotp();
        users.save(user);
    }

    /** Desativa o 2FA — exige código válido para evitar deleção não autorizada. */
    @Transactional
    public void disable(DisableTotpCommand cmd) {
        User user = load(cmd.userId());
        if (user.requiresTotp() && !totpService.verify(user.totpSecret(), cmd.code())) {
            throw new InvalidTotpCodeException();
        }
        user.disableTotp();
        users.save(user);
    }

    private User load(String userId) {
        return users.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("Usuário", userId));
    }
}
