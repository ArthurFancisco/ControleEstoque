package br.com.flowstock.service;

import br.com.flowstock.config.UserPrincipal;
import br.com.flowstock.domain.entity.Company;
import br.com.flowstock.domain.entity.User;
import br.com.flowstock.domain.enums.UserRole;
import br.com.flowstock.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Usuario nao autenticado.");
        }
        return principal.user();
    }

    public Long requireCompanyId() {
        User user = currentUser();
        Company company = user.getCompany();
        if (company == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Usuario sem empresa vinculada.");
        }
        return company.getId();
    }

    public boolean isSuperAdmin() {
        return currentUser().getRole() == UserRole.SUPER_ADMIN;
    }
}
