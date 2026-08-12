package br.com.flowstock.config;

import br.com.flowstock.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class InitialAdminPasswordSeeder implements ApplicationRunner {
    private static final String MIGRATION_PLACEHOLDER_HASH =
        "$2a$10$dXJ3SW6G7P50lGmMkkIGbu6Q3jqqi3q.y1v5mLwM4vOL2aDeJ9WvO";
    private static final int MINIMUM_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String initialPassword;

    public InitialAdminPasswordSeeder(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        @Value("${INITIAL_ADMIN_PASSWORD:}") String initialPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialPassword = initialPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findByEmailIgnoreCase("admin@flowstock.local").ifPresent(user -> {
            if (!MIGRATION_PLACEHOLDER_HASH.equals(user.getPasswordHash())) {
                return;
            }

            validateInitialPassword();
            user.setPasswordHash(passwordEncoder.encode(initialPassword));
            user.setUpdatedAt(Instant.now());
        });
    }

    private void validateInitialPassword() {
        if (initialPassword == null || initialPassword.isBlank()) {
            throw new IllegalStateException(
                "Defina INITIAL_ADMIN_PASSWORD antes da primeira inicialização."
            );
        }

        if (initialPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                "INITIAL_ADMIN_PASSWORD deve possuir pelo menos 12 caracteres."
            );
        }
    }
}
