package br.com.flowstock.service;

import br.com.flowstock.config.JwtService;
import br.com.flowstock.domain.entity.Company;
import br.com.flowstock.domain.entity.User;
import br.com.flowstock.domain.enums.CompanyStatus;
import br.com.flowstock.domain.enums.UserRole;
import br.com.flowstock.repository.UserRepository;
import br.com.flowstock.web.dto.AuthDtos.LoginRequest;
import br.com.flowstock.web.dto.AuthDtos.LoginResponse;
import br.com.flowstock.web.dto.AuthDtos.UserMeResponse;
import br.com.flowstock.web.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email ou senha invalidos."));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email ou senha invalidos.");
        }

        Company company = user.getCompany();
        if (user.getRole() != UserRole.SUPER_ADMIN && company == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Usuario sem empresa vinculada.");
        }
        if (company != null && (company.getStatus() == CompanyStatus.SUSPENDED || company.getStatus() == CompanyStatus.CANCELED)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Empresa bloqueada. Entre em contato com o suporte.");
        }

        user.setLastLoginAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return new LoginResponse(jwtService.generate(user), toMe(user));
    }

    @Transactional(readOnly = true)
    public UserMeResponse me() {
        return toMe(currentUserService.currentUser());
    }

    public UserMeResponse toMe(User user) {
        Company company = user.getCompany();
        return new UserMeResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            company == null ? null : company.getId(),
            company == null ? null : company.getName(),
            company == null ? null : company.getStatus()
        );
    }
}
