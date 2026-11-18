package com.clincanywhere.security;

import com.clincanywhere.entity.UserAccount;
import com.clincanywhere.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.jspecify.annotations.Nullable;

/** Creates the first cross-clinic platform owner from explicit production secrets. */
@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.bootstrap.platform-owner.enabled", havingValue = "true")
public class PlatformOwnerBootstrap implements ApplicationRunner {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwords;
    private final String username;
    private final String password;

    public PlatformOwnerBootstrap(UserAccountRepository accounts, PasswordEncoder passwords,
                                  @Value("${app.bootstrap.platform-owner.username:}") String username,
                                  @Value("${app.bootstrap.platform-owner.password:}") String password) {
        this.accounts = accounts; this.passwords = passwords; this.username = username; this.password = password;
    }

    @Override @Transactional
    public void run(@Nullable ApplicationArguments args) {
        if (accounts.existsByRole("PLATFORM_OWNER")) return;
        int length = password.getBytes(StandardCharsets.UTF_8).length;
        if (username.isBlank() || length < 16 || length > 72) {
            throw new IllegalStateException("Platform owner bootstrap requires a username and a 16-72 byte password");
        }
        if (accounts.existsByUsername(username)) throw new IllegalStateException("Platform owner username is already taken");
        var owner = new UserAccount(username, passwords.encode(password), "PLATFORM_OWNER", Set.of());
        owner.updateProfile("Platform Owner", null, null);
        accounts.save(owner);
    }
}
