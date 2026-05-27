/*
 * Serenity DirtyContextHandler parity — DELETE resources created during a test.
 * Registered after each test via fixture teardown (see fixtures/services.fixture.ts).
 */

import { CustomAPIRequest } from '../fixtures/context.models';
import { Logger } from '../helpers/logging/logger';

export interface DirtyResource {
    context: CustomAPIRequest;
    path: string;
}

export interface CleanupOptions {
    /**
     * Nests cleanup under the test in Playwright reporters (list / HTML).
     * Pass `test.step` from the fixture teardown.
     */
    step?: <T>(title: string, body: () => Promise<T>) => Promise<T>;
    /**
     * `logger` (default) — same winston lines as API calls in base.service.
     * `console` — colored acceptance-progress personas (debug only).
     */
    style?: 'logger' | 'console';
}

/**
 * Tracks runtime resources (process instances, standalone tasks, …) for cleanup.
 * Paths are relative to the API baseURL (e.g. /rb/v1/process-instances/{id}).
 */
export class DirtyContextRegistry {
    private readonly resources: DirtyResource[] = [];

    register(context: CustomAPIRequest, path: string): void {
        const normalized = path.startsWith('/') ? path : `/${path}`;
        this.resources.unshift({ context, path: normalized });
    }

    pendingCount(): number {
        return this.resources.length;
    }

    async cleanup(options: CleanupOptions = {}): Promise<void> {
        if (this.resources.length === 0) {
            return;
        }

        const run = () => this.runCleanup(options.style ?? 'logger');

        if (options.step) {
            const count = this.resources.length;
            await options.step(`Cleanup ${count} tracked resource${count === 1 ? '' : 's'}`, run);
        } else {
            await run();
        }
    }

    private async runCleanup(style: 'logger' | 'console'): Promise<void> {
        if (style === 'console') {
            await this.runCleanupConsole();
            return;
        }

        const failed: DirtyResource[] = [];

        for (const resource of this.resources) {
            const user = resource.context.username ?? 'unknown';
            const start = Date.now();

            try {
                const response = await resource.context.delete(resource.path);
                const status = response.status();
                const duration = Date.now() - start;
                this.logDelete(user, resource.path, status, duration);

                if (status >= 400 && status !== 404) {
                    failed.push(resource);
                }
            } catch (error) {
                const duration = Date.now() - start;
                this.logDelete(user, resource.path, 0, duration);
                Logger.warn(
                    `[${user}] :: [DELETE] :: cleanup failed :: ${resource.path} :: ${
                        error instanceof Error ? error.message : String(error)
                    }`
                );
                failed.push(resource);
            }
        }

        this.resources.length = 0;

        if (failed.length > 0) {
            Logger.warn(
                `Cleanup: ${failed.length} resource(s) could not be deleted (left on cluster; scoped names should keep tests isolated)`
            );
        }
    }

    /** Same shape as base.service successful request logs. */
    private logDelete(username: string, path: string, status: number, durationMs: number): void {
        if (status === 404) {
            return;
        }

        const msg = `[${username}] :: [DELETE] :: [${status}] :: [${durationMs} ms] :: ${path}`;

        if (status >= 400) {
            Logger.warn(msg);
        } else {
            Logger.info(msg);
        }
    }

    /** Verbose personas — opt-in via ACCEPTANCE_CLEANUP_VERBOSE=true */
    private async runCleanupConsole(): Promise<void> {
        const {
            cleanupPhase,
            cleanupStep,
            cleanupVibe,
            cleanupLog,
            describeResourcePath,
            inferActorFromPath,
        } = await import('./acceptance-progress');

        const count = this.resources.length;
        cleanupPhase(
            'coordinator',
            `After-test cleanup — ${count} ${count === 1 ? 'resource' : 'resources'} on the guest list`
        );

        const failed: DirtyResource[] = [];
        let index = 0;

        for (const resource of this.resources) {
            index += 1;
            const actor = inferActorFromPath(resource.path);
            const kind = describeResourcePath(resource.path);
            const user = resource.context.username ?? 'unknown';

            cleanupStep(actor, `DELETE ${kind} ${resource.path} (${user})`);

            if (index === 1 || index === count || index % 3 === 0) {
                cleanupVibe(actor);
            }

            try {
                const response = await resource.context.delete(resource.path);
                const status = response.status();
                if (status >= 400 && status !== 404) {
                    cleanupLog(actor, `⚠ DELETE returned ${status} for ${user}`);
                    failed.push(resource);
                } else if (status === 404) {
                    cleanupLog(actor, `✓ already gone (404) — ${kind}`);
                } else {
                    cleanupLog(actor, `✓ ${kind} evicted (${status})`);
                }
            } catch (error) {
                const detail = error instanceof Error ? error.message : String(error);
                cleanupLog(actor, `⚠ DELETE failed for ${user}: ${detail}`);
                failed.push(resource);
            }
        }

        this.resources.length = 0;

        if (failed.length > 0) {
            cleanupLog('coordinator', `${failed.length} resource(s) could not be deleted`);
        } else {
            cleanupLog('coordinator', '✓ Guest list clear');
        }
    }
}
