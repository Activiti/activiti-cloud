package org.activiti.cloud.services.common.security.test.support;

import java.util.Map;
import java.util.Set;

public interface RolesClaimProvider {

    void setResourceRoles(Map<String, String[]> resourceRoles, Map<String, Object> claims);

    void setGlobalRoles(Set<String> globalRoles, Map<String, Object> claims);
}
