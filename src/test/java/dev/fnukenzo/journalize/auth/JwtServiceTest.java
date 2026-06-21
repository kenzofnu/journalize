package dev.fnukenzo.journalize.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import dev.fnukenzo.journalize.user.User;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // No Spring is running in a unit test, so @Value fields are null.
        // We set them manually to mimic what Spring would inject at runtime.
        ReflectionTestUtils.setField(jwtService, "secret",
                "a-test-secret-that-is-long-enough-to-satisfy-hmac-sha-algorithms");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
    }

    @Test
    void generateToken_thenExtractUsername_returnsSameUsername() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        String token = jwtService.generateToken(user);
        String extracted = jwtService.extractUsername(token);

        assertThat(extracted).isEqualTo("alice");
    }

    @Test
    void extractUsername_withTamperedToken_throws() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        String token = jwtService.generateToken(user);
        // Flip the last character to break the signature
        char lastChar = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (lastChar == 'a' ? 'b' : 'a');

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractUsername_withExpiredToken_throws() {
        // Set expiration to a negative value so the token is "born expired"
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        String expiredToken = jwtService.generateToken(user);

        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(Exception.class);
    }
}
