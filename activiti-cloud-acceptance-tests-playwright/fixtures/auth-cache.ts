/*
 * Worker-scoped OAuth context cache — one Keycloak token + APIRequestContext per user per worker.
 */

import type { UserKey } from '../config/users';
import { ContextFactory } from './context-factory';
import { CustomAPIRequest } from './context.models';

/** Refresh before JWT expiry to avoid mid-test 401s. */
const TOKEN_REFRESH_BUFFER_MS = 60_000;

export class AuthCache {
    private readonly contexts = new Map<UserKey, CustomAPIRequest>();

    async getContext(userKey: UserKey): Promise<CustomAPIRequest> {
        const existing = this.contexts.get(userKey);
        if (existing && this.isTokenValid(existing)) {
            return existing;
        }

        if (existing) {
            await existing.dispose();
            this.contexts.delete(userKey);
        }

        const context = await ContextFactory.getContextByUserName(userKey);
        this.contexts.set(userKey, context);
        return context;
    }

    async disposeAll(): Promise<void> {
        for (const context of this.contexts.values()) {
            await context.dispose();
        }
        this.contexts.clear();
    }

    private isTokenValid(context: CustomAPIRequest): boolean {
        return context.expires_in.getTime() - TOKEN_REFRESH_BUFFER_MS > Date.now();
    }
}
