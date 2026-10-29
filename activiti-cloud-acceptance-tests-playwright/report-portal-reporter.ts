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

import RPReporter from '@reportportal/agent-js-playwright';
import type { TestCase, TestResult } from '@playwright/test/reporter';
import { parseReportPortalOutput } from './rp-output-parser';
import { reportPortalAbortController } from './rp-request-controller';
import { gateReportPortalRequests, type RestClientLike } from './rp-request-gate';

const DEFAULT_FINISH_TIMEOUT_MS = 60 * 1000;
const DEFAULT_MAX_CONCURRENT_REQUESTS = 10;
const DEFAULT_START_JITTER_MS = 5000;
const MAX_LISTED_TESTS = 50;
const MAX_REASON_LENGTH = 120;

interface ClientResponse {
    tempId: string;
    promise: Promise<unknown>;
}

interface ReportPortalClientInternals {
    restClient: RestClientLike;
    startTestItem: (...args: unknown[]) => ClientResponse;
    finishTestItem: (itemTempId: string, ...args: unknown[]) => ClientResponse;
    sendLog: (itemTempId: string, ...args: unknown[]) => ClientResponse;
}

interface FailedRequest {
    reason: string;
    itemTempId?: string;
}

interface BufferedLog {
    level: string;
    message: string;
    time: number;
}

/**
 * ReportPortal reporter that:
 * - sends console output (stdout/stderr) only for tests that did not end with their expected status,
 *   so passing tests do not flood ReportPortal with log requests (set RP_SEND_ALL_LOGS=true to send everything),
 * - does not block the run for longer than RP_FINISH_TIMEOUT_MS (default 60s) when ReportPortal is slow or unavailable,
 * - limits concurrent requests to RP_MAX_CONCURRENT_REQUESTS (default 10) and delays the first one by a random
 *   0..RP_START_JITTER_MS (default 5s), so parallel CI jobs do not hit ReportPortal with simultaneous bursts,
 * - prints a single summary with the affected tests instead of a stack trace for every failed request.
 */
export default class ReportPortalReporter extends RPReporter {
    private readonly sendAllLogs = process.env.RP_SEND_ALL_LOGS === 'true';
    private readonly finishTimeoutMs = Number(process.env.RP_FINISH_TIMEOUT_MS) || DEFAULT_FINISH_TIMEOUT_MS;
    private readonly bufferedLogs = new Map<string, BufferedLog[]>();
    private readonly stdoutCarry = new Map<string, string>();
    private readonly testTitles = new Map<string, string>();
    private readonly requestItems = new WeakMap<Promise<unknown>, string>();
    private readonly pendingRequests = new Map<Promise<unknown>, string | undefined>();
    private readonly failedRequests: FailedRequest[] = [];

    constructor(config: ConstructorParameters<typeof RPReporter>[0]) {
        super(config);
        const client = this.client as unknown as ReportPortalClientInternals;
        gateReportPortalRequests(client.restClient, {
            maxConcurrent: Number(process.env.RP_MAX_CONCURRENT_REQUESTS) || DEFAULT_MAX_CONCURRENT_REQUESTS,
            startJitterMs: Number(process.env.RP_START_JITTER_MS ?? DEFAULT_START_JITTER_MS) || 0,
            signal: reportPortalAbortController.signal,
        });
        this.trackItemRequests(client);
    }

    addRequestToPromisesQueue<T>(promise: Promise<T>): void {
        const itemTempId = this.requestItems.get(promise);
        this.pendingRequests.set(promise, itemTempId);
        this.promises.push(
            promise.then(
                () => {
                    this.pendingRequests.delete(promise);
                },
                (error: unknown) => {
                    this.pendingRequests.delete(promise);
                    this.failedRequests.push({ reason: describeError(error), itemTempId });
                },
            ),
        );
    }

    onTestBegin(test: TestCase): void {
        this.bufferedLogs.set(test.id, []);
        super.onTestBegin(test);
        const item = this.testItems.get(test.id);
        if (item) {
            this.testTitles.set(item.id, test.titlePath().filter(Boolean).join(' › '));
        }
    }

    onStdOut(chunk: string | Buffer, test?: TestCase): void {
        if (!this.shouldBuffer(test)) {
            super.onStdOut(chunk, test);
            return;
        }

        const output = `${this.stdoutCarry.get(test.id) ?? ''}${String(chunk)}`;
        this.stdoutCarry.delete(test.id);
        const parsed = parseReportPortalOutput(output);
        for (const part of parsed.parts) {
            if (part.type === 'event') {
                super.onStdOut(part.value, test);
            } else {
                this.bufferLog(test, 'INFO', part.value);
            }
        }
        if (parsed.carry) {
            this.stdoutCarry.set(test.id, parsed.carry);
        }
    }

