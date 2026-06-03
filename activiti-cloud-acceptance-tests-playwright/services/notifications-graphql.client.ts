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

import { createClient, type Client } from 'graphql-ws';
import {
    DEFAULT_RUNTIME_BUNDLE_SERVICE_NAME,
    resolveNotificationsEndpoints,
} from '../config/connection/notifications-url';
import { tryTakeMatchingEngineEvents } from '../helpers/notifications.flow';
import { webSocketLogger } from '../helpers/logging/logger-builder';
import {
    createTraefikAwareWebSocket,
    formatGraphqlWsError,
} from '../helpers/notifications-websocket';
import type { EngineEventNotification, EngineEventType } from '../models/notifications.models';

const ENGINE_EVENTS_SUBSCRIPTION = `
subscription(
  $serviceName: String!
  $eventTypes: [EngineEventType!]
  $businessKey: String!
  $processDefinitionKey: String!
) {
  engineEvents(
    serviceName: [$serviceName]
    eventType: $eventTypes
    businessKey: [$businessKey]
    processDefinitionKey: [$processDefinitionKey]
  ) {
    serviceName
    processDefinitionKey
    eventType
  }
}`;

const ENGINE_EVENTS_SUBSCRIPTION_WITH_ACTOR = `
subscription(
  $serviceName: String!
  $eventTypes: [EngineEventType!]
  $businessKey: String!
  $processDefinitionKey: String!
  $actor: String!
) {
  engineEvents(
    serviceName: [$serviceName]
    eventType: $eventTypes
    businessKey: [$businessKey]
    processDefinitionKey: [$processDefinitionKey]
    actor: [$actor]
  ) {
    serviceName
    processDefinitionKey
    eventType
    actor
  }
}`;

export interface EngineEventsSubscriptionOptions {
    accessToken: string;
    eventTypes: EngineEventType[];
    businessKey: string;
    processDefinitionKey: string;
    serviceName?: string;
    actor?: string;
    subscriptionReadyTimeoutMs?: number;
}

export interface EngineEventsSubscription {
    readonly client: Client;
    awaitReady(): Promise<void>;
    waitForExpectedEvents(
        expected: EngineEventNotification[],
        timeoutMs?: number
    ): Promise<EngineEventNotification[]>;
    close(): void;
}

function normalizeAccessToken(accessToken: string): string {
    return accessToken.startsWith('Bearer ') ? accessToken : `Bearer ${accessToken}`;
}

/**
 * GraphQL engineEvents subscription over WebSocket (graphql-transport-ws / graphql-ws client).
 * Parity with Serenity {@code ProcessInstanceNotifications} + {@code NotificationsSteps}.
 */
export function createEngineEventsSubscription(
    options: EngineEventsSubscriptionOptions
): EngineEventsSubscription {
    const {
        accessToken,
        eventTypes,
        businessKey,
        processDefinitionKey,
        serviceName = DEFAULT_RUNTIME_BUNDLE_SERVICE_NAME,
        actor,
        subscriptionReadyTimeoutMs = 15_000,
    } = options;

    const { webSocketGraphqlUrl, hostHeader } = resolveNotificationsEndpoints();
    const eventBuffer: EngineEventNotification[] = [];
    let bufferNotify: (() => void) | null = null;

    const authHeader = normalizeAccessToken(accessToken);
    let subscriptionReady: Promise<void>;
    let resolveReady!: () => void;
    let rejectReady!: (error: Error) => void;
    let readyResolved = false;
    subscriptionReady = new Promise<void>((resolve, reject) => {
        resolveReady = () => {
            if (!readyResolved) {
                readyResolved = true;
                clearTimeout(readyTimer);
                resolve();
            }
        };
        rejectReady = (error: Error) => {
            if (!readyResolved) {
                readyResolved = true;
                clearTimeout(readyTimer);
                reject(error);
            }
        };
    });

    const readyTimer = setTimeout(() => {
        rejectReady(new Error(`GraphQL subscription not ready within ${subscriptionReadyTimeoutMs}ms`));
    }, subscriptionReadyTimeoutMs);

    const notifyBufferWaiters = () => {
        const notify = bufferNotify;
        bufferNotify = null;
        notify?.();
    };

    const appendEvents = (events: EngineEventNotification[]) => {
        if (events.length === 0) {
            return;
        }
        eventBuffer.push(...events);
        notifyBufferWaiters();
    };

    const client = createClient({
        url: webSocketGraphqlUrl,
        webSocketImpl: createTraefikAwareWebSocket(hostHeader, authHeader),
        connectionParams: {
            Authorization: authHeader,
        },
        connectionAckWaitTimeout: 15_000,
        on: {
            connected: () => {
                webSocketLogger.debug(`connected ${webSocketGraphqlUrl}`);
                resolveReady();
            },
            closed: (event) => {
                webSocketLogger.debug(`closed ${formatGraphqlWsError(event).message}`);
            },
            error: (error) => {
                const err = formatGraphqlWsError(error);
                webSocketLogger.error(`connection error: ${err.message}`);
                rejectReady(err);
            },
        },
    });

    const document = actor ? ENGINE_EVENTS_SUBSCRIPTION_WITH_ACTOR : ENGINE_EVENTS_SUBSCRIPTION;
    const variables: Record<string, unknown> = {
        serviceName,
        eventTypes,
        businessKey,
        processDefinitionKey,
        ...(actor ? { actor } : {}),
    };

    let unsubscribe = () => {};

    unsubscribe = client.subscribe(
        { query: document, variables },
        {
            next: (result) => {
                if (result.errors?.length) {
                    const message = result.errors.map((e) => e.message).join('; ');
                    rejectReady(new Error(`GraphQL subscription error: ${message}`));
                    return;
                }

                resolveReady();
                const events = (result.data?.engineEvents ?? []) as EngineEventNotification[];
                appendEvents(events);
            },
            error: (error) => {
                const err = formatGraphqlWsError(error);
                rejectReady(err);
            },
            complete: () => {
                webSocketLogger.debug('subscription complete');
            },
        }
    );

    return {
        client,
        awaitReady: () => subscriptionReady,
        async waitForExpectedEvents(
            expected: EngineEventNotification[],
            timeoutMs = 60_000
        ): Promise<EngineEventNotification[]> {
            await subscriptionReady;
            const deadline = Date.now() + timeoutMs;

            while (Date.now() < deadline) {
                const taken = tryTakeMatchingEngineEvents(eventBuffer, expected);
                if (taken) {
                    return taken;
                }

                const remaining = deadline - Date.now();
                if (remaining <= 0) {
                    break;
                }

                await new Promise<void>((resolve, reject) => {
                    const timer = setTimeout(() => {
                        bufferNotify = null;
                        reject(
                            new Error(
                                `Timed out after ${timeoutMs}ms waiting for engine events: ${expected
                                    .map((e) => e.eventType)
                                    .join(',')}`
                            )
                        );
                    }, remaining);
                    bufferNotify = () => {
                        clearTimeout(timer);
                        resolve();
                    };
                });
            }

            throw new Error(
                `Timed out after ${timeoutMs}ms waiting for engine events: ${expected
                    .map((e) => e.eventType)
                    .join(',')}`
            );
        },
        close() {
            clearTimeout(readyTimer);
            unsubscribe();
            client.dispose();
        },
    };
}
