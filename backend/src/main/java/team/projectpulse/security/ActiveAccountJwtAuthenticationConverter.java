package team.projectpulse.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

/**
 * Refuses a bearer token whose account is deactivated or gone, on every request (FR-SEC-active-account).
 *
 * <p><strong>Why per request.</strong> A JWT is valid until it expires, and login is the only place that used to
 * read {@code enabled}. A student deactivated mid-session therefore kept full access for the rest of her token's
 * lifetime. Reading the account here closes that window at the cost of one primary-key lookup per request. A
 * shorter token lifetime, a token-version claim, and a revocation list were rejected in {@code design/stu.md}.
 *
 * <p><strong>Why throw {@link DisabledException}.</strong> It is an {@code AuthenticationException}, so the
 * bearer-token filter hands it to {@code CustomBearerTokenAuthenticationEntryPoint} and the caller gets 401 with
 * the account-status message, not a 500. The authorities are still built by the existing
 * {@link JwtAuthenticationConverter} bean, to which this delegates once the account is confirmed active.
 */
@Component
public class ActiveAccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    private final JwtAuthenticationConverter delegate;


    public ActiveAccountJwtAuthenticationConverter(UserRepository userRepository, JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.userRepository = userRepository;
        this.delegate = jwtAuthenticationConverter;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Integer userId = readUserId(jwt);
        boolean active = userId != null && this.userRepository.findById(userId)
                .map(PeerEvaluationUser::isEnabled)
                .orElse(false);
        if (!active) {
            throw new DisabledException("This account is deactivated or no longer exists.");
        }
        return this.delegate.convert(jwt);
    }

    /**
     * Reads the {@code userId} claim the way {@code UserUtils.getUserId} does: the encoder writes an Integer, but
     * a JSON round-trip can hand it back as a Long or a String.
     */
    private static Integer readUserId(Jwt jwt) {
        Object claim = jwt.getClaim("userId");
        if (claim instanceof Integer i) return i;
        if (claim instanceof Long l) return l.intValue();
        if (claim instanceof String s) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

}