    onStdErr(chunk: string | Buffer, test?: TestCase): void {
        if (!this.shouldBuffer(test)) {
            super.onStdErr(chunk, test);
            return;
        }
        const message = String(chunk);
        this.bufferLog(test, message.toLowerCase().includes('error') ? 'ERROR' : 'WARN', message);
    }

    async onTestEnd(test: TestCase, result: TestResult): Promise<void> {
        const unfinishedOutput = this.stdoutCarry.get(test.id);
        if (unfinishedOutput) {
            this.bufferLog(test, 'INFO', unfinishedOutput);
        }
        this.stdoutCarry.delete(test.id);
        const logs = this.bufferedLogs.get(test.id) ?? [];
        this.bufferedLogs.delete(test.id);
        if (result.status !== test.expectedStatus) {
            logs.forEach((log) => this.sendTestItemLog(log, test));
        }
        return super.onTestEnd(test, result);
    }

    async onEnd(): Promise<void> {
        let timer: NodeJS.Timeout | undefined;
        const timeout = new Promise<'timeout'>((resolve) => {
            timer = setTimeout(() => resolve('timeout'), this.finishTimeoutMs);
        });
        const finish = super.onEnd().then(() => 'finished' as const);
        const outcome = await Promise.race([finish, timeout]);
        clearTimeout(timer);
        const timedOut = outcome === 'timeout';
        if (timedOut) {
            reportPortalAbortController.abort();
            console.warn(`[ReportPortal] Launch was not finished within ${this.finishTimeoutMs}ms, not waiting any longer.`);
        }
        this.printDeliverySummary(timedOut);
    }

    private trackItemRequests(client: ReportPortalClientInternals): void {
        const startTestItem = client.startTestItem.bind(client);
        client.startTestItem = (...args) => {
            const response = startTestItem(...args);
            this.requestItems.set(response.promise, response.tempId);
            return response;
        };
        for (const method of ['finishTestItem', 'sendLog'] as const) {
            const original = client[method].bind(client);
            client[method] = (itemTempId, ...args) => {
                const response = original(itemTempId, ...args);
                this.requestItems.set(response.promise, itemTempId);
                return response;
            };
        }
    }

    private printDeliverySummary(timedOut: boolean): void {
        const notDelivered: FailedRequest[] = [...this.failedRequests];
        if (timedOut) {
            this.pendingRequests.forEach((itemTempId) => notDelivered.push({ reason: 'not finished in time', itemTempId }));
        }
        if (notDelivered.length === 0) {
            return;
        }

        const reasons = new Map<string, number>();
        const tests = new Set<string>();
        for (const { reason, itemTempId } of notDelivered) {
            reasons.set(reason, (reasons.get(reason) ?? 0) + 1);
            const title = itemTempId && this.testTitles.get(itemTempId);
            if (title) {
                tests.add(title);
            }
        }

        const reasonSummary = [...reasons].map(([reason, count]) => `${count}x ${reason}`).join(', ');
        const lines = [`[ReportPortal] ${notDelivered.length} request(s) were not delivered (${reasonSummary}). The launch in ReportPortal may be incomplete.`];
        if (tests.size > 0) {
            const listed = [...tests].slice(0, MAX_LISTED_TESTS);
            lines.push(`[ReportPortal] Affected tests (${tests.size}):`, ...listed.map((title) => `  - ${title}`));
            if (tests.size > listed.length) {
                lines.push(`  ... and ${tests.size - listed.length} more`);
            }
        } else {
            lines.push('[ReportPortal] Only launch/suite level requests were affected.');
        }
        console.warn(lines.join('\n'));
    }

    private shouldBuffer(test?: TestCase): test is TestCase {
        return !this.sendAllLogs && !!test && this.bufferedLogs.has(test.id);
    }

    private bufferLog(test: TestCase, level: string, message: string): void {
        // Keep the original timestamp so the log timeline in ReportPortal matches the test execution
        this.bufferedLogs.get(test.id)?.push({ level, message, time: Date.now() });
    }
}

function describeError(error: unknown): string {
    const message = error instanceof Error ? error.message : String(error);
    // RestClient appends the response body, URL and method - keep only the reason so failures can be grouped
    return message.split('\n')[0].split(': {')[0].slice(0, MAX_REASON_LENGTH);
}
