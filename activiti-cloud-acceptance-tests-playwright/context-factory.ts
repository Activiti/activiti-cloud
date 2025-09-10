/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import { APIRequestContext, request } from '@playwright/test';
import { fromUnixTime } from 'date-fns';
import { jwtDecode } from 'jwt-decode';
import { AuthFormData, CustomAPIRequest, ExtendedJwtPayload, TokenDetails, UserData } from './context.models';
import { users } from './users';

export class ContextFactory {

    static async getContextByUserName(username: string): Promise<CustomAPIRequest> {
        if (!(username in users)) {
            throw new Error(`Unknown user: ${username}. Available users: ${Object.keys(users).join(', ')}`);
        }
        const { access_token } = await ContextFactory.getAuthTokenForUser(ContextFactory.getFormData(users[username as keyof typeof users]));
        return this.getContextByParameters(access_token, 'GATEWAY_HOST', username);
    }

    static async getAuthTokenForUser(authFormData: AuthFormData): Promise<TokenDetails> {
        const ssoUrl = process.env['SSO_HOST']!;

        const requestContext = await request.newContext();
        const resp = await requestContext.post(ssoUrl, authFormData);

        if (!/20\d/.exec(resp.status().toString())) {
            const errorMessage = `Error during sending a POST request: \n
            Endpoint: ${ssoUrl} \n
            Params: ${JSON.stringify(ContextFactory.cleanSensitiveAuthFromData(authFormData), null, 2)}\n
            Error: ${JSON.stringify(resp, null, 2)}
            Response body: ${await resp.text()}`;
            throw new Error(errorMessage);
        } else {
            const { access_token, expires_in } = await resp.json();
            return { access_token, expires_in };
        }
    }
    static getFormData(userData: UserData): AuthFormData {
        if (!userData?.username || !userData?.password) {
            throw new Error('User data must contain username and password');
        }

        return {
          form: {
            username: userData.username,
            password: userData.password,
            grant_type: 'password',
            client_id: process.env.REALM
          }
        }
    }

    private static async getContextByParameters(accessToken: string, contextBaseUrl: 'GATEWAY_HOST', username: string): Promise<CustomAPIRequest> {
        const tokenData = jwtDecode(accessToken);
        const { exp: expires_in } = tokenData;

        // Determine protocol and construct baseURL
        const protocol = process.env.GATEWAY_PROTOCOL || 'https';
        const host = process.env[contextBaseUrl];
        const baseURL = `${protocol}://${host}`;

        // For localhost port forwarding, we need to set the proper Host header
        const extraHeaders: Record<string, string> = {
            Authorization: `Bearer ${accessToken}`,
            accept: 'application/json, text/plain, */*'
        };

        // If using localhost port forwarding, set the Host header to the original domain
        if (host?.includes('localhost')) {
            const originalHost = `gateway-${process.env.PREVIEW_NAME}.${process.env.CLUSTER_NAME}.${process.env.CLUSTER_DOMAIN}`;
            extraHeaders.Host = originalHost;
        }

        const context = await request.newContext({
            baseURL: baseURL,
            extraHTTPHeaders: extraHeaders
        });

        return this.getCustomContextObject(context, accessToken, expires_in!.toString(), username);
    }

    private static cleanSensitiveAuthFromData(authFormData: AuthFormData): AuthFormData {
        const data = { form: { ...authFormData.form } };
        if (data.form.password) {
            data.form.password = '***';
        }
        if (data.form.client_secret) {
            data.form.client_secret = '***';
        }
        return data;
    }

    private static getCustomContextObject(context: APIRequestContext, token: string, expires_in: string, username: string): CustomAPIRequest {
        const newContext = {
            token,
            expires_in: fromUnixTime(parseInt(expires_in)),
            username,
            ...context
        };

        return Object.setPrototypeOf(newContext, Object.getPrototypeOf(context));
    }
}
