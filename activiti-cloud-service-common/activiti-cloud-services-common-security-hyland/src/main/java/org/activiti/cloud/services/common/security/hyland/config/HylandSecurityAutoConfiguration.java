package org.activiti.cloud.services.common.security.hyland.config;

import org.activiti.api.runtime.shared.security.PrincipalIdentityProvider;
import org.activiti.api.runtime.shared.security.SecurityContextPrincipalProvider;
import org.activiti.api.runtime.shared.security.SecurityContextTokenProvider;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.common.security.hyland.HylandIAMSecurityContextTokenProvider;
import org.activiti.cloud.services.common.security.hyland.HylandSecurityContextPrincipalProvider;
import org.activiti.cloud.services.common.security.hyland.HylandSecurityManagerImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class HylandSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextTokenProvider securityContextTokenProvider() {
        return new HylandIAMSecurityContextTokenProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextPrincipalProvider authenticatedPrincipalProvider() {
        return new HylandSecurityContextPrincipalProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityManager securityManager(SecurityContextPrincipalProvider authenticatedPrincipalProvider,
        PrincipalIdentityProvider principalIdentityProvider,
        HylandPrincipalGroupsProviderChain principalGroupsProvider,
        HylandPrincipalRolesProviderChain principalRolesProviderChain) {
        return new HylandSecurityManagerImpl(authenticatedPrincipalProvider,
            principalIdentityProvider,
            principalGroupsProvider,
            principalRolesProviderChain);
    }


}
