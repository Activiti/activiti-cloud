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

import { randomUUID } from 'node:crypto';
import WebSocket from 'ws';
import { webSocketLogger } from '../../../helpers/logging/logger-builder';
import { formatGraphqlWsError } from './graphql-ws-error';

const GRAPHQL_WS_SUBPROTOCOL = 'graphql-transport-ws';

const MESSAGE = {
    CONNECTION_ACK: 'connection_ack',
    CONNECTION_ERROR: 'connection_error',
    CONNECTION_INIT: 'connection_init',
    CONNECTION_TERMINATE: 'connection_terminate',
    NEXT: 'next',
    PING: 'ping',
    PONG: 'pong',
    SUBSCRIBE: 'subscribe',
} as const;

export interface GraphQlSubscribePayload {
    query: string;
    variables?: Record<string, unknown>;
    operationName?: string;
}

type GraphQlNextHandler = (data: unknown) => void;
type GraphQlErrorHandler = (error: Error) => void;

export class GraphQlWebSocketClient {
    private ws: WebSocket | undefined;
    private connectionAcknowledged = false;
    private readonly nextHandlers: GraphQlNextHandler[] = [];
    private readonly errorHandlers: GraphQlErrorHandler[] = [];

    constructor(
        private readonly url: string,
        private readonly authorization: string,
        private readonly hostHeader?: string
    ) {}

    connect(): void {
        if (this.ws) {
            return;
        }

        const headers: Record<string, string> = { Authorization: this.authorization };
        if (this.hostHeader) {
            headers.Host = this.hostHeader;
        }

        this.ws = new WebSocket(this.url, GRAPHQL_WS_SUBPROTOCOL, { headers });
        this.ws.on('open', () => this.sendConnectionInit());
        this.ws.on('message', (raw) => this.handleMessage(raw));
        this.ws.on('error', (error) => this.notifyError(formatGraphqlWsError(error)));
        this.ws.on('close', (code, reason) => {
            webSocketLogger.debug(`closed WebSocket closed: code=${code} reason=${reason.toString()}`);
        });
    }

    onNext(handler: GraphQlNextHandler): void {
        this.nextHandlers.push(handler);
    }

    onError(handler: GraphQlErrorHandler): void {
        this.errorHandlers.push(handler);
    }

    async waitForConnectionAck(timeoutMs = 15_000): Promise<void> {
        const startedAt = Date.now();
        while (!this.connectionAcknowledged) {
            if (Date.now() - startedAt >= timeoutMs) {
                throw new Error(`GraphQL WebSocket connection_ack not received within ${timeoutMs}ms`);
            }
            await sleep(100);
        }
    }

    async subscribe(payload: GraphQlSubscribePayload, timeoutMs = 15_000): Promise<void> {
        await this.waitForConnectionAck(timeoutMs);

        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            throw new Error('GraphQL WebSocket is not open');
        }

        webSocketLogger.debug(`subscribe ${payload.operationName ?? 'anonymous'}`);
        this.ws.send(
            JSON.stringify({
                id: randomUUID(),
                type: MESSAGE.SUBSCRIBE,
                payload: { ...payload, extensions: {} },
            })
        );
    }

    close(): void {
        if (this.ws?.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({ type: MESSAGE.CONNECTION_TERMINATE }));
            this.ws.close();
        }
        this.ws = undefined;
        this.connectionAcknowledged = false;
    }

    private sendConnectionInit(): void {
        if (!this.ws) {
            throw new Error('GraphQL WebSocket is not initialized');
        }

        webSocketLogger.debug(`connected ${this.url}`);
        this.connectionAcknowledged = false;
        this.ws.send(
            JSON.stringify({
                type: MESSAGE.CONNECTION_INIT,
                payload: { Authorization: this.authorization },
            })
        );
    }

    private handleMessage(raw: WebSocket.RawData): void {
        const message = JSON.parse(rawDataToString(raw)) as {
            type?: string;
            payload?: { data?: unknown; errors?: unknown[] };
        };

        switch (message.type) {
            case MESSAGE.CONNECTION_ACK:
                webSocketLogger.debug('connection_ack received');
                this.connectionAcknowledged = true;
                break;
            case MESSAGE.CONNECTION_ERROR:
                this.notifyError(new Error(`GraphQL connection error: ${rawDataToString(raw)}`));
                break;
            case MESSAGE.PING:
                this.ws?.send(JSON.stringify({ type: MESSAGE.PONG, payload: {} }));
                break;
            case MESSAGE.NEXT:
                if (message.payload?.errors?.length) {
                    this.notifyError(
                        new Error(
                            message.payload.errors
                                .map((part) => formatGraphqlWsError(part).message)
                                .join('; ')
                        )
                    );
                    break;
                }
                this.nextHandlers.forEach((handler) => handler(message.payload?.data));
                break;
            default:
                webSocketLogger.debug(`ignored GraphQL WS message type: ${message.type ?? 'undefined'}`);
        }
    }

    private notifyError(error: Error): void {
        webSocketLogger.error(`connection error: ${error.message}`);
        this.errorHandlers.forEach((handler) => handler(error));
    }
}

function rawDataToString(raw: WebSocket.RawData): string {
    if (typeof raw === 'string') {
        return raw;
    }
    if (raw instanceof Buffer) {
        return raw.toString();
    }
    if (Array.isArray(raw)) {
        return Buffer.concat(raw).toString();
    }
    return Buffer.from(new Uint8Array(raw)).toString();
}

function sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
}
