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

import { jwtDecode } from 'jwt-decode';
import { users } from '../../../config/users';
import { DEFAULT_RUNTIME_BUNDLE_SERVICE_NAME } from '../connection/endpoints';
import type { EngineEventNotification, EngineEventType } from './engine-event.model';

export function actorFromAccessToken(accessToken: string): string {
    const claims = jwtDecode<{ sub?: string }>(accessToken);
    return claims.sub ?? users.testAdminUser.username;
}

export function expectedEngineEventBatch(
    eventTypes: EngineEventType[],
    processDefinitionKey: string,
    options: { serviceName?: string; actor?: string } = {}
): EngineEventNotification[] {
    const serviceName = options.serviceName ?? DEFAULT_RUNTIME_BUNDLE_SERVICE_NAME;
    return eventTypes.map((eventType) => ({
        serviceName,
        processDefinitionKey,
        eventType,
        ...(options.actor ? { actor: options.actor } : {}),
    }));
}
