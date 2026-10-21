package br.com.sentinelcorp.Neuro_Gen.identity.infrastructure.service;

import br.com.sentinelcorp.Neuro_Gen.identity.domain.model.User;
import br.com.sentinelcorp.Neuro_Gen.identity.domain.port.TokenIssuer;
import br.com.sentinelcorp.Neuro_Gen.shared.config.NeuroGenProperties;
import br.com.sentinelcorp.Neuro_Gen.shared.security.CurrentUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Emite o access token JWT assinado (RS256) usando o {@link JwtEncoder} do Authorization Server.
 * Claims: {@code sub}=userId, {@code role}, {@code email}, {@code email_verified}.
 */
@Component
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final NeuroGenProperties props;

    public JwtTokenIssuer(JwtEncoder jwtEncoder, NeuroGenProperties props) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
    }

    @Override
    public IssuedAccessToken issueAccessToken(User user) {
        long ttl = props.getJwt().getAccessTokenTtlSeconds();
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.getJwt().getIssuer())
                .issuedAt(now)
                .expiresAt(now.plus(ttl, ChronoUnit.SECONDS))
                .subject(user.id().asString())
                .claim(CurrentUser.ROLE_CLAIM, user.role().name())
                .claim("email", user.email().value())
                .claim(CurrentUser.EMAIL_VERIFIED_CLAIM, user.emailVerified())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, ttl);
    }
}
