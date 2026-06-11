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

import { timeouts } from '../../../config/runtime/timeouts';
import {
    DEFAULT_RUNTIME_BUNDLE_SERVICE_NAME,
    resolveNotificationsEndpoints,
} from '../connection/endpoints';
import { EngineEventBuffer } from '../events/engine-event-buffer';
import type { EngineEventNotification, EngineEventType } from '../events/engine-event.model';
import { GraphQlWebSocketClient } from '../ws/graphql-websocket.client';
import { ENGINE_EVENTS_SUBSCRIPTION } from './engine-events.queries';

export interface OpenEngineEventsSubscriptionOptions {
    accessToken: string;
    eventTypes: EngineEventType[];
    businessKey: string;
    processDefinitionKey: string;
    serviceName?: string;
    actor?: string;
    readyTimeoutMs?: number;
}

export interface EngineEventsSubscription {
    waitForExpectedEvents(
        expected: EngineEventNotification[],
        timeoutMs?: number
    ): Promise<EngineEventNotification[]>;
    close(): void;
}

export async function openEngineEventsSubscription(
    options: OpenEngineEventsSubscriptionOptions
): Promise<EngineEventsSubscription> {
    const {
        accessToken,
        eventTypes,
        businessKey,
        processDefinitionKey,
        serviceName = DEFAULT_RUNTIME_BUNDLE_SERVICE_NAME,
        actor,
        readyTimeoutMs = 15_000,
    } = options;

    const { webSocketGraphqlUrl, hostHeader } = resolveNotificationsEndpoints();
    const buffer = new EngineEventBuffer();
    const ws = new GraphQlWebSocketClient(
        webSocketGraphqlUrl,
        normalizeBearer(accessToken),
        hostHeader
    );

    ws.onNext((data) => {
        const payload = data as { engineEvents?: EngineEventNotification[] } | undefined;
        buffer.add(payload?.engineEvents ?? []);
    });

    ws.connect();

    try {
        await ws.subscribe(
            {
                operationName: 'engineEvents',
                query: ENGINE_EVENTS_SUBSCRIPTION,
                variables: buildSubscriptionVariables({
                    serviceName,
                    eventTypes,
                    businessKey,
                    processDefinitionKey,
                    actor,
                }),
            },
            readyTimeoutMs
        );
    } catch (error) {
        ws.close();
        throw error;
    }

    await settleAfterSubscribe();

    return {
        waitForExpectedEvents: (expected, timeoutMs = timeouts.engineEvents.wait) =>
            buffer.waitFor(expected, timeoutMs),
        close: () => ws.close(),
    };
}

async function settleAfterSubscribe(): Promise<void> {
    const settleMs = timeouts.engineEvents.subscriptionSettle;
    if (settleMs <= 0) {
        return;
    }
    await new Promise((resolve) => setTimeout(resolve, settleMs));
}

function buildSubscriptionVariables(options: {
    serviceName: string;
    eventTypes: EngineEventType[];
    businessKey: string;
    processDefinitionKey: string;
    actor?: string;
}): Record<string, unknown> {
    return {
        serviceName: options.serviceName,
        eventTypes: options.eventTypes,
        businessKey: options.businessKey,
        processDefinitionKey: options.processDefinitionKey,
        ...(options.actor ? { actor: [options.actor] } : {}),
    };
}

function normalizeBearer(accessToken: string): string {
    return accessToken.startsWith('Bearer ') ? accessToken : `Bearer ${accessToken}`;
}
