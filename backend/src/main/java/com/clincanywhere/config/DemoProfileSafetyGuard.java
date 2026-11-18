package com.clincanywhere.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo & prod")
public class DemoProfileSafetyGuard {
    public DemoProfileSafetyGuard() {
        throw new IllegalStateException("The demo profile must never be activated with the production profile.");
    }
}
