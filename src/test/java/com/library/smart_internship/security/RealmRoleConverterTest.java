package com.library.smart_internship.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealmRoleConverterTest {

    private final RealmRoleConverter converter = new RealmRoleConverter();

    @Test
    void mapsOnlySupportedRealmRolesToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("user-123")
                .claim("realm_access", Map.of("roles",
                        List.of("STUDENT", "COMPANY", "offline_access", "uma_authorization")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        List<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();

        assertEquals(List.of("ROLE_COMPANY", "ROLE_STUDENT"), authorities);
    }

    @Test
    void returnsNoAuthoritiesWhenRealmRolesAreAbsent() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        assertEquals(List.of(), converter.convert(jwt));
    }
}
