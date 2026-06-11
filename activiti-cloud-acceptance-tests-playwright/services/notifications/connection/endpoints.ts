/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { resolveGatewayConnection } from '../../../config/connection/gateway-url';

export interface NotificationsEndpoints {
    httpGraphqlUrl: string;
    webSocketGraphqlUrl: string;
    hostHeader?: string;
}

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
