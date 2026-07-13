package br.com.flowstock.web.dto;

import br.com.flowstock.domain.enums.CompanyStatus;
import br.com.flowstock.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {
    }

    public record LoginResponse(
        String token,
        UserMeResponse user
    ) {
    }

    public record UserMeResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        Long companyId,
        String companyName,
        CompanyStatus companyStatus
    ) {
    }
}
