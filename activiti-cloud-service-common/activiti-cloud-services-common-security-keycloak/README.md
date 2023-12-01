# activiti-cloud-services-common-security-keycloak

This module provides Keycloak implementation of the Activiti [SecurityManager](https://github.com/Activiti/Activiti/blob/develop/activiti-api/activiti-api-runtime-shared/src/main/java/org/activiti/api/runtime/shared/security/SecurityManager.java) Api using the following building blocks:

- [SecurityContextPrincipalProvider](src/main/java/org/activiti/cloud/services/common/security/keycloak/KeycloakSecurityContextTokenProvider.java)
- [PrincipalIdentityProvider](src/main/java/org/activiti/cloud/services/common/security/keycloak/KeycloakPrincipalIdentityProvider.java)
- [PrincipalGroupsProvider](src/main/java/org/activiti/cloud/services/common/security/keycloak/KeycloakAccessTokenPrincipalGroupsProvider.java)
- [PrincipalRolesProvider](src/main/java/org/activiti/cloud/services/common/security/keycloak/KeycloakAccessTokenPrincipalRolesProvider.java)

In order to be able to extract groups from Keycloak JWT the 'KeycloakAccessTokenPrincipalGroupsProvider' requires extra configuration on Keycloak client to map groups as claims in the JWT, i.e.

```json
{
  "id": "5f5272d0-dd32-49ba-b1ab-ac2e6a66d4aa",
  "name": "groups",
  "protocol": "openid-connect",
  "protocolMapper": "oidc-group-membership-mapper",
  "consentRequired": false,
  "config": {
    "full.path": "false",
    "id.token.claim": "true",
    "access.token.claim": "true",
    "claim.name": "groups",
    "userinfo.token.claim": "true"
  }
}
```
