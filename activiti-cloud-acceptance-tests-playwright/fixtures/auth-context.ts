/*
 * Shared authenticated API context access — same path for tests (AuthCache) and setup/preflight (ephemeral).
 */

import type { UserKey } from '../config/users';
import { AuthCache } from './auth-cache';
import { ContextFactory } from './context-factory';
import { CustomAPIRequest } from './context.models';

export async function withAuthenticatedContext<T>(
    userKey: UserKey,
    fn: (context: CustomAPIRequest) => Promise<T>,
    authCache?: AuthCache
): Promise<T> {
    if (authCache) {
        const context = await authCache.getContext(userKey);
        return fn(context);
    }

    const context = await ContextFactory.getContextByUserName(userKey);
    try {
        return await fn(context);
    } finally {
        await context.dispose();
    }
}
