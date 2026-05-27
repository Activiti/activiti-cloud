/*
 * Copyright 2005-2023 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import { APIRequestContext, request } from '@playwright/test';
import { fromUnixTime } from 'date-fns';
import { jwtDecode } from 'jwt-decode';
import { resolveGatewayConnection } from '../config/connection/gateway-url';
import { getKeycloakOAuthConfig } from '../config/connection/keycloak-config';
import { users } from '../config/users';
import { AuthFormData, CustomAPIRequest, TokenDetails, UserData } from './context.models';

export class ContextFactory {

    static async getContextByUserName(userKey: keyof typeof users): Promise<CustomAPIRequest> {
        if (!(userKey in users)) {
            throw new Error(`Unknown user: ${userKey}. Available users: ${Object.keys(users).join(', ')}`);
        }
        const { access_token } = await ContextFactory.getAuthTokenForUser(
            ContextFactory.getFormData(users[userKey])
        );
        return this.getContextByParameters(access_token, 'GATEWAY_HOST', userKey);
    }

    static async getAuthTokenForUser(authFormData: AuthFormData): Promise<TokenDetails> {
        const { tokenUrl, hostHeader } = getKeycloakOAuthConfig();

        const requestContext = await request.newContext();
        try {
            const headers: Record<string, string> = {};
            if (hostHeader) {
                headers.Host = hostHeader;
            }

            const resp = await requestContext.post(tokenUrl, {
                ...authFormData,
                headers: Object.keys(headers).length ? headers : undefined,
            });

            if (!/20\d/.exec(resp.status().toString())) {
                const errorMessage = `Error during sending a POST request: \n
            Endpoint: ${tokenUrl} \n
            Params: ${JSON.stringify(ContextFactory.cleanSensitiveAuthFromData(authFormData), null, 2)}\n
            Error: ${JSON.stringify(resp, null, 2)}
            Response body: ${await resp.text()}`;
                throw new Error(errorMessage);
            }

            const { access_token, expires_in } = await resp.json();
            return { access_token, expires_in };
        } finally {
            await requestContext.dispose();
        }
    }
    static getFormData(userData: UserData): AuthFormData {
        if (!userData?.username || !userData?.password) {
            throw new Error('User data must contain username and password');
        }

        const { clientId, clientSecret } = getKeycloakOAuthConfig();

        const form: AuthFormData['form'] = {
            username: userData.username,
            password: userData.password,
            grant_type: 'password',
            client_id: clientId,
        };

        if (clientSecret) {
            form.client_secret = clientSecret;
        }

        return { form };
    }

    private static async getContextByParameters(accessToken: string, _contextBaseUrl: 'GATEWAY_HOST', username: string): Promise<CustomAPIRequest> {
        const tokenData = jwtDecode(accessToken);
        const { exp: expires_in } = tokenData;

        const { baseURL, hostHeader } = resolveGatewayConnection();

        const extraHeaders: Record<string, string> = {
            Authorization: `Bearer ${accessToken}`,
            accept: 'application/json, text/plain, */*'
        };

        if (hostHeader) {
            extraHeaders.Host = hostHeader;
        }

        const context = await request.newContext({
            baseURL,
            extraHTTPHeaders: extraHeaders
        });

        const expiresAt = fromUnixTime(parseInt(expires_in!.toString(), 10));
        return wrapAuthenticatedApiContext(context, accessToken, expiresAt, username);
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

}

/** Exposes token metadata on a Playwright APIRequestContext without prototype mutation. */
export function wrapAuthenticatedApiContext(
    api: APIRequestContext,
    token: string,
    expires_in: Date,
    username: string
): CustomAPIRequest {
    return new Proxy(api, {
        get(target, prop, receiver) {
            if (prop === 'token') {
                return token;
            }
            if (prop === 'expires_in') {
                return expires_in;
            }
            if (prop === 'username') {
                return username;
            }
            const value = Reflect.get(target, prop, receiver);
            return typeof value === 'function' ? value.bind(target) : value;
        },
    }) as CustomAPIRequest;
}
