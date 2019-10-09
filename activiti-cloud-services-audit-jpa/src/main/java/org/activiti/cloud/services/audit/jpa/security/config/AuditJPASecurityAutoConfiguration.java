package org.activiti.cloud.services.audit.jpa.security.config;

import org.activiti.api.runtime.shared.identity.UserGroupManager;
import org.activiti.api.runtime.shared.security.SecurityManager;
import org.activiti.cloud.services.audit.jpa.security.SecurityPoliciesApplicationServiceImpl;
import org.activiti.core.common.spring.security.policies.conf.SecurityPoliciesProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditJPASecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityPoliciesApplicationServiceImpl securityPoliciesApplicationService(UserGroupManager userGroupManager,
                                                                                     SecurityManager securityManager,
                                                                                     SecurityPoliciesProperties securityPoliciesProperties) {
        return new SecurityPoliciesApplicationServiceImpl(userGroupManager, 
                                                          securityManager, 
                                                          securityPoliciesProperties);
    }
    
}
