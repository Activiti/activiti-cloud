/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import { APIResponse } from '@playwright/test';
import { CustomAPIRequest } from '../fixtures/context.models';
import { DirtyContextRegistry } from '../helpers/dirty-context';
import { scopedBusinessKey, scopedName, TestScope } from '../helpers/test-isolation';
import { Options, HttpStatusCheck } from '../models/base-service.models';
import { Logger } from '../helpers/logging/logger';
import { PollProfile, pollOptions } from '../config/runtime/timeouts';

export interface RequestResponse {
    [key: string]: any;
    httpStatus?: number;
    body?: string;
}

export interface APIResponseData<T> extends APIResponse {
    data: T;
}

const StatusCodes = {
    Success: /20\d/,
    ClientError: /40\d/,
    ServerError: /50\d/
};

const COLLECTION_ALIASES: Record<string, string[]> = {
    processInstances: ['processInstances', 'list'],
    tasks: ['tasks', 'list'],
    events: ['events', 'list'],
    processDefinitions: ['processDefinitions', 'list'],
    variables: ['variables', 'list'],
    serviceTasks: ['serviceTasks', 'cloudServiceTasks', 'list'],
    cloudIntegrationContexts: ['cloudIntegrationContexts', 'integrationContexts', 'list'],
};

export abstract class BaseService {
    context: CustomAPIRequest;
    protected dirtyRegistry?: DirtyContextRegistry;
    protected testScope?: TestScope;
    protected trackedResourceBase = '/rb/v1';

    constructor(context: CustomAPIRequest) {
        this.context = context;
    }

    attachIsolation(
        dirtyRegistry?: DirtyContextRegistry,
        testScope?: TestScope,
        trackedResourceBase = '/rb/v1'
    ): void {
        this.dirtyRegistry = dirtyRegistry;
        this.testScope = testScope;
        this.trackedResourceBase = trackedResourceBase.replace(/\/$/, '');
    }

    protected trackCreatedResource(relativePath: string): void {
        if (!this.dirtyRegistry || !relativePath) {
            return;
        }
        const path = relativePath.startsWith('/')
            ? relativePath
            : `${this.trackedResourceBase}/${relativePath.replace(/^\//, '')}`;
        this.dirtyRegistry.register(this.context, path);
    }

    protected defaultProcessInstanceName(explicit?: string): string {
        if (explicit) {
            return explicit;
        }
        return this.testScope ? scopedName(this.testScope, 'pi') : `pw-pi-${Date.now()}`;
    }

    protected defaultBusinessKey(explicit?: string): string {
        if (explicit) {
            return explicit;
        }
        return this.testScope ? scopedBusinessKey(this.testScope) : `pw-bk-${Date.now()}`;
    }

    protected static async waitFor<T>(
        fetcher: () => Promise<T>,
        predicate: (value: T) => boolean,
        profile: PollProfile = 'querySync',
        description?: string,
        intervalsOverride?: readonly number[]
    ): Promise<T> {
        const { timeout, intervals } = pollOptions(profile, intervalsOverride);
        const deadline = Date.now() + timeout;
        let attempt = 0;
        let lastValue: T | undefined;
        while (Date.now() < deadline) {
            lastValue = await fetcher();
            if (predicate(lastValue)) {
                return lastValue;
            }
            const intervalMs = intervals[Math.min(attempt, intervals.length - 1)];
            attempt += 1;
            await new Promise((resolve) => setTimeout(resolve, intervalMs));
        }
        throw new Error(
            `Timed out after ${timeout}ms waiting${description ? ` for ${description}` : ''}`
        );
    }

    async get(endpoint: string, options?: Options): Promise<RequestResponse> {
        return this.request('get', endpoint, options);
    }

    async getWithData<T>(endpoint: string, options?: Options): Promise<APIResponseData<T>> {
        const response = (await this.requestRaw('get', endpoint, options)) as APIResponseData<T>;

        try {
            const deserializedData = await response.json();
            response.data = deserializedData;
        } catch (e) {
            Logger.warn(`Could not deserialize data from response: ${e}`);
        }

        return response;
    }

    async postWithData<T>(endpoint: string, options?: Options): Promise<APIResponseData<T>> {
        const response = (await this.requestRaw('post', endpoint, options)) as APIResponseData<T>;

        try {
            const deserializedData = await response.json();
            response.data = deserializedData;
        } catch (e) {
            Logger.warn(`Could not deserialize body data from response: ${e}`);
        }

        return response;
    }

    async post(endpoint: string, options?: Options): Promise<RequestResponse> {
        return this.request('post', endpoint, options);
    }

    async fetch(endpoint: string, options?: Options): Promise<RequestResponse> {
        return this.request('fetch', endpoint, {
            ...options
        });
    }

    async put(endpoint: string, options?: Options): Promise<RequestResponse> {
        return this.request('put', endpoint, options);
    }

    async delete(endpoint: string, options?: Options): Promise<RequestResponse> {
        return this.request('delete', endpoint, options);
    }

