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

const DEFAULT_FINISH_TIMEOUT_MS = 60 * 1000;

interface BufferedLog {
    level: string;
    message: string;
    time: number;
}

/**
 * ReportPortal reporter that:
 * - sends console output (stdout/stderr) only for tests that did not end with their expected status,
 *   so passing tests do not flood ReportPortal with log requests (set RP_SEND_ALL_LOGS=true to send everything),
 * - does not block the run for longer than RP_FINISH_TIMEOUT_MS (default 60s) when ReportPortal is slow or unavailable.
 */
export default class ReportPortalReporter extends RPReporter {
    private readonly sendAllLogs = process.env.RP_SEND_ALL_LOGS === 'true';
    private readonly finishTimeoutMs = Number(process.env.RP_FINISH_TIMEOUT_MS) || DEFAULT_FINISH_TIMEOUT_MS;
    private readonly bufferedLogs = new Map<string, BufferedLog[]>();
    private readonly stdoutCarry = new Map<string, string>();

    onTestBegin(test: TestCase): void {
        this.bufferedLogs.set(test.id, []);
        super.onTestBegin(test);
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
        if (outcome === 'timeout') {
            reportPortalAbortController.abort();
            console.warn(
                `[ReportPortal] Launch was not finished within ${this.finishTimeoutMs}ms, not waiting any longer. The launch in ReportPortal may be incomplete.`,
            );
        }
    }

    private shouldBuffer(test?: TestCase): test is TestCase {
        return !this.sendAllLogs && !!test && this.bufferedLogs.has(test.id);
    }

    private bufferLog(test: TestCase, level: string, message: string): void {
        // Keep the original timestamp so the log timeline in ReportPortal matches the test execution
        this.bufferedLogs.get(test.id)?.push({ level, message, time: Date.now() });
    }
}
