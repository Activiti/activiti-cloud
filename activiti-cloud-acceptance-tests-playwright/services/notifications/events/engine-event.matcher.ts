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

export function engineEventMatches(
    actual: EngineEventNotification,
    expected: EngineEventNotification
): boolean {
    return (
        actual.eventType === expected.eventType &&
        actual.processDefinitionKey === expected.processDefinitionKey &&
        (expected.serviceName === undefined || actual.serviceName === expected.serviceName) &&
        (expected.actor === undefined || actual.actor === expected.actor)
    );
}

export function takeMatchingEngineEvents(
    buffer: EngineEventNotification[],
    expected: EngineEventNotification[]
): EngineEventNotification[] | null {
    const usedIndices = new Set<number>();
    const taken: EngineEventNotification[] = [];

    for (const exp of expected) {
        const index = buffer.findIndex(
            (actual, i) => !usedIndices.has(i) && engineEventMatches(actual, exp)
        );
        if (index < 0) {
            return null;
        }
        usedIndices.add(index);
        taken.push(buffer[index]);
    }

    [...usedIndices]
        .sort((a, b) => b - a)
        .forEach((index) => buffer.splice(index, 1));

    return taken;
}