    async getHttpStatus(endpoint: string, options?: Options): Promise<number> {
        const response = await this.requestRaw('get', endpoint, options);
        return response.status();
    }

    async postHttpStatus(endpoint: string, options?: Options): Promise<number> {
        const response = await this.requestRaw('post', endpoint, options);
        return response.status();
    }

    private async request(httpMethod: string, endpoint: string, overriddenOptions?: Options): Promise<RequestResponse> {
        const startTime = Date.now();
        const response = await this.requestRaw(httpMethod, endpoint, overriddenOptions);
        const httpStatus = response.status();
        const contentType = response.headers()['content-type'] || '';
        const { bodyText, parsedBody } = await this.readResponseBody(response, contentType);
        const requestTimeDuration = `[${Date.now() - startTime} ms]`;

        if (this.checkStatusCode(httpStatus, StatusCodes['Success'])) {
            Logger.info(
                this.getRequestLogMessage(httpMethod, endpoint, httpStatus, requestTimeDuration, this.context.username!, overriddenOptions)
            );
        }

        if (this.checkStatusCode(httpStatus, StatusCodes['ClientError'])) {
            Logger.warn(this.formatWarnMessage(httpMethod, httpStatus, requestTimeDuration, response.url(), bodyText));
        }

        if (this.checkStatusCode(httpStatus, StatusCodes['ServerError'])) {
            const maskedAsClientError =
                this.bodyMatchesStatusPattern(bodyText, StatusCodes['ClientError']) ||
                this.bodyMatchesStatusPattern(bodyText, /Forbidden/);
            if (!maskedAsClientError) {
                const errorMessage = this.getErrorMessage(
                    httpMethod,
                    endpoint,
                    bodyText,
                    requestTimeDuration,
                    overriddenOptions
                );
                Logger.error(errorMessage);
                throw new Error(errorMessage);
            }
            Logger.warn(this.formatWarnMessage(httpMethod, httpStatus, requestTimeDuration, response.url(), bodyText));
        }

        if (parsedBody !== undefined) {
            return this.wrapParsedBody(httpStatus, parsedBody, bodyText);
        }

        return this.parseJsonResponse(httpStatus, bodyText, contentType);
    }

    private async readResponseBody(
        response: APIResponse,
        contentType: string
    ): Promise<{ bodyText: string; parsedBody?: unknown }> {
        if (contentType.includes('json')) {
            try {
                const parsedBody = await response.json();
                return { bodyText: JSON.stringify(parsedBody), parsedBody };
            } catch {
                return { bodyText: await response.text() };
            }
        }

        return { bodyText: await response.text() };
    }

    private wrapParsedBody(httpStatus: number, parsed: unknown, bodyText: string): RequestResponse {
        if (Array.isArray(parsed)) {
            return parsed as unknown as RequestResponse;
        }
        return typeof parsed === 'object' && parsed !== null
            ? { httpStatus, ...(parsed as Record<string, unknown>) }
            : { httpStatus, body: bodyText };
    }

