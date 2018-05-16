package org.activiti.cloud.services.identity.basic;

import org.activiti.engine.UserGroupLookupProxy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@ConditionalOnProperty(name = "activiti.cloud.services.identity.basic.enabled", matchIfMissing = true)
@Import(SecurityConfig.class)
public class ActivitiBasicIdentityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BasicUserGroupLookupProxy.class)
    public BasicUserGroupLookupProxy basicUserGroupLookupProxy(InMemoryUserDetailsManager userDetailsService){
        return new BasicUserGroupLookupProxy(userDetailsService);
    }

    @Bean
    @ConditionalOnMissingBean(BasicUserRoleLookupProxy.class)
    public BasicUserRoleLookupProxy basicUserRoleLookupProxy(UserGroupLookupProxy userGroupLookupProxy){
        return new BasicUserRoleLookupProxy(userGroupLookupProxy);
    }
}
