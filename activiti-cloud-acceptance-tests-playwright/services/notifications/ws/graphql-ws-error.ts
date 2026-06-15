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

export function formatGraphqlWsError(error: unknown): Error {
    if (error instanceof Error) {
        return error;
    }

    if (Array.isArray(error)) {
        return new Error(error.map(formatGraphqlWsErrorPart).join('; '));
    }

    if (typeof error === 'object' && error !== null) {
        const record = error as Record<string, unknown>;

        if ('code' in record || 'reason' in record) {
            const code = record.code ?? 'unknown';
            const reason = record.reason ?? '';
            return new Error(`WebSocket closed: code=${code} reason=${reason}`);
        }

        if ('errors' in record && Array.isArray(record.errors)) {
            return new Error(record.errors.map(formatGraphqlWsErrorPart).join('; '));
        }

        if ('message' in record && record.message != null) {
            return new Error(String(record.message));
        }

        try {
            return new Error(JSON.stringify(error));
        } catch {
            return new Error(String(error));
        }
    }

    return new Error(String(error));
}

function formatGraphqlWsErrorPart(part: unknown): string {
    if (typeof part === 'object' && part !== null && 'message' in part) {
        return String((part as { message: unknown }).message);
    }
    return String(part);
}
