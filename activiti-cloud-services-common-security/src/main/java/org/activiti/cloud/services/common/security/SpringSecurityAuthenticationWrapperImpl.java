package org.activiti.cloud.services.common.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityAuthenticationWrapperImpl implements SpringSecurityAuthenticationWrapper {

    @Override
    public String getAuthenticatedUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
