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

import type { EngineEventNotification } from './engine-event.model';
import { takeMatchingEngineEvents } from './engine-event.matcher';

export class EngineEventBuffer {
    private readonly events: EngineEventNotification[] = [];
    private wakeWaiter: (() => void) | null = null;

    add(batch: EngineEventNotification[]): void {
        if (batch.length === 0) {
            return;
        }
        this.events.push(...batch);
        this.wakeWaiter?.();
        this.wakeWaiter = null;
    }

    async waitFor(expected: EngineEventNotification[], timeoutMs: number): Promise<EngineEventNotification[]> {
        const deadline = Date.now() + timeoutMs;

        while (Date.now() < deadline) {
            const taken = takeMatchingEngineEvents(this.events, expected);
            if (taken) {
                return taken;
            }

            const remaining = deadline - Date.now();
            if (remaining <= 0) {
                break;
            }

            await new Promise<void>((resolve) => {
                const timer = setTimeout(() => {
                    this.wakeWaiter = null;
                    resolve();
                }, remaining);

                this.wakeWaiter = () => {
                    clearTimeout(timer);
                    resolve();
                };

                if (takeMatchingEngineEvents(this.events, expected)) {
                    clearTimeout(timer);
                    this.wakeWaiter = null;
                    resolve();
                }
            });
        }

        throw new Error(formatEngineEventTimeout(expected, timeoutMs));
    }
}

function formatEngineEventTimeout(expected: EngineEventNotification[], timeoutMs: number): string {
    const eventTypes = expected.map((event) => event.eventType).join(',');
    return `Timed out after ${timeoutMs}ms waiting for engine events: ${eventTypes}`;
}
