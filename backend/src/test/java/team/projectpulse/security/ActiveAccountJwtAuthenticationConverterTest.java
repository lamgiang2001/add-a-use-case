package team.projectpulse.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtAuthenticationConverterTest {

    @Mock
    UserRepository userRepository;

    ActiveAccountJwtAuthenticationConverter converter;


    @BeforeEach
    void setUp() {
        this.converter = new ActiveAccountJwtAuthenticationConverter(this.userRepository, new JwtAuthenticationConverter());
    }

    private static Jwt jwtWithUserId(Object userId) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject("j.moon@abc.edu")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.build();
    }

    @Test
    void testActiveAccountIsConverted() {
        Student jerry = new Student("j.moon@abc.edu", "Jerry", "Moon", "j.moon@abc.edu", "123456", true, "student");
        given(this.userRepository.findById(6)).willReturn(Optional.of(jerry));

        AbstractAuthenticationToken token = this.converter.convert(jwtWithUserId(6L));

        assertThat(token.getName()).isEqualTo("j.moon@abc.edu");
    }

    @Test
    void testDeactivatedAccountIsRefused() {
        Student jerry = new Student("j.moon@abc.edu", "Jerry", "Moon", "j.moon@abc.edu", "123456", false, "student");
        given(this.userRepository.findById(6)).willReturn(Optional.of(jerry));

        assertThatThrownBy(() -> this.converter.convert(jwtWithUserId(6)))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void testUnknownAccountIsRefused() {
        given(this.userRepository.findById(999999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> this.converter.convert(jwtWithUserId("999999")))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void testMissingOrMalformedUserIdClaimIsRefused() {
        assertThatThrownBy(() -> this.converter.convert(jwtWithUserId(null)))
                .isInstanceOf(DisabledException.class);
        assertThatThrownBy(() -> this.converter.convert(jwtWithUserId("not-a-number")))
                .isInstanceOf(DisabledException.class);
    }

}
