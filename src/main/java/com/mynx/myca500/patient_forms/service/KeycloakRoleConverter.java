package com.mynx.myca500.patient_forms.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keycloak puts roles in realm_access.roles and resource_access.<client>.roles.
 * Each becomes ROLE_<name>, so @PreAuthorize("hasRole('ADMIN')") works.
 * Scopes (SCOPE_xxx) are kept as well.
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
    private final String clientId;

    public KeycloakRoleConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        Collection<GrantedAuthority> scopes = scopeConverter.convert(jwt);
        if (scopes != null) {
            authorities.addAll(scopes);
        }

        addRoles(jwt.getClaimAsMap("realm_access"), authorities);

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null && clientId != null && !clientId.isBlank()) {
            Object client = resourceAccess.get(clientId);
            if (client instanceof Map<?, ?> clientMap) {
                addRoles(clientMap, authorities);
            }
        }
        return authorities;
    }

    private void addRoles(Map<?, ?> source, Set<GrantedAuthority> out) {
        if (source == null) {
            return;
        }
        if (source.get("roles") instanceof Collection<?> roles) {
            roles.forEach(r -> out.add(new SimpleGrantedAuthority("ROLE_" + r)));
        }
    }
}