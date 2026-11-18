package com.clincanywhere.security;

import com.clincanywhere.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.jspecify.annotations.Nullable;

@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.bootstrap.admin.enabled", havingValue = "true")
public class ProductionAdminBootstrap implements ApplicationRunner {
    private final UserAccountRepository accounts;
    private final String username;
    private final String password;
    public ProductionAdminBootstrap(UserAccountRepository accounts,
                                   @Value("${app.bootstrap.admin.username:}") String username,
                                   @Value("${app.bootstrap.admin.password:}") String password) {
        this.accounts = accounts; this.username = username; this.password = password;
    }
    @Override @Transactional
    public void run(@Nullable ApplicationArguments args) {
        if (accounts.existsByRole("ADMIN")) return;
        if (username.isBlank() || password.getBytes(StandardCharsets.UTF_8).length < 16 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("One-time admin bootstrap requires a username and a 16-72 byte password");
        }
        if (accounts.existsByUsername(username)) throw new IllegalStateException("Bootstrap username already exists without admin authority");
        // A platform admin is provisioned through the lead conversion workflow. A
        // bootstrap clinic admin must not receive memberships in fictional tenants.
        throw new IllegalStateException("Admin bootstrap cannot create a clinic-less administrator. Convert an authorized clinic lead to provision its first clinic admin.");
    }
}
