/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import { APIResponse } from '@playwright/test';
import { CustomAPIRequest } from '../context.models';
import { Options } from '../models/base-service.models';
import { Logger } from '../logger';

export interface RequestResponse {
    [key: string]: any;
    status?: number;
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

export abstract class BaseService {
    context: CustomAPIRequest;

    constructor(context: CustomAPIRequest) {
        this.context = context;
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

    private async request(httpMethod: string, endpoint: string, overriddenOptions?: Options): Promise<RequestResponse> {
        const response = await this.requestRaw(httpMethod, endpoint, overriddenOptions);

        return this.returnParsedJsonData(response);
    }

    private async requestRaw(httpMethod: string, endpoint: string, overriddenOptions?: Options): Promise<APIResponse> {
        const startTime = Date.now();

        let response: APIResponse;
        switch (httpMethod.toLowerCase()) {
            case 'get':
                response = await this.context.get(endpoint, overriddenOptions);
                break;
            case 'post':
                response = await this.context.post(endpoint, overriddenOptions);
                break;
            case 'put':
                response = await this.context.put(endpoint, overriddenOptions);
                break;
            case 'delete':
                response = await this.context.delete(endpoint, overriddenOptions);
                break;
            case 'fetch':
                response = await this.context.fetch(endpoint, overriddenOptions);
                break;
            default:
                throw new Error(`Unsupported HTTP method: ${httpMethod}`);
        }

        const endTime = Date.now();
        const requestTimeDuration = `[${endTime - startTime} ms]`;
        const statusCode = response.status();

        if (this.checkStatusCode(statusCode, StatusCodes['Success'])) {
            Logger.info(this.getRequestLogMessage(httpMethod, endpoint, statusCode, requestTimeDuration, this.context.username!, overriddenOptions));
        }

        if (this.checkStatusCode(statusCode, StatusCodes['ClientError'])) {
            Logger.warn(await this.getWarnMessage(response, httpMethod, requestTimeDuration));
        }

        if (this.checkStatusCode(statusCode, StatusCodes['ServerError'])) {
            const conditionIfNucleusApiResponseContainsClientError =
                (await this.checkErrorMessageDetails(response, StatusCodes['ClientError'])) || (await this.checkErrorMessageDetails(response, /Forbidden/));
            if (!conditionIfNucleusApiResponseContainsClientError) {
                const errorMessage = this.getErrorMessage(httpMethod, endpoint, await response.text(), requestTimeDuration, overriddenOptions);
                Logger.error(errorMessage);
                throw new Error(errorMessage);
            }

            Logger.warn(await this.getWarnMessage(response, httpMethod, requestTimeDuration));
        }

        return response;
    }

    private async returnParsedJsonData(response: APIResponse): Promise<RequestResponse> {
        if (response.headers()['content-type'] === 'application/zip') {
            return response.body();
        }

        try {
            return JSON.parse(JSON.parse(JSON.stringify(await response.text())));
        } catch (e) {
            Logger.warn(`Failed to parse JSON response: ${e}`);
            return { status: response.status(), body: await response.text() };
        }
    }

    private checkStatusCode(statusCode: number, statusRegex: RegExp): boolean {
        return statusRegex.test(statusCode.toString());
    }

    private async checkErrorMessageDetails(response: APIResponse, statusRegex: RegExp): Promise<boolean> {
        return statusRegex.test(await response.text());
    }

    private getRequestLogMessage(httpMethod: string, endpoint: string, statusCode: number, requestTimeDuration: string, username: string, overriddenOptions?: Options): string {
        const baseMessage = `[${username}] :: [${httpMethod.toUpperCase()}] :: [${statusCode}] :: ${requestTimeDuration} :: ${endpoint}`;
        return overriddenOptions && !(overriddenOptions?.data instanceof Buffer)
            ? baseMessage.concat(` with data ${JSON.stringify(overriddenOptions)}`)
            : baseMessage;
    }

    private getErrorMessage(httpMethod: string, endpoint: string, e: any, requestTimeDuration: string, overriddenOptions?: Options): string {
        const params = JSON.stringify(overriddenOptions?.data);
        return `Error ${httpMethod.toUpperCase()} request :: ${requestTimeDuration} ::
        Endpoint: ${endpoint} ${params ? '\n Params: ' + params : ''}
        Message: ${e.message || e}`;
    }

    private async getWarnMessage(response: APIResponse, httpMethod: string, requestTimeDuration: string): Promise<string> {
        return `[${httpMethod.toUpperCase()}] :: [${response.status()}] :: ${requestTimeDuration} :: ${response.url()} Message: ${await response.text()}`;
    }
}
