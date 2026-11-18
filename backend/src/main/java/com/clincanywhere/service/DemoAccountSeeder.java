package com.clincanywhere.service;

import java.util.Map;
import com.clincanywhere.entity.UserAccount;
import com.clincanywhere.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Demo accounts are limited to development, test, and isolated public-demo deployments. */
@Component
@Profile({"dev", "test", "demo & !prod"})
public class DemoAccountSeeder implements CommandLineRunner {
    private final UserAccountRepository accounts;
    private final PasswordEncoder passwords;
    public DemoAccountSeeder(UserAccountRepository accounts, PasswordEncoder passwords) { this.accounts = accounts; this.passwords = passwords; }
    @Override public void run(String... args) {
        Map<String, String> roles = Map.of("admin", "ADMIN", "reception", "RECEPTIONIST", "doctor", "DOCTOR",
                "labtech", "LAB", "pharmacy", "PHARMACIST", "billing", "BILLING", "store", "INVENTORY",
                "owner", "PLATFORM_OWNER", "onboarder", "ONBOARDING");
        roles.forEach((username, role) -> {
            var account = accounts.findByUsername(username).orElseGet(() -> new UserAccount(username, passwords.encode("demo123"), role));
            account.assignClinics(role.equals("PLATFORM_OWNER") || role.equals("ONBOARDING")
                    ? java.util.Set.of() : java.util.Set.of("clinic-a", "clinic-b", "clinic-c"));
            account.updateProfile(switch (role) {
                case "PLATFORM_OWNER" -> "Platform Owner";
                case "ONBOARDING" -> "Onboarding Teammate";
                default -> username;
            }, null, null);
            accounts.save(account);
        });
    }
}