    private async requestRaw(httpMethod: string, endpoint: string, overriddenOptions?: Options): Promise<APIResponse> {
        const maxAttempts = 3;
        let lastError: unknown;
        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                switch (httpMethod.toLowerCase()) {
                    case 'get':
                        return await this.context.get(endpoint, overriddenOptions);
                    case 'post':
                        return await this.context.post(endpoint, overriddenOptions);
                    case 'put':
                        return await this.context.put(endpoint, overriddenOptions);
                    case 'delete':
                        return await this.context.delete(endpoint, overriddenOptions);
                    case 'fetch':
                        return await this.context.fetch(endpoint, overriddenOptions);
                    default:
                        throw new Error(`Unsupported HTTP method: ${httpMethod}`);
                }
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                const isNetworkError =
                    msg.includes('ECONNREFUSED') ||
                    msg.includes('socket hang up') ||
                    msg.includes('ECONNRESET') ||
                    msg.includes('ETIMEDOUT') ||
                    msg.includes('connect ECONNABORTED');
                if (!isNetworkError || attempt === maxAttempts) {
                    throw err;
                }
                lastError = err;
                const delay = attempt * 2000;
                Logger.warn(`[network] ${httpMethod.toUpperCase()} ${endpoint} failed (attempt ${attempt}/${maxAttempts}): ${msg} — retrying in ${delay}ms`);
                await new Promise((resolve) => setTimeout(resolve, delay));
            }
        }
        throw lastError;
    }

    private parseJsonResponse(httpStatus: number, text: string, contentType: string): RequestResponse {
        if (contentType.includes('application/zip')) {
            return { httpStatus, body: text };
        }

        if (!text) {
            return { httpStatus };
        }

        try {
            const parsed = JSON.parse(text);
            if (Array.isArray(parsed)) {
                return parsed as unknown as RequestResponse;
            }
            return typeof parsed === 'object' && parsed !== null
                ? { httpStatus, ...parsed }
                : { httpStatus, body: text };
        } catch (e) {
            Logger.warn(`Failed to parse JSON response: ${e}`);
            return { httpStatus, body: text };
        }
    }

    private checkStatusCode(statusCode: number, statusRegex: RegExp): boolean {
        return statusRegex.test(statusCode.toString());
    }

    private bodyMatchesStatusPattern(body: string, pattern: RegExp): boolean {
        return pattern.test(body);
    }

    private getRequestLogMessage(
        httpMethod: string,
        endpoint: string,
        statusCode: number,
        requestTimeDuration: string,
        username: string,
        overriddenOptions?: Options
    ): string {
        const baseMessage = `[${username}] :: [${httpMethod.toUpperCase()}] :: [${statusCode}] :: ${requestTimeDuration} :: ${endpoint}`;
        return overriddenOptions && !(overriddenOptions?.data instanceof Buffer)
            ? baseMessage.concat(` with data ${JSON.stringify(overriddenOptions)}`)
            : baseMessage;
    }

    private getErrorMessage(
        httpMethod: string,
        endpoint: string,
        body: string,
        requestTimeDuration: string,
        overriddenOptions?: Options
    ): string {
        const params = JSON.stringify(overriddenOptions?.data);
        return `Error ${httpMethod.toUpperCase()} request :: ${requestTimeDuration} ::
        Endpoint: ${endpoint} ${params ? '\n Params: ' + params : ''}
        Message: ${body}`;
    }

    private formatWarnMessage(
        httpMethod: string,
        statusCode: number,
        requestTimeDuration: string,
        url: string,
        body: string
    ): string {
        return `[${httpMethod.toUpperCase()}] :: [${statusCode}] :: ${requestTimeDuration} :: ${url} Message: ${body}`;
    }

    /** Unwraps Activiti Cloud HAL / list / Nucleus list responses into a typed array. */
    protected unwrapList<T>(response: RequestResponse, embeddedKey: string): T[] {
        if (Array.isArray(response)) {
            return this.flattenListItems<T>(response);
        }

        const { httpStatus, body, content, _embedded, ...rest } = response;
        const keysToTry = COLLECTION_ALIASES[embeddedKey] ?? [embeddedKey, 'list'];

        if (Array.isArray(content)) {
            return this.flattenListItems<T>(content);
        }

        const listContainer = rest.list as { entries?: unknown[] } | undefined;
        if (listContainer?.entries) {
            return this.flattenListItems<T>(listContainer.entries);
        }

        for (const key of keysToTry) {
            if (_embedded?.[key]) {
                return this.flattenListItems<T>(_embedded[key] as unknown[]);
            }
            if (Array.isArray(rest[key])) {
                return this.flattenListItems<T>(rest[key] as unknown[]);
            }
            const nested = rest[key] as { entries?: unknown[] } | undefined;
            if (nested?.entries) {
                return this.flattenListItems<T>(nested.entries);
            }
        }

        return [];
    }

    protected unwrapEntity<T extends Record<string, unknown>>(response: RequestResponse): T {
        const { httpStatus, body, content, _embedded, entry, ...instance } = response;

        if (content && typeof content === 'object') {
            return content as T;
        }

        if (entry && typeof entry === 'object') {
            if (this.isErrorEntry(entry)) {
                throw new Error(entry.message || 'API error');
            }
            return entry as T;
        }

        if (instance.id) {
            return instance as T;
        }

        return instance as T;
    }

    protected async getText(endpoint: string, headers?: Record<string, string>): Promise<string> {
        const response = await this.requestRaw('get', endpoint, { headers });
        return response.text();
    }

    protected extractTextBody(response: RequestResponse): string {
        if (typeof response.body === 'string') {
            return response.body;
        }
        if (response.httpStatus && response.httpStatus >= 400) {
            throw new Error(
                typeof response.body === 'string' ? response.body : JSON.stringify(response)
            );
        }
        return '';
    }

    private flattenListItem<T>(item: unknown): T {
        if (item && typeof item === 'object' && 'entry' in item) {
            const wrapped = (item as { entry?: unknown }).entry;
            if (wrapped && typeof wrapped === 'object' && !this.isErrorEntry(wrapped)) {
                return wrapped as T;
            }
        }
        return item as T;
    }

    private flattenListItems<T>(items: unknown[]): T[] {
        return items.map((item) => this.flattenListItem<T>(item));
    }

    private isErrorEntry(entry: unknown): entry is { code: number; message: string } {
        return (
            typeof entry === 'object' &&
            entry !== null &&
            'code' in entry &&
            'message' in entry &&
            typeof (entry as { code: unknown }).code === 'number'
        );
    }

    static getStatusCheck<T extends BaseService>(label: string, path: string): HttpStatusCheck<T> {
        return { label, run: (service) => service.getHttpStatus(path) };
    }

    static postStatusCheck<T extends BaseService>(
        label: string,
        path: string,
        data: unknown
    ): HttpStatusCheck<T> {
        return { label, run: (service) => service.postHttpStatus(path, { data }) };
    }

    static runStatusChecks<T extends BaseService>(
        checks: readonly HttpStatusCheck<T>[],
        service: T
    ): Promise<number[]> {
        return Promise.all(checks.map(({ run }) => run(service)));
    }
}
