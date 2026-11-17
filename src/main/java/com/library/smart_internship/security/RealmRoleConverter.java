package com.library.smart_internship.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts only the platform's supported Keycloak realm roles into Spring
 * Security authorities. Client roles and arbitrary realm roles are ignored.
 */
@Component
public class RealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Set<String> SUPPORTED_ROLES =
            Set.of("STUDENT", "COMPANY", "SUPERVISOR", "ADMIN");

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return List.of();
        }

        Object rawRoles = realmAccess.get("roles");
        if (!(rawRoles instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(SUPPORTED_ROLES::contains)
                .distinct()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
