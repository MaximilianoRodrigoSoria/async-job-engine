package com.ar.laboratory.asyncjobengine.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private static final String SECRET =
            "test-secret-key-for-jwt-signing-256bits-minimum-0123456789";

    private final JwtProperties props = new JwtProperties(SECRET, 3_600_000L);
    private final JwtTokenProvider provider = new JwtTokenProvider(props);

    @Test
    @DisplayName("Genera un token válido y lo valida correctamente")
    void generaYValidaToken() {
        String token = provider.generateToken("alice", List.of("ROLE_USER", "ROLE_ADMIN"));

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Extrae el Authentication con subject y roles")
    void extraeAuthenticationConRoles() {
        String token = provider.generateToken("alice", List.of("ROLE_USER"));

        Authentication auth = provider.getAuthentication(token);

        assertThat(auth.getName()).isEqualTo("alice");
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Un token inválido devuelve false")
    void tokenInvalidoDevuelveFalse() {
        assertThat(provider.validateToken("esto-no-es-un-jwt")).isFalse();
    }

    @Test
    @DisplayName("Maneja la ausencia de roles sin fallar")
    void manejaRolesVacios() {
        String token = provider.generateToken("bob", List.of());

        Authentication auth = provider.getAuthentication(token);

        assertThat(auth.getName()).isEqualTo("bob");
        assertThat(auth.getAuthorities()).isEmpty();
    }
}
