package com.clincanywhere.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.jspecify.annotations.Nullable;

/** Fail closed until the accountable release owner records approval and its evidence reference. */
@Component
public class ProductionReadinessGate implements ApplicationRunner {
    private final Environment environment;
    public ProductionReadinessGate(Environment environment) { this.environment=environment; }
    @Override public void run(@Nullable ApplicationArguments args) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) return;
        boolean approved=environment.getProperty("app.production.release-approved",Boolean.class,false);
        String evidence=environment.getProperty("app.production.evidence-reference","").trim();
        if (!approved || evidence.isBlank()) throw new IllegalStateException(
                "Production startup blocked: complete docs/PRODUCTION-RELEASE-GATES.md and set PRODUCTION_RELEASE_GATES_APPROVED=true plus PRODUCTION_RELEASE_EVIDENCE_REF");
    }
}
