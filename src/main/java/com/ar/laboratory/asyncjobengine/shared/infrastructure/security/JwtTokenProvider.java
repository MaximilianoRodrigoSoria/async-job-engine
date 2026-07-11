package com.ar.laboratory.asyncjobengine.shared.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Proveedor de tokens JWT.
 *
 * <p>Responsable de:
 *
 * <ul>
 *   <li>Generar tokens firmados con HMAC-SHA256.
 *   <li>Validar la firma y la expiración.
 *   <li>Extraer el {@link Authentication} de Spring Security a partir del token.
 * </ul>
 *
 * <p>Se instancia solo cuando {@code app.security.enabled=true} via {@link SecurityConfig}.
 */
@Slf4j
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(JwtProperties props) {
        this.signingKey = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = props.expirationMs();
    }

    /**
     * Genera un token JWT para el subject (username) dado.
     *
     * @param subject nombre de usuario o identificador único
     * @param roles lista de roles del usuario (ej. ["ROLE_USER", "ROLE_ADMIN"])
     * @return token JWT firmado
     */
    public String generateToken(String subject, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Valida un token JWT.
     *
     * @param token token a validar
     * @return {@code true} si es válido y no expiró; {@code false} en caso contrario
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el {@link Authentication} de Spring Security a partir del token.
     *
     * @param token token JWT válido
     * @return objeto de autenticación con el subject como principal y sus roles como authorities
     */
    @SuppressWarnings("unchecked")
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        List<String> roles = claims.get("roles", List.class);

        List<SimpleGrantedAuthority> authorities =
                (roles == null ? List.<String>of() : roles)
                        .stream().map(SimpleGrantedAuthority::new).toList();

        return new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
