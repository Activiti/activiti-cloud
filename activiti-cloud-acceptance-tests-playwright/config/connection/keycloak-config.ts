import { isDevelopProfile } from './cluster-profile';
import { resolveSsoConnection } from './sso-url';

export interface KeycloakOAuthConfig {
    tokenUrl: string;
    hostHeader?: string;
    clientId: string;
    clientSecret?: string;
}

export function getKeycloakOAuthConfig(): KeycloakOAuthConfig {
    const { tokenUrl, hostHeader } = resolveSsoConnection();

    const clientId = process.env.KEYCLOAK_CLIENT_ID || (isDevelopProfile() ? 'activiti' : 'activiti');
    const clientSecret = process.env.KEYCLOAK_CLIENT_SECRET?.trim();

    return {
        tokenUrl,
        ...(hostHeader ? { hostHeader } : {}),
        clientId,
        ...(clientSecret ? { clientSecret } : {}),
    };
}
