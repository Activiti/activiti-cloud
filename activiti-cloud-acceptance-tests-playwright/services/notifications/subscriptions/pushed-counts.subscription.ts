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

import { resolveNotificationsEndpoints } from '../connection/endpoints';
import { GraphQlWebSocketClient } from '../ws/graphql-websocket.client';
import { normalizeBearer, settleAfterSubscribe } from './subscription-utils';
import { webSocketLogger } from '../../../helpers/logging/logger-builder';

export interface PushedCountMessage {
    count: number;
    asOf: string;
}

type PushedCountType = 'assignedTasks' | 'queuedTasks' | 'runningProcesses';

export interface PushedCountsSubscriptionOptions {
    accessToken: string;
    countTypes: PushedCountType[];
    readyTimeoutMs?: number;
}

export interface PushedCountsSubscription {
    getLatestCounts(): Record<string, PushedCountMessage>;
    close(): void;
}

const buildPushedCountsSubscriptionQuery = (
    countType: PushedCountType
): string => {
    return `
        subscription pushedCounts_${countType} {
            ${countType} {
                count
                asOf
            }
        }
    `;
};

export async function openPushedCountsSubscription(
    options: PushedCountsSubscriptionOptions
): Promise<PushedCountsSubscription> {
    const {
        accessToken,
        countTypes,
        readyTimeoutMs = 30_000,
    } = options;

    const { webSocketGraphqlUrl, hostHeader } = resolveNotificationsEndpoints();
    const countUpdates: Record<string, PushedCountMessage[]> = {};
    countTypes.forEach((type) => {
        countUpdates[type] = [];
    });

    webSocketLogger.info(`Opening pushed counts subscription for: ${countTypes.join(', ')}`);
    webSocketLogger.debug(`WebSocket URL: ${webSocketGraphqlUrl}`);

    const ws = new GraphQlWebSocketClient(
        webSocketGraphqlUrl,
        normalizeBearer(accessToken),
        hostHeader
    );

    ws.onNext((data) => {
        const payload = data as Record<string, PushedCountMessage> | undefined;
        webSocketLogger.debug(`Received pushed counts message: ${JSON.stringify(payload)}`);
        if (payload) {
            countTypes.forEach((type) => {
                if (payload[type]) {
                    countUpdates[type].push(payload[type]);
                    webSocketLogger.debug(`Updated ${type}: ${payload[type].count} (as of ${payload[type].asOf})`);
                }
            });
        }
    });

    ws.onError((error) => {
        const errorMsg = `WebSocket error: ${error.message}`;
        webSocketLogger.error(errorMsg);
    });

    ws.connect();

    try {
        webSocketLogger.debug(`Subscribing to ${countTypes.length} count types...`);
        await Promise.all(
            countTypes.map((countType) =>
                ws.subscribe(
                    {
                        operationName: `pushedCounts_${countType}`,
                        query: buildPushedCountsSubscriptionQuery(countType),
                        variables: {},
                    },
                    readyTimeoutMs
                )
            )
        );
        webSocketLogger.debug(`All subscriptions sent to backend`);
    } catch (error) {
        webSocketLogger.error(`Subscription failed: ${error}`);
        ws.close();
        throw error;
    }

    await settleAfterSubscribe();

    webSocketLogger.debug(`Waiting for initial message batch (timeout: ${readyTimeoutMs}ms)...`);
    const startTime = Date.now();
    let allTypesReceived = false;

    while (!allTypesReceived && (Date.now() - startTime) < readyTimeoutMs) {
        allTypesReceived = countTypes.every((type) => countUpdates[type].length > 0);
        if (!allTypesReceived) {
            await new Promise((resolve) => setTimeout(resolve, 100));
        }
    }

    if (allTypesReceived) {
        webSocketLogger.info(`All count types received initial message, clearing counters to capture only new updates`);
    } else {
        const received = countTypes.filter((type) => countUpdates[type].length > 0).join(', ') || 'none';
        webSocketLogger.warn(
            `Timeout waiting for initial message batch. Received: ${received}. ` +
            `Subscription may still work, proceeding anyway.`
        );
    }

    countTypes.forEach((type) => {
        countUpdates[type] = [];
    });

    return {
        getLatestCounts: () => {
            const latest: Record<string, PushedCountMessage> = {};
            countTypes.forEach((type) => {
                const updates = countUpdates[type];
                if (updates.length > 0) {
                    latest[type] = updates[updates.length - 1];
                }
            });
            return latest;
        },
        close: () => ws.close(),
    };
}
