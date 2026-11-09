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
    waitForCountUpdate(predicate: (count: PushedCountMessage) => boolean, timeoutMs?: number): Promise<PushedCountMessage>;
    waitForAssignedTasksCount(predicate: (count: number) => boolean, timeoutMs?: number): Promise<PushedCountMessage>;
    waitForQueuedTasksCount(predicate: (count: number) => boolean, timeoutMs?: number): Promise<PushedCountMessage>;
    waitForRunningProcessesCount(predicate: (count: number) => boolean, timeoutMs?: number): Promise<PushedCountMessage>;
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
        readyTimeoutMs = 15_000,
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

    return {
        waitForCountUpdate: (predicate, timeoutMs = timeouts.engineEvents.wait) =>
            waitForPredicate(countUpdates, predicate, timeoutMs),
        waitForAssignedTasksCount: (predicate, timeoutMs) =>
            waitForCountPredicate(countUpdates, 'assignedTasks', predicate, timeoutMs),
        waitForQueuedTasksCount: (predicate, timeoutMs) =>
            waitForCountPredicate(countUpdates, 'queuedTasks', predicate, timeoutMs),
        waitForRunningProcessesCount: (predicate, timeoutMs) =>
            waitForCountPredicate(countUpdates, 'runningProcesses', predicate, timeoutMs),
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

async function waitForPredicate(
    countUpdates: Record<string, PushedCountMessage[]>,
    predicate: (count: PushedCountMessage) => boolean,
    timeoutMs: number
): Promise<PushedCountMessage> {
    const startTime = Date.now();
    const pollInterval = 100;

    while (Date.now() - startTime < timeoutMs) {
        for (const updates of Object.values(countUpdates)) {
            for (const update of updates) {
                if (predicate(update)) {
                    return update;
                }
            }
        }
        await new Promise((resolve) => setTimeout(resolve, pollInterval));
    }

    throw new Error(`Timeout waiting for count predicate after ${timeoutMs}ms`);
}

async function waitForCountPredicate(
    countUpdates: Record<string, PushedCountMessage[]>,
    countType: string,
    predicate: (count: number) => boolean,
    timeoutMs: number = timeouts.engineEvents.wait
): Promise<PushedCountMessage> {
    const startTime = Date.now();
    const pollInterval = 100;

    while (Date.now() - startTime < timeoutMs) {
        const updates = countUpdates[countType] ?? [];
        for (const update of updates) {
            if (predicate(update.count)) {
                return update;
            }
        }
        await new Promise((resolve) => setTimeout(resolve, pollInterval));
    }
    throw new Error(`Timeout waiting for ${countType} count predicate after ${timeoutMs}ms`);
}
