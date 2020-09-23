# activiti-cloud-services-common-security-keycloak

This module provides Keycloak implementation of the Activiti [SecurityManager](https://github.com/Activiti/Activiti/blob/develop/activiti-api/activiti-api-runtime-shared/src/main/java/org/activiti/api/runtime/shared/security/SecurityManager.java) Api using the following building blocks:

- [SecurityContextPrincipalProvider](activiti-cloud-service-common/activiti-cloud-services-common-security-keycloak/src/main/java/org/activiti/cloud/services/common/security/keycloak/KeycloakSecurityContextTokenProvider.java)
- [PrincipalIdentityProvider](https://github.com/Activiti/activiti-cloud/blob/develop/activiti-cloud-service-common/activiti-cloud-services-common-security-keycloak/src/main/java/org/activiti/cloud/services/common/security/keycloak/KeycloakPrincipalIdentityProvider.java)
- [PrincipalGroupsProvider](https://github.com/Activiti/activiti-cloud/blob/develop/activiti-cloud-service-common/activiti-cloud-services-common-security-keycloak/src/main/java/org/activiti/cloud/services/common/security/keycloak/KeycloakAccessTokenPrincipalGroupsProvider.java)
- [PrincipalRolesProvider](https://github.com/Activiti/activiti-cloud/blob/develop/activiti-cloud-service-common/activiti-cloud-services-common-security-keycloak/src/main/java/org/activiti/cloud/services/common/security/keycloak/KeycloakAccessTokenPrincipalRolesProvider.java)
