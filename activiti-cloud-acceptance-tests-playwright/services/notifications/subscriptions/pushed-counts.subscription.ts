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

    const ws = new GraphQlWebSocketClient(
        webSocketGraphqlUrl,
        normalizeBearer(accessToken),
        hostHeader
    );

    ws.onNext((data) => {
        const payload = data as Record<string, PushedCountMessage> | undefined;
        if (payload) {
            countTypes.forEach((type) => {
                if (payload[type]) {
                    countUpdates[type].push(payload[type]);
                }
            });
        }
    });

    ws.connect();

    try {
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
    } catch (error) {
        ws.close();
        throw error;
    }

    await settleAfterSubscribe();

    await waitForInitialMessages(countUpdates, countTypes, readyTimeoutMs);

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

async function waitForInitialMessages(
    countUpdates: Record<string, PushedCountMessage[]>,
    countTypes: PushedCountType[],
    timeoutMs: number
): Promise<void> {
    const startTime = Date.now();
    const pollInterval = 50;

    while (Date.now() - startTime < timeoutMs) {
        const allReceived = countTypes.every((type) => countUpdates[type].length > 0);
        if (allReceived) {
            return;
        }
        await new Promise((resolve) => setTimeout(resolve, pollInterval));
    }

    const missing = countTypes.filter((type) => countUpdates[type].length === 0);
    throw new Error(
        `Timeout waiting for initial pushed count messages for: ${missing.join(', ')}. ` +
        `This indicates the subscription was not properly registered on the backend. ` +
        `Please check that the pushed-counts feature is enabled on the query service.`
    );
}
