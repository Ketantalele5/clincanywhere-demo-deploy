package com.clincanywhere.security;

import com.clincanywhere.service.CurrentActorProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextActorProvider implements CurrentActorProvider {
    @Override
    public String currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
}
