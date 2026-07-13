package br.com.flowstock.config;

import br.com.flowstock.domain.entity.User;
import br.com.flowstock.domain.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record UserPrincipal(User user) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    public Long userId() {
        return user.getId();
    }

    public UserRole role() {
        return user.getRole();
    }

    public Long companyId() {
        return user.getCompany() == null ? null : user.getCompany().getId();
    }
}
