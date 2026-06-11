/**
 * GraphQL notifications endpoints (hosted on activiti-cloud-query via /notifications ingress).
 */

import { resolveGatewayConnection } from './gateway-url';

export interface NotificationsEndpoints {
    httpGraphqlUrl: string;
    webSocketGraphqlUrl: string;
    hostHeader?: string;
}

/** Default runtime-bundle service name used in engine event subscriptions (Serenity: runtime.bundle.service.name=rb). */
export const DEFAULT_RUNTIME_BUNDLE_SERVICE_NAME =
    process.env.RUNTIME_BUNDLE_SERVICE_NAME?.trim() || 'rb';

export function resolveNotificationsEndpoints(): NotificationsEndpoints {
    const { baseURL, hostHeader } = resolveGatewayConnection();
    const httpBase = baseURL.replace(/\/$/, '');
    const wsBase = httpBase.replace(/^http/, 'ws');

    return {
        httpGraphqlUrl: `${httpBase}/notifications/graphql`,
        webSocketGraphqlUrl: `${wsBase}/notifications/v2/ws/graphql`,
        hostHeader,
    };
}
