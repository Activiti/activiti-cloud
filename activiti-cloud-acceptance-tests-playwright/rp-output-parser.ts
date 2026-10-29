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

const reportPortalEventTypes = new Set([
    'rp:setDescription',
    'rp:setTestCaseId',
    'rp:setStatus',
    'rp:setLaunchStatus',
    'rp:addAttributes',
    'rp:addLog',
    'rp:addLaunchLog',
]);

export interface ReportPortalOutputPart {
    type: 'output' | 'event';
    value: string;
}

export interface ParsedReportPortalOutput {
    parts: ReportPortalOutputPart[];
    carry: string;
}

export function parseReportPortalOutput(output: string): ParsedReportPortalOutput {
    const parts: ReportPortalOutputPart[] = [];
    let outputStart = 0;

    for (let index = 0; index < output.length; index += 1) {
        if (output[index] !== '{') {
            continue;
        }

        const objectEnd = findJsonObjectEnd(output, index);
        if (objectEnd === -1) {
            const candidate = output.slice(index);
            if (isPartialReportPortalEvent(candidate)) {
                if (index > outputStart) {
                    parts.push({ type: 'output', value: output.slice(outputStart, index) });
                }
                return { parts, carry: candidate };
            }
            continue;
        }

        const value = output.slice(index, objectEnd + 1);
        if (isReportPortalEvent(value)) {
            if (index > outputStart) {
                parts.push({ type: 'output', value: output.slice(outputStart, index) });
            }
            parts.push({ type: 'event', value });
            outputStart = objectEnd + 1;
            index = objectEnd;
        } else {
            index = objectEnd;
        }
    }

    if (outputStart < output.length) {
        parts.push({ type: 'output', value: output.slice(outputStart) });
    }
    return { parts, carry: '' };
}

function findJsonObjectEnd(value: string, start: number): number {
    let depth = 0;
    let inString = false;
    let escaped = false;

    for (let index = start; index < value.length; index += 1) {
        const character = value[index];
        if (inString) {
            if (escaped) {
                escaped = false;
            } else {
                switch (character) {
                    case '\\':
                        escaped = true;
                        break;
                    case '"':
                        inString = false;
                        break;
                }
            }
            continue;
        }

        if (character === '"') {
            inString = true;
        } else if (character === '{') {
            depth += 1;
        } else if (character === '}') {
            depth -= 1;
            if (depth === 0) {
                return index;
            }
        }
    }
    return -1;
}

function isReportPortalEvent(value: string): boolean {
    try {
        const parsed: unknown = JSON.parse(value);
        return typeof parsed === 'object' && parsed !== null && 'type' in parsed && typeof parsed.type === 'string' && reportPortalEventTypes.has(parsed.type);
    } catch {
        return false;
    }
}

function isPartialReportPortalEvent(value: string): boolean {
    const prefix = '{"type":"';
    if (value.length < prefix.length) {
        return prefix.startsWith(value);
    }
    if (!value.startsWith(prefix)) {
        return false;
    }

    const partialType = value.slice(prefix.length);
    const closingQuote = partialType.indexOf('"');
    const type = closingQuote === -1 ? partialType : partialType.slice(0, closingQuote);
    return [...reportPortalEventTypes].some((eventType) => eventType.startsWith(type));
}
