package br.com.sentinelcorp.Neuro_Gen.identity.application;

import br.com.sentinelcorp.Neuro_Gen.identity.application.result.UserResult;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.UserRepository;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.exception.NotFoundException;
import br.com.sentinelcorp.Neuro_Gen.shared.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use case de leitura do perfil da conta autenticada. */
@Service
public class UserProfileUseCase {

    private final UserRepository users;

    public UserProfileUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResult getProfile(UserId userId) {
        return users.findById(userId)
                .map(UserResult::from)
                .orElseThrow(() -> new NotFoundException("Usuário", userId.asString()));
    }
}
