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

type RequestFn = (...args: unknown[]) => Promise<unknown>;

export interface RestClientLike {
    request: RequestFn;
}

export interface RequestGateOptions {
    maxConcurrent: number;
    startJitterMs: number;
    signal: AbortSignal;
}

const silencedErrors = new WeakSet<object>();
let consoleDirPatched = false;

/**
 * Wraps the ReportPortal REST client so that:
 * - at most `maxConcurrent` requests are in flight at once (the rest wait in a FIFO queue),
 * - the first request is delayed by a random jitter, so parallel CI jobs do not hit ReportPortal at the same moment,
 * - queued requests are rejected immediately once `signal` is aborted,
 * - failed requests do not print a stack trace each (the reporter prints a single summary instead).
 */
export function gateReportPortalRequests(restClient: RestClientLike, { maxConcurrent, startJitterMs, signal }: RequestGateOptions): void {
    silenceReportPortalRequestErrors();
    const request = restClient.request.bind(restClient);
    const waiting: Array<() => void> = [];
    let active = 0;
    let startDelay: Promise<void> | undefined;

    signal.addEventListener('abort', () => waiting.splice(0).forEach((wake) => wake()), { once: true });

    const run = async (args: unknown[]): Promise<unknown> => {
        startDelay ??= delay(Math.floor(Math.random() * startJitterMs));
        await startDelay;
        while (active >= maxConcurrent && !signal.aborted) {
            await new Promise<void>((resolve) => waiting.push(resolve));
        }
        if (signal.aborted) {
            throw new Error('canceled');
        }
        active++;
        try {
            return await request(...args);
        } finally {
            active--;
            waiting.shift()?.();
        }
    };

    restClient.request = (...args: unknown[]) =>
        run(args).catch((error: unknown) => {
            if (error && typeof error === 'object') {
                silencedErrors.add(error);
            }
            throw error;
        });
}

/** The ReportPortal client prints every failed request with console.dir - skip the ones the reporter already tracks. */
function silenceReportPortalRequestErrors(): void {
    if (consoleDirPatched) {
        return;
    }
    consoleDirPatched = true;
    const dir = console.dir.bind(console);
    console.dir = (item?: unknown, options?: object) => {
        if (item && typeof item === 'object' && silencedErrors.has(item)) {
            return;
        }
        dir(item, options);
    };
}

function delay(ms: number): Promise<void> {
    return ms > 0 ? new Promise((resolve) => setTimeout(resolve, ms).unref()) : Promise.resolve();
}
