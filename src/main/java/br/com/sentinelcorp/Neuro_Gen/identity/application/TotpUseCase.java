package br.com.sentinelcorp.Neuro_Gen.identity.application;

import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.ConfirmTotpCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.DisableTotpCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.command.IdentityCommands.EnableTotpCommand;
import br.com.sentinelcorp.Neuro_Gen.identity.application.result.TotpSetupResult;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.exception.InvalidTotpCodeException;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.TotpService;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.UserRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case de 2FA (RF-01.9). {@code enable} gera e persiste o segredo e devolve o URI otpauth;
 * {@code confirm} valida um código (sanity-check do setup); {@code disable} exige código válido.
 */
@Service
public class TotpUseCase {

    private final UserRepository users;
    private final TotpService totpService;

    public TotpUseCase(UserRepository users, TotpService totpService) {
        this.users = users;
        this.totpService = totpService;
    }

    @Transactional
    public TotpSetupResult enable(EnableTotpCommand cmd) {
        User user = load(cmd.userId());
        TotpService.GeneratedTotp generated = totpService.generate(user.email().value());
        user.enableTotp(generated.secret());
        users.save(user);
        return new TotpSetupResult(generated.otpAuthUri(), generated.base32Plain());
    }

    @Transactional
    public void confirm(ConfirmTotpCommand cmd) {
        User user = load(cmd.userId());
        if (!user.requiresTotp() || !totpService.verify(user.totpSecret(), cmd.code())) {
            throw new InvalidTotpCodeException();
        }
    }

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
        UserId id = UserId.of(userId);
        return users.findById(id).orElseThrow(() -> new NotFoundException("Usuário", userId));
    }
}
