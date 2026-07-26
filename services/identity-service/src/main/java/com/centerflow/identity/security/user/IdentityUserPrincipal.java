package com.centerflow.identity.security.user;

import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.user.domain.User;
import com.centerflow.identity.user.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class IdentityUserPrincipal
        implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final UserStatus status;
    private final List<RoleName> roles;
    private final List<GrantedAuthority> authorities;

    private IdentityUserPrincipal(
            UUID userId,
            String email,
            String passwordHash,
            UserStatus status,
            List<RoleName> roles
    ) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.roles = List.copyOf(roles);

        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(
                        "ROLE_" + role.name()
                ))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public static IdentityUserPrincipal from(
            User user,
            List<RoleName> roles
    ) {
        return new IdentityUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getStatus(),
                roles
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public List<RoleName> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority>
    getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}