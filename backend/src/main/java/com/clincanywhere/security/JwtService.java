package com.clincanywhere.security;

import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationSeconds;
    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      Environment environment,
                      @Value("${app.jwt.expiration-seconds:900}") long expirationSeconds) {
        if (secret == null || secret.isBlank()) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("JWT_SECRET must be configured in the production profile.");
            }
            secret = "Y2FyZXBpbG90LWxvY2FsLWRlbW8tc2VjcmV0LWtleS1ub3QtZm9yLXByb2Q=";
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
    }
    public String issue(UserDetails user) {
        Instant now = Instant.now();
        String role = user.getAuthorities().stream().findFirst().orElseThrow().getAuthority().replace("ROLE_", "");
        return Jwts.builder().subject(user.getUsername()).claim("role", role)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key).compact();
    }
    public Claims claims(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
    public String subject(String token) { return claims(token).getSubject(); }
    public boolean isValid(String token, UserDetails user) {
        return isValid(claims(token), user);
    }
    public boolean isValid(Claims claims, UserDetails user) {
        return claims.getSubject().equals(user.getUsername()) && user.isEnabled()
                && claims.getExpiration().after(new Date());
    }
    public long expirationSeconds() { return expirationSeconds; }
}
